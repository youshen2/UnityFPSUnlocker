# Unity FPS Unlocker

面向 Unity IL2CPP 游戏的 Xposed 模块，可按应用设置目标帧率、注入延迟、渲染缩放和显示模式。管理界面使用 MIUIX 与 Jetpack Compose 重构。

> 修改游戏运行参数可能造成画面异常、闪退或违反游戏服务条款，请自行评估风险。

## 功能

- MIUIX 风格设置界面及页面切换动画。
- 真正的 per-app 管理：点击应用进入详情页，可单独控制是否生效、是否使用全局默认值，以及各项独立参数。
- 已启用独立管理的应用在列表顶部单独分组；关闭“对该应用生效”后回到普通应用列表。
- 目标 FPS、注入延迟、渲染缩放和显示模式均有独立开关；关闭后不修改对应参数，同时保留上一次填写的数值。
- 可选择仅处理应用管理器中已启用的应用。
- 在需要时动态申请 MIUI/HyperOS 的应用列表权限，并支持搜索及显示系统应用。
- Hook 结果在本地保存，不依赖 logcat；日志页展示最近 200 条成功、失败和跳过记录。
- 支持 `arm64-v8a`、`armeabi-v7a`、`x86`、`x86_64`。

## 环境要求

- Android 9（API 28）或更高版本。
- 支持现代 Xposed API 的框架，推荐 LSPosed；模块声明的最低 Xposed 版本为 93。
- 目标应用使用 Unity IL2CPP。

## 使用方法

1. 安装 APK，在 Xposed 管理器中启用模块，并勾选需要处理的目标应用。
2. 打开模块设置。首次进入应用管理器时，按提示授予“获取应用列表”权限。
3. 在“全局默认值”中设置通用参数；不希望修改的项目直接关闭右侧开关。
4. 如需独立配置，进入“应用管理器”，点击目标应用，开启“对该应用生效”，再关闭“使用全局默认值”并调整参数。
5. 完全结束并重新启动目标应用，使新的 Hook 配置生效。
6. 可在“Hook 日志”中刷新查看最近的执行结果。

## 参数说明

| 参数 | 作用 | 关闭开关后 |
| --- | --- | --- |
| 目标 FPS | 设置 Unity 目标帧率 | 不修改目标帧率 |
| 注入延迟 | 等待指定秒数后初始化 native Hook | 不等待 |
| 渲染缩放 | 按比例调整 Unity 渲染分辨率 | 不调用分辨率修改逻辑 |
| 显示模式 | 设置窗口的首选显示模式 | 不 Hook 显示模式 |
| 修改指令 | 修改目标帧率函数指令，避免后续覆盖 | 不修改函数指令；仅影响 FPS 设置 |

“仅处理受管理应用”开启后，没有在应用详情中启用“对该应用生效”的进程会被直接跳过。

## 构建

项目使用 JDK 17、Android SDK 36、NDK 29 和 CMake 3.22.1。

```bash
bash gradlew assembleDebug
bash gradlew assembleRelease
```

构建产物位于：

- Debug：`app/build/outputs/apk/debug/app-debug.apk`
- Release：`app/build/outputs/apk/release/app-release-unsigned.apk`

Release APK 默认未签名，需要自行签名后安装。

## 项目来源

- 原作者：[hexstr](https://github.com/hexstr)
- 上游项目：[hexstr/UnityFPSUnlocker](https://github.com/hexstr/UnityFPSUnlocker)
- 二改作者：爅峫
- 当前源码仓库：[youshen2/UnityFPSUnlocker](https://github.com/youshen2/UnityFPSUnlocker)

## 许可

本项目使用 [MIT License](LICENSE)。
