# Unity FPS Unlocker · libxposed API 101

面向 Unity IL2CPP 游戏的 Xposed 模块 libxposed API 101 适配版：按应用解锁帧率、注入延迟、调整渲染分辨率与显示模式。

本项目是 [hexstr/UnityFPSUnlocker](https://github.com/hexstr/UnityFPSUnlocker) 的 libxposed API 101 适配版本，不再使用经典 `de.robv.android.xposed` API。Hook 与配置层完全重写，设置界面沿用原模块的 MIUIX 布局与应用图标，并接入远程配置和动态作用域。

> 修改游戏运行参数可能造成画面异常、闪退或违反游戏服务条款，请自行评估风险。

## 与旧版的区别

- 入口迁移为 `XposedModule`，通过 `META-INF/xposed/java_init.list` 注册。
- 配置读写迁移到 libxposed 远程首选项：Hook 进程使用 `getRemotePreferences()`，设置界面使用 `XposedService`。
- 模块声明 `staticScope=false`，应用管理器通过 `XposedService.requestScope()` / `removeScope()` 动态增删作用域。
- 日志仍由目标进程通过带令牌的显式广播回传，模块进程本地环形保存最近 200 条。
- native 部分重写为独立命名空间，使用固定 JNI 符号的 Java 桥；IL2CPP icall 解析与指令修改逻辑保留。
- 设置界面沿用原模块的全局默认值 / 应用管理器 / 应用详情 / Hook 日志 / 关于页面与启动图标，配置读写改为远程首选项，应用启用时自动申请作用域。

## 为什么使用 API 101

API 102 新增了 `detach()`、运行中目标列表与 `hotReloadModule()`。对本模块而言，帧率与分辨率写入是进程级且不可逆的，热重载无法可靠回滚；而 API 101 已覆盖 Hook、远程配置、动态作用域与远程服务能力，兼容的框架版本更多，因此本项目以 101 为目标版本。

## 功能

| 参数 | 作用 | 关闭后 |
| --- | --- | --- |
| 目标 FPS | 调用 `UnityEngine.Application::set_targetFrameRate` | 不修改帧率 |
| 修改指令 | 将目标函数首指令改写为返回，避免游戏后续覆盖 | 只设置一次帧率 |
| 注入延迟 | 等待指定秒数后再寻找 `libil2cpp.so` | 立即开始 |
| 渲染缩放 | 按比例调用 `Screen::SetResolution` / `ScreenManager` | 不修改分辨率 |
| 显示模式 | 设置窗口的 `preferredDisplayModeId` | 不 Hook 显示模式 |

- 支持 `arm64-v8a`、`armeabi-v7a`、`x86`、`x86_64`。
- 每个应用可独立启用/禁用，并选择继承全局默认值或使用独立参数。
- 支持搜索应用、显示系统应用、按开关申请 Xposed 作用域。
- Hook 日志本地保存，不依赖 logcat。

## 使用方法

1. 安装 APK，在 LSPosed 等支持 libxposed API 101 的框架中启用模块。
2. 打开模块，在“应用管理器”中选中目标 Unity 游戏；首次选择会弹出框架的作用域授权。
3. 在“全局默认值”中配置通用参数；如需独立配置，在应用详情中关闭“使用全局默认值”。
4. 完全结束并重新启动目标游戏，使新配置生效。
5. 在“Hook 日志”中查看最近一次 Hook 结果。

若框架未激活，模块仍可打开并编辑配置，但无法自动申请作用域；需要手动在框架中勾选目标包名。

## 构建

需要 JDK 17、Android SDK 37、NDK `29.0.14206865`、CMake `3.22.1`。

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

产物：

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`（`local.properties` 提供签名时会签名）

## 项目结构

```
app/src/main/
├── cpp/                     # native IL2CPP Hook（xdl + JNI）
├── java/io/github/hexstr/UnityFPSUnlocker/
│   ├── config/              # 远程配置、令牌、默认值
│   ├── hook/                # XposedModule 入口、显示模式、native 加载、日志分发
│   ├── logging/             # 日志协议、接收器、环形存储
│   ├── nativebridge/        # Java JNI 桥
│   └── ui/                  # 原模块的 Compose + MIUIX 设置界面（已接入远程配置）
└── resources/META-INF/xposed/
    ├── java_init.list
    └── module.prop
```

## 来源与许可

- 原作者：[hexstr](https://github.com/hexstr)
- 上游项目：[hexstr/UnityFPSUnlocker](https://github.com/hexstr/UnityFPSUnlocker)
- 二改来源：[youshen2/UnityFPSUnlocker](https://github.com/youshen2/UnityFPSUnlocker)

本项目使用 MIT License，详见 `LICENSE`。
