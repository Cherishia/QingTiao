# 贡献指南

## 报告问题

通过 [问题反馈表](https://github.com/Cherishia/QingTiao/issues/new?template=bug_report.yml) 提交轻跳版本、Android 版本、受影响的应用、复现步骤和实际结果。

遇到漏跳或误点时，可以在轻跳首页打开「抓取记录」，选择对应的应用，再附上与问题有关的片段。提交前请移除账号、消息等个人信息。

## 提交改动

1. 从 `main` 创建分支，按 [README 的构建步骤](README.md#从源码构建) 运行测试和本地构建。
2. 在 Pull Request 中说明复现条件、改动和验证结果。涉及识别规则时，请覆盖应识别及应忽略的场景。涉及调度时，请说明后台扫描或截图次数是否有变化。
3. 使用实际设备验证时，请写明设备和 Android 版本。通过日志或截图回放验证时，请注明回放，可以参考[历史验证记录](VALIDATION.md)。

## 发布版本

维护者发布时递增 `versionCode`，使用与 `versionName` 一致的 Git 标签，并将原证书签名的 `QingTiao-版本号.apk` 上传到正式 Release。源码包应对应同一标签，并包含 [LICENSE](LICENSE) 和 [README.md](README.md)。
