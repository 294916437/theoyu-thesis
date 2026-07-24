基于官方最新的构建文档（以目前支持的 \*\*libwebrtc `m140` 分支\*\* 为准），为你整理了一份条理清晰的 \*\*`libmediasoupclient` 个人构建步骤指南\*\*。



\---



\# `libmediasoupclient` 源码构建步骤指南



\## 一、 环境要求 (Prerequisites)



在开始前，请确保本地开发环境符合以下要求：



\* \*\*操作系统\*\*：POSIX (Linux / macOS) 或 Windows

\* \*\*CMake\*\*：>= 3.14

\* \*\*编译器\*\*：GCC/G++ >= 4.9 或 Clang（必须支持 \*\*C++20\*\* 标准）

\* \*\*Chromium 编译工具链\*\*：`depot\_tools`（编译 `libwebrtc` 必备）



\---



\## 二、 步骤 1：获取并编译 `libwebrtc`



`libmediasoupclient` 依赖 Google 的 `libwebrtc` 静态库，需先拉取对应分支并进行编译。



\### 1.1 拉取代码与切分支



```bash

\# 1. 创建工作目录

mkdir -p \~/src/webrtc-checkout

cd \~/src/webrtc-checkout



\# 2. 拉取 WebRTC 源码 (使用 Chromium 的 depot\_tools)

fetch --nohooks webrtc

gclient sync



\# 3. 进入 src 目录并切换到官方指定的 m140 分支 (branch-heads/7339)

cd src

git checkout -b m140 refs/remotes/branch-heads/7339

gclient sync



```



\### 1.2 生成构建配置 (GN)



根据你的操作系统选择对应的配置参数。



> \*\*注意关键参数\*\*：

> \* `use\_custom\_libcxx=false`：\*\*强制使用系统 C++ 标准库\*\*，防止后续与 `libmediasoupclient` 符号链接时发生 `libc++` 冲突。

> \* `use\_rtti=true`：开启 RTTI 运行时类型识别。

> 

> 



\* \*\*macOS 环境参数：\*\*

```bash

gn gen out/m140 --args='is\_debug=false is\_component\_build=false is\_clang=true rtc\_include\_tests=false rtc\_use\_h264=true use\_rtti=true use\_custom\_libcxx=false'



```





\* \*\*Linux (GCC) 环境参数：\*\*

```bash

gn gen out/m140 --args='is\_debug=false is\_component\_build=false is\_clang=false rtc\_include\_tests=false rtc\_use\_h264=true use\_rtti=true use\_custom\_libcxx=false treat\_warnings\_as\_errors=false use\_ozone=true'



```







\### 1.3 编译 libwebrtc



```bash

ninja -C out/m140



```



\*编译完成后，静态库放置在：`\~/src/webrtc-checkout/src/out/m140/obj\*`



\---



\## 三、 步骤 2：获取并编译 `libmediasoupclient`



\### 2.1 克隆源码并切换版本号



```bash

cd \~/src

git clone https://github.com/versatica/libmediasoupclient.git

cd libmediasoupclient/



\# 切换到最新的稳定 Tag (例如 3.X.Y)

git checkout 3.x.y



```



\### 2.2 使用 CMake 配置工程



通过变量指定刚才编译好的 `libwebrtc` 源码目录与二进制库目录：



```bash

cmake . -Bbuild \\

&#x20; -DLIBWEBRTC\_INCLUDE\_PATH:PATH=/home/foo/src/webrtc-checkout/src \\

&#x20; -DLIBWEBRTC\_BINARY\_PATH:PATH=/home/foo/src/webrtc-checkout/src/out/m140/obj \\

&#x20; -DCMAKE\_CXX\_FLAGS="-fvisibility=hidden"



```



> 💡 \*\*进阶：结合前面你遇到的 Android 16 KB 页对齐问题\*\*

> 如果你是为 \*\*Android\*\* 交叉编译该 Native 库，记得在 `CMAKE\_CXX\_FLAGS` / `CMAKE\_SHARED\_LINKER\_FLAGS` 中追加对齐参数：

> `-Wl,-z,max-page-size=16384`



\### 2.3 执行构建与安装



```bash

\# 编译

make -C build/



\# (可选) 将静态库和头文件安装到系统目录

sudo make install -C build/



```



编译成功后，系统产物目录：



\* \*\*静态库\*\*：`/usr/local/lib/libmediasoupclient.a`

\* \*\*头文件\*\*：`/usr/local/include/mediasoupclient/mediasoupclient.hpp`



\---



\## 四、 CMake 编译常用参数表 (CMake Flags)



| 参数名 | 类型 | 说明 | 是否必填 | 默认值 |

| --- | --- | --- | --- | --- |

| `LIBWEBRTC\_INCLUDE\_PATH` | Path | `libwebrtc` 源码 `src/` 文件夹路径 | \*\*是\*\* | - |

| `LIBWEBRTC\_BINARY\_PATH` | Path | `libwebrtc` 静态库 `.a` / `.obj` 放置路径 | \*\*是\*\* | - |

| `MEDIASOUPCLIENT\_LOG\_DEV` | Bool | 启用开发者日志宏 `MSC\_LOG\_DEV` | 否 | `false` |

| `MEDIASOUPCLIENT\_LOG\_TRACE` | Bool | 启用 Trace 日志宏 `MSC\_LOG\_TRACE` | 否 | `false` |

| `MEDIASOUPCLIENT\_BUILD\_TESTS` | Bool | 是否构建单元测试 | 否 | `false` |

| `CMAKE\_CXX\_FLAGS` | String | 额外的 C++ 编译 Flag（如符号可见性/对齐控制等） | 否 | - |



\---



\## 五、 踩坑注意事项 (Linkage Considerations)



1\. \*\*符号可见性不匹配 (Symbol Visibility Warnings)\*\*：

\* 如果 `libwebrtc` 编译时使用了 `hidden` 符号隐藏，但 `libmediasoupclient` 没加，链接时会报大量 `ld: warning: direct access in function...` 警告。

\* \*\*解决办法\*\*：在配置 `libmediasoupclient` 时显式传入 `-DCMAKE\_CXX\_FLAGS="-fvisibility=hidden"`。





2\. \*\*C++ 标准库冲突 (Standard Library Mismatch)\*\*：

\* `libwebrtc` 默认倾向于使用 Chromium 自带的 `libc++`。如果不强行指定 `use\_custom\_libcxx=false`，后续在链接系统 `libstdc++` 时极易爆出未定义符号错误。

