/**
 * Meeting Room Theme
 * Synced from Android native theme definitions (Color.kt and MeetingSemanticColors.kt)
 */

export const colors = {
  // Brand colors
  primary: "#0B57D0",
  primaryDark: "#A8C7FA",
  primaryContainer: "#D3E3FD",
  primaryContainerDark: "#0842A0",
  
  teal: "#006A6A",
  tealDark: "#80D5D2",
  green: "#386A20",
  greenDark: "#A5D6A7",

  error: "#BA1A1A",
  errorDark: "#FFB4AB",
  errorContainer: "#FFDAD6", // Custom light red for danger backgrounds

  // Backgrounds & Surfaces
  background: "#F8FAFD",
  backgroundDark: "#101418",
  surface: "#FFFFFF",
  surfaceDark: "#161B22",
  surfaceVariant: "#E1E3E8",
  surfaceVariantDark: "#414750",

  // Text
  text: "#1F1F1F",
  textDark: "#E3E3E3",
  textMuted: "#5F6368",
  textMutedDark: "#C4C7C5",

  // Outlines
  outline: "#74777F",
  outlineVariant: "#C4C7C5", // Lighter outline for borders
  outlineDark: "#8E939B",

  // Semantic
  speaking: "#0F9D58",
  recording: "#D93025",
  screenShare: "#F9AB00",
  networkPoor: "#EA8600",
  videoTile: "#202124",
  videoTileDark: "#0B0F14",
  onVideoTile: "#FFFFFF",
};

// Utilities for colors with opacity
export const rgba = (hex: string, alpha: number) => {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return `rgba(${r}, ${g}, ${b}, ${alpha})`;
};
