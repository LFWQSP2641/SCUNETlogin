# SCUNET-login

一个面向安卓设备的 SCUNET 校园网登录工具，支持多账号管理、出口选择、一键登录，以及基于 Shizuku 的可选网络自动管理能力。

## 项目简介

本项目用于简化校园网认证流程。应用会根据已保存账号发起登录请求，并在应用内展示日志，方便排查登录问题。

当前版本为原生安卓应用，界面基于 Jetpack Compose，配置持久化使用 DataStore。

## 主要功能

- 多账号配置与管理（新增、编辑、删除、切换激活账号）
- 支持多种服务出口：教育网、电信、移动、联通
- 一键触发登录
- 登录日志查看与清空
- 可选启用 Shizuku 能力，在登录流程中自动管理 Wi-Fi（如切换/重连指定网络）

## 技术栈

- Kotlin
- Android Gradle Plugin
- Jetpack Compose + Material 3
- AndroidX Navigation Compose
- Ktor Client（OkHttp 引擎）
- Kotlinx Serialization
- DataStore Preferences
- Shizuku API

## 运行环境

- JDK 21
- Android Studio（建议使用较新稳定版）
- Android SDK：
  - minSdk 28
  - targetSdk 36
  - compileSdk 36（含 minorApiLevel 1）

## 构建与安装

在仓库根目录执行（Windows）：

```powershell
cd SCUNETlogin
.\gradlew.bat assembleDebug
```

调试包输出目录通常为：

- `SCUNETlogin/app/build/outputs/apk/debug/`

如需发布构建：

```powershell
cd SCUNETlogin
.\gradlew.bat assembleRelease
```

## 使用说明

1. 打开应用后，先进入账号编辑页新增账号。
2. 填写名称、学号、密码，并选择服务出口。
3. 回到首页，确认当前激活账号。
4. 点击登录按钮执行认证。
5. 如需查看过程与错误信息，可切换到日志页。

## Shizuku 可选功能说明

- 首页提供“登录时自动管理网络（Shizuku）”开关。
- 开启该能力前，请先确保设备已正确安装并授权 Shizuku。
- 若未授权或不可用，应用会按普通模式执行登录。

## 注意事项

- 本项目为校园网场景工具，请仅在合法、授权的网络环境中使用。
- 账号密码属于敏感信息，请妥善保管设备并谨慎分发安装包。
- 不同学校或网络环境的认证页面可能存在差异，若登录失败请先查看日志页信息。

## 项目结构（简要）

- `SCUNETlogin/app/src/main/java/.../ui/`：界面与导航
- `SCUNETlogin/app/src/main/java/.../viewmodel/`：状态管理与业务编排
- `SCUNETlogin/app/src/main/java/.../service/`：登录请求与网络流程
- `SCUNETlogin/app/src/main/java/.../data/`：数据模型与持久化配置

## 许可证

本项目使用仓库中的许可证文件，详见 `LICENSE.txt`。
