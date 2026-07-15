# CoonImage

一个 Android APP：拍照后，通过**阿里云百炼 / DashScope** 调用可选大模型对照片做 AI 处理（换天空、换装、抠图、去除无关人员等），输入关键字即可，结果自动保存到手机存储根目录的 `CoonImage` 文件夹。API Key 输入一次后加密保存在本机。

## 功能
- 相机拍照（CameraX）
- 关键字驱动的图像编辑，内置关键字映射：
  - `换天空` → 替换天空
  - `换装` / `衣服` / `服装` → 换装
  - `抠图` / `去背景` / `扣图` → 抠出主体
  - `去人` / `去除` / `路人` / `无关人员` → 移除路人并补全背景
  - 其它关键字原样作为编辑指令
- 可选大模型（下拉切换）：
  - 通义万相·图像编辑（wanx-x-painting）
  - 通义万相·文生图（wanx2.1-t2i-plus）
  - 通义千问·智能路由（qwen-plus 理解关键字后调用图像编辑）
- 结果自动保存至 `/sdcard/CoonImage/`

## 准备
1. 注册阿里云百炼，开通 **通义万相** 与 **通义千问** 模型，获取 DashScope API Key。
2. 首次打开 APP 时粘贴 API Key（仅保存在本机加密存储）。

## 权限说明
- 相机：用于拍照。
- 所有文件访问（MANAGE_EXTERNAL_STORAGE）：用于在存储根目录创建并写入 `CoonImage` 文件夹（Android 11+ 需要）。APP 会在保存时引导你在系统设置中授予。

## 本地运行
用 Android Studio（Gradle 8.9 / AGP 8.5 / minSdk 26）打开 `CoonImageApp` 目录，连接真机运行。

## GitHub 远程编译（产出 APK）
本仓库已配置 GitHub Actions（`build.yml`）。把代码推送到 GitHub 仓库后，Actions 会自动构建并产出 APK：
1. 在自己账号下新建一个空仓库。
2. 推送本工程到该仓库（main/master 分支）。
3. 进入仓库 **Actions → Build Android APK**，等待完成。
4. 在 **Artifacts** 中下载 `coonimage-debug-apk`（即 `app-debug.apk`）。
5. 手机开启「允许安装未知来源应用」，安装该 APK 即可使用。

> 提示：debug 包已自带调试签名，可直接安装。
