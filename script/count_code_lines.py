#!/usr/bin/env python3
"""Fast source line counter for this repository.

The script counts authored source files only. It intentionally reports
physical lines, non-empty lines, and blank lines instead of trying to infer
"logical code lines" across many languages with incompatible comment syntax.
"""

from __future__ import annotations

import argparse
import os
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


LANGUAGE_BY_EXTENSION = {
    ".c": "C",
    ".cc": "C++",
    ".cpp": "C++",
    ".cxx": "C++",
    ".h": "C/C++ Header",
    ".hpp": "C++ Header",
    ".cs": "C#",
    ".css": "CSS",
    ".go": "Go",
    ".html": "HTML",
    ".java": "Java",
    ".js": "JavaScript",
    ".jsx": "JavaScript JSX",
    ".json": "JSON",
    ".kt": "Kotlin",
    ".kts": "Kotlin",
    ".less": "Less",
    ".lua": "Lua",
    ".mjs": "JavaScript",
    ".mm": "Objective-C++",
    ".pbxproj": "Xcode Project",
    ".php": "PHP",
    ".proto": "Protocol Buffers",
    ".ps1": "PowerShell",
    ".py": "Python",
    ".rs": "Rust",
    ".sass": "Sass",
    ".scala": "Scala",
    ".scss": "SCSS",
    ".sh": "Shell",
    ".sql": "SQL",
    ".swift": "Swift",
    ".ts": "TypeScript",
    ".tsx": "TypeScript TSX",
    ".vue": "Vue",
    ".xml": "XML",
    ".yaml": "YAML",
    ".yml": "YAML",
}

LANGUAGE_BY_FILENAME = {
    "Dockerfile": "Dockerfile",
    "Containerfile": "Dockerfile",
    "Makefile": "Makefile",
    "Rakefile": "Ruby",
    "Jenkinsfile": "Groovy",
}

DEFAULT_EXCLUDED_DIRS = {
    ".cache",
    ".git",
    ".gradle",
    ".idea",
    ".next",
    ".nuxt",
    ".output",
    ".pnpm-store",
    ".pytest_cache",
    ".ruff_cache",
    ".svn",
    ".turbo",
    ".venv",
    ".vscode",
    "__pycache__",
    "bin",
    "build",
    "coverage",
    "debug",
    "dist",
    "generated",
    "logs",
    "node_modules",
    "mediasoup-demo",
    "demos",
    "test",
    "out",
    "release",
    "target",
    "tmp",
    "vendor",
}

DEFAULT_EXCLUDED_FILES = {
    "Cargo.lock",
    "package-lock.json",
    "pnpm-lock.yaml",
    "yarn.lock",
}

DEFAULT_EXCLUDED_SUFFIXES = (
    ".bundle.js",
    ".bundle.css",
    ".d.ts.map",
    ".generated.js",
    ".generated.ts",
    ".map",
    ".min.css",
    ".min.js",
)


@dataclass(slots=True)
class LineStats:
    files: int = 0
    total: int = 0
    non_empty: int = 0
    blank: int = 0
    bytes_read: int = 0

    def add(self, other: "LineStats") -> None:
        self.files += other.files
        self.total += other.total
        self.non_empty += other.non_empty
        self.blank += other.blank
        self.bytes_read += other.bytes_read


@dataclass(frozen=True, slots=True)
class SourceFile:
    path: Path
    language: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Count authored source lines in the current project."
    )
    parser.add_argument(
        "root",
        nargs="?",
        default=None,
        help="project root to scan, defaults to the repository containing this script",
    )
    parser.add_argument(
        "--by-dir",
        type=int,
        default=1,
        metavar="DEPTH",
        help="also summarize by top-level directory depth; use 0 to disable",
    )
    parser.add_argument(
        "--top",
        type=int,
        default=20,
        help="show the largest N files by total lines",
    )
    parser.add_argument(
        "--include-json",
        action="store_true",
        help="include JSON files; default skips dependency manifests and lockfiles only",
    )
    parser.add_argument(
        "--exclude-dir",
        action="append",
        default=[],
        help="additional directory name to exclude; can be used multiple times",
    )
    parser.add_argument(
        "--max-file-size",
        type=int,
        default=5 * 1024 * 1024,
        help="skip files larger than this many bytes, defaults to 5242880",
    )
    return parser.parse_args()


def find_project_root(start: Path) -> Path:
    """Find the nearest repository root from a starting path."""
    current = start.resolve()
    if current.is_file():
        current = current.parent

    for candidate in (current, *current.parents):
        if (candidate / ".git").exists():
            return candidate
        if (candidate / "package.json").exists() and (
            (candidate / "backend").is_dir() or (candidate / "frontend").is_dir()
        ):
            return candidate

    return Path.cwd().resolve()


def resolve_scan_root(root_arg: str | None) -> Path:
    if root_arg:
        return Path(root_arg).resolve()
    return find_project_root(Path(__file__))


def language_for(path: Path) -> str | None:
    if path.name in LANGUAGE_BY_FILENAME:
        return LANGUAGE_BY_FILENAME[path.name]
    return LANGUAGE_BY_EXTENSION.get(path.suffix.lower())


def is_excluded_file(path: Path, include_json: bool, max_file_size: int) -> bool:
    name = path.name
    lower_name = name.lower()

    if name in DEFAULT_EXCLUDED_FILES:
        return True
    if any(lower_name.endswith(suffix) for suffix in DEFAULT_EXCLUDED_SUFFIXES):
        return True
    if lower_name.endswith(".lock"):
        return True
    if not include_json and lower_name in {"package.json", "tsconfig.json", "jsconfig.json"}:
        return True

    try:
        stat = path.stat()
    except OSError:
        return True
    return stat.st_size > max_file_size


def iter_source_files(
    root: Path,
    excluded_dirs: set[str],
    include_json: bool,
    max_file_size: int,
) -> Iterable[SourceFile]:
    stack = [root]

    while stack:
        directory = stack.pop()
        try:
            with os.scandir(directory) as entries:
                for entry in entries:
                    try:
                        if entry.is_dir(follow_symlinks=False):
                            if entry.name not in excluded_dirs:
                                stack.append(Path(entry.path))
                            continue
                        if not entry.is_file(follow_symlinks=False):
                            continue
                    except OSError:
                        continue

                    path = Path(entry.path)
                    language = language_for(path)
                    if language is None:
                        continue
                    if is_excluded_file(path, include_json, max_file_size):
                        continue
                    yield SourceFile(path=path, language=language)
        except OSError:
            continue


def count_file(path: Path) -> LineStats | None:
    stats = LineStats(files=1)

    try:
        with path.open("rb") as file:
            for line in file:
                if b"\0" in line:
                    return None
                stats.total += 1
                stats.bytes_read += len(line)
                if line.strip():
                    stats.non_empty += 1
                else:
                    stats.blank += 1
    except OSError:
        return None

    return stats


def dir_key(path: Path, root: Path, depth: int) -> str:
    if depth <= 0:
        return ""

    try:
        relative = path.relative_to(root)
    except ValueError:
        relative = path

    parts = relative.parts[:-1]
    if not parts:
        return "."
    return str(Path(*parts[:depth]))


def print_table(headers: tuple[str, ...], rows: list[tuple[object, ...]]) -> None:
    widths = [len(header) for header in headers]
    for row in rows:
        for index, value in enumerate(row):
            widths[index] = max(widths[index], len(str(value)))

    fmt = "  ".join(f"{{:<{width}}}" for width in widths)
    print(fmt.format(*headers))
    print(fmt.format(*("-" * width for width in widths)))
    for row in rows:
        print(fmt.format(*row))


def format_int(value: int) -> str:
    return f"{value:,}"


def main() -> int:
    args = parse_args()
    root = resolve_scan_root(args.root)
    if not root.exists() or not root.is_dir():
        print(f"error: root is not a directory: {root}", file=sys.stderr)
        return 2

    excluded_dirs = set(DEFAULT_EXCLUDED_DIRS)
    excluded_dirs.update(args.exclude_dir)

    total = LineStats()
    by_language: dict[str, LineStats] = {}
    by_directory: dict[str, LineStats] = {}
    largest_files: list[tuple[int, str, str]] = []
    skipped_binary = 0

    for source in iter_source_files(
        root=root,
        excluded_dirs=excluded_dirs,
        include_json=args.include_json,
        max_file_size=args.max_file_size,
    ):
        stats = count_file(source.path)
        if stats is None:
            skipped_binary += 1
            continue

        total.add(stats)
        by_language.setdefault(source.language, LineStats()).add(stats)

        if args.by_dir > 0:
            key = dir_key(source.path, root, args.by_dir)
            by_directory.setdefault(key, LineStats()).add(stats)

        relative = str(source.path.relative_to(root))
        largest_files.append((stats.total, source.language, relative))

    print(f"Project root: {root}")
    print(f"Excluded dirs: {', '.join(sorted(excluded_dirs))}")
    print()
    print("Summary")
    print(f"  Files:       {format_int(total.files)}")
    print(f"  Total lines: {format_int(total.total)}")
    print(f"  Non-empty:   {format_int(total.non_empty)}")
    print(f"  Blank:       {format_int(total.blank)}")
    print(f"  Bytes read:  {format_int(total.bytes_read)}")
    if skipped_binary:
        print(f"  Binary/failed files skipped after detection: {format_int(skipped_binary)}")

    language_rows = [
        (
            language,
            format_int(stats.files),
            format_int(stats.total),
            format_int(stats.non_empty),
            format_int(stats.blank),
        )
        for language, stats in sorted(
            by_language.items(), key=lambda item: item[1].total, reverse=True
        )
    ]
    if language_rows:
        print()
        print("By language")
        print_table(("Language", "Files", "Lines", "Non-empty", "Blank"), language_rows)

    if args.by_dir > 0 and by_directory:
        directory_rows = [
            (
                directory,
                format_int(stats.files),
                format_int(stats.total),
                format_int(stats.non_empty),
                format_int(stats.blank),
            )
            for directory, stats in sorted(
                by_directory.items(), key=lambda item: item[1].total, reverse=True
            )
        ]
        print()
        print(f"By directory depth {args.by_dir}")
        print_table(("Directory", "Files", "Lines", "Non-empty", "Blank"), directory_rows)

    if args.top > 0 and largest_files:
        top_rows = [
            (format_int(lines), language, path)
            for lines, language, path in sorted(largest_files, reverse=True)[: args.top]
        ]
        print()
        print(f"Top {min(args.top, len(top_rows))} files")
        print_table(("Lines", "Language", "File"), top_rows)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
