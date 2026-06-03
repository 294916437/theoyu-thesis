# 设置遇到错误不中断，但打印红字
$ErrorActionPreference = "Continue"

# 修复 PowerShell 控制台中文乱码：切换代码页为 UTF-8
chcp 65001 >$null 2>&1
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$OutputEncoding = [Text.Encoding]::UTF8

# 切换到脚本所在目录 (backend目录)
Set-Location $PSScriptRoot

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "         微服务动态启动脚本" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# 所有的微服务列表
$allServiceDirs = @(
    "thesis-gateway",
    "thesis-auth",
    "thesis-user\thesis-user-biz",
    "thesis-oss\thesis-oss-biz",
    "thesis-kv\thesis-kv-biz",
    "thesis-id-generator\thesis-id-generator-biz",
    "thesis-media\thesis-media-biz",
    "thesis-chat\thesis-chat-biz"
)

Write-Host "`n可用微服务列表：" -ForegroundColor Cyan
for ($i = 0; $i -lt $allServiceDirs.Length; $i++) {
    Write-Host "$($i + 1). $($allServiceDirs[$i])"
}

# ================= 1. 编译环节 =================
Write-Host "`n[第一步：编译阶段]" -ForegroundColor Yellow
$compileChoice = Read-Host "请选择需要重新编译的模块序号 (输入数字如 1,2，输入 'all' 全量编译，直接回车跳过编译)"

if ([string]::IsNullOrWhiteSpace($compileChoice)) {
    Write-Host "跳过 Maven 编译环节..." -ForegroundColor DarkGray
} elseif ($compileChoice.Trim().ToLower() -eq 'all') {
    Write-Host "正在全量编译整个项目 (跳过测试)..." -ForegroundColor Cyan
    mvn clean install -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Maven 编译失败，请检查代码或环境配置！" -ForegroundColor Red
        exit 1
    }
} else {
    $compileArray = $compileChoice.Split(',')
    $plArgs = @()
    foreach ($c in $compileArray) {
        $index = [int]$c.Trim() - 1
        if ($index -ge 0 -and $index -lt $allServiceDirs.Length) {
            # 将路径分隔符统一替换为正斜杠，以适配 Maven 的 -pl 参数规范
            $modulePath = $allServiceDirs[$index] -replace "\\", "/"
            $plArgs += $modulePath
        } else {
            Write-Host "警告: 忽略无效的编译序号 '$c'" -ForegroundColor Yellow
        }
    }
    
    if ($plArgs.Count -gt 0) {
        $plString = $plArgs -join ","
        Write-Host "正在增量编译所选模块及其依赖 ($plString) ..." -ForegroundColor Cyan
        # 使用 -pl (指定模块) 和 -am (also make，同时构建其依赖的其他模块如 API/Common)
        $mvnCommand = "mvn clean install -pl $plString -am -DskipTests"
        Write-Host "执行命令: $mvnCommand" -ForegroundColor DarkGray
        Invoke-Expression $mvnCommand
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Maven 部分模块编译失败，请检查代码！" -ForegroundColor Red
            exit 1
        }
    }
}

# ================= 2. 启动环节 =================
Write-Host "`n[第二步：启动阶段]" -ForegroundColor Yellow
$startChoice = Read-Host "请选择要启动的服务序号 (输入数字如 1,2，输入 'all' 启动全部，直接回车退出)"

# 解析用户的选择
$selectedDirs = @()
if ([string]::IsNullOrWhiteSpace($startChoice)) {
    Write-Host "未选择启动服务，脚本正常退出。" -ForegroundColor Green
    exit 0
} elseif ($startChoice.Trim().ToLower() -eq "all") {
    $selectedDirs = $allServiceDirs
} else {
    $startArray = $startChoice.Split(',')
    foreach ($c in $startArray) {
        $index = [int]$c.Trim() - 1
        if ($index -ge 0 -and $index -lt $allServiceDirs.Length) {
            $selectedDirs += $allServiceDirs[$index]
        } else {
            Write-Host "警告: 忽略无效的启动序号 '$c'" -ForegroundColor Yellow
        }
    }
}

if ($selectedDirs.Count -eq 0) {
    Write-Host "未选择任何有效的服务启动，脚本退出。" -ForegroundColor Red
    exit 0
}

Write-Host "`n=========================================" -ForegroundColor Cyan
Write-Host "开始拉起以下微服务:" -ForegroundColor Cyan
$selectedDirs | ForEach-Object { Write-Host " - $_" -ForegroundColor Green }
Write-Host "=========================================" -ForegroundColor Cyan

foreach ($dir in $selectedDirs) {
    $targetPath = Join-Path $dir "target"
    
    if (Test-Path $targetPath) {
        # 寻找可执行的 jar 包 (排除以 -sources.jar 结尾的源码包)
        $jarFile = Get-ChildItem -Path $targetPath -Filter "*.jar" | Where-Object { $_.Name -notmatch "sources\.jar$" -and $_.Name -notmatch "javadoc\.jar$" } | Select-Object -First 1
        
        if ($jarFile) {
            Write-Host "正在启动 -> $($jarFile.Name)" -ForegroundColor Green
            $windowTitle = "微服务: $dir"
            
            # 使用 Start-Process 弹出一个新的 PowerShell 窗口，并限制 JVM 内存
            Start-Process powershell -ArgumentList "-NoExit", "-Command", "chcp 65001 >`$null 2>&1; [Console]::OutputEncoding = [Text.Encoding]::UTF8; `$Host.UI.RawUI.WindowTitle = '$windowTitle'; java `"-Dfile.encoding=UTF-8`" -Xms128m -Xmx256m -jar `"$($jarFile.FullName)`""
        } else {
            Write-Host "[跳过] 未在 $targetPath 中找到 Jar 文件。可能它是一个没有可执行类的依赖模块，或未被编译！" -ForegroundColor Yellow
        }
    } else {
        Write-Host "[错误] 目录 $targetPath 不存在。可能是该模块尚未进行 Maven 编译。" -ForegroundColor Red
    }
}

Write-Host "`n所有选定服务已发令启动！" -ForegroundColor Cyan
