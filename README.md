# 轻跳 · QingTiao

轻跳是一款 Android 广告跳过工具。它通过无障碍服务识别开屏广告、浏览弹窗和视频广告面板，并在确认当前窗口和目标后点击跳过或关闭。

[下载最新版](https://github.com/Cherishia/QingTiao/releases/latest) · [更新记录](https://github.com/Cherishia/QingTiao/releases) · [反馈问题](https://github.com/Cherishia/QingTiao/issues/new/choose)

## 安装与更新

- 支持 Android 8 及以上。画面识别需要 Android 11 及以上。
- 在 GitHub 发布页的 **Assets** 中下载 `QingTiao-版本号.apk`。安装后打开轻跳，在「无障碍设置」中启用轻跳，再打开「自动跳过广告」。
- 从 0.5.40 起，可以点首页底部版本号旁的「检查更新」。有新版时轻跳会下载并校验安装包，随后打开 Android 安装界面。首次更新可能需要允许轻跳安装应用。

## 功能

| 功能 | 用途 |
| --- | --- |
| 跳过开屏广告 | 优先识别控件。部分没有文字控件的广告可用画面识别。 |
| 处理浏览广告 | 可选，用于关闭支持的弹窗和视频广告面板。 |
| 摇一摇跳转返回 | 发现广告证据时短时监听摇动，对符合条件的浏览器或商店跳转尝试返回。 |
| 不处理的应用 | 按应用排除识别和操作。应用要求关闭无障碍时，可从「停用并重开」进入，用完后在系统设置重新启用轻跳。 |
| 恢复提醒 | 无障碍服务长时间未重连或被关闭时，可发送通知。 |
| 抓取记录 | 查看最近应用的识别结果、漏跳原因和点击过程。 |

识别范围受应用界面和系统版本影响。遇到漏跳或误点，可以从「抓取记录」复制相关采样。

## 权限与数据

- 无障碍服务用于读取当前界面的控件、申请按需截图以及执行点击。画面在手机内存中处理，轻跳只在本地保留识别摘要。
- 界面事件触发识别，锁屏后暂停。清理后恢复提醒使用系统通知权限。
- 点击「检查更新」才会连接 GitHub，发送的是版本查询和安装包下载请求。更新检查会核对包名、版本与签名。

## 从源码构建

准备 Python 3、JDK 17、Android SDK Platform 36 和 Android Build Tools（如 35.0.0）。克隆本仓库，在项目根目录运行以下命令。将参数换成本机路径。

```powershell
python build.py --java-home 'C:/path/to/jdk-17' --build-tools 'C:/path/to/Android/Sdk/build-tools/35.0.0' --android-jar 'C:/path/to/Android/Sdk/platforms/android-36/android.jar' --out './QingTiao.apk'
```

脚本会先运行回归测试，再编译并签名。可以用 `--build-dir` 指定构建目录。首次本地构建会生成测试签名。覆盖安装 GitHub 正式版时，请使用发布页提供的同签名 APK。

历史测试细节见[验证记录](VALIDATION.md)。

## 反馈与贡献

欢迎通过[问题反馈表](https://github.com/Cherishia/QingTiao/issues/new?template=bug_report.yml)报告问题，也可以提交 [Pull Request](https://github.com/Cherishia/QingTiao/pulls)。提交改动、测试和发布的步骤见[贡献指南](CONTRIBUTING.md)。

## 许可与素材

轻跳的源代码、构建脚本和测试采用 [MIT 许可证](LICENSE)。再发布代码时，请保留版权声明和许可证文本。

桌面图标和参考插图的授权另行处理，见[图标说明](artwork/README.md)。
