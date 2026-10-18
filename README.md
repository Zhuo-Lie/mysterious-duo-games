# 神秘双人小游戏 · Mysterious Duo Games

**一台手机，两个人，随时来一局。**<br>
**One phone. Two players. A friendly rivalry, anytime.**

原生安卓同屏双人小游戏合集：红蓝对抗、粗线条和暖纸感界面。支持面对面操作，核心玩法离线运行，无需注册账号；当前版本未接入广告或内购。

A native Android collection of local two-player mini-games with bold outlines, warm paper-inspired UI, and red-versus-blue controls. Play face to face on one device. Core gameplay works offline, with no account required. The current version has no ads or in-app purchases.

![Android](https://img.shields.io/badge/Android-6.0%2B-3DDC84?logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-native-orange)
![Version](https://img.shields.io/badge/version-4.7-blue)

[中文介绍](#中文介绍) · [English](#english) · [反馈 / Issues](../../issues)

## 界面预览 · Preview

以下图片由 Android 测试环境渲染，并非真机截图。<br>
Rendered in the Android test environment; these are not physical-device screenshots.

<p align="center">
  <img src="docs/界面预览/lobby-native-test.png" width="220" alt="游戏大厅 / Game lobby">
  <img src="docs/界面预览/sumo.png" width="220" alt="相扑 / Sumo">
  <img src="docs/界面预览/tank-cover-cracked.png" width="220" alt="坦克对战 / Tank duel">
</p>

## 中文介绍

### 九款小游戏

| 游戏 | 玩法亮点 |
| --- | --- |
| 相扑 | 持续点击发力，把对手推出场地；跨步与角力反馈，可调推力 |
| 弹球 | 拖动球拍回击；可选渐进加速，对拉拍数与精准回击提示 |
| 台球 | 十个目标球加白球；随机先手、首颗合法进球分色、自由球摆放、有限反弹预瞄 |
| 冰上桌球 | 拖动球槌进攻与防守；可调节奏、获胜分数和球体大小 |
| 机关弹珠 | 左右挡板独立控制；移动弹柱、旋转横杆与混合布局，可轮换场地 |
| 赛车 | 一人一个摇杆；三张赛道、顺序检查点、引擎声和尾部尘埃 |
| 坦克对战 | 每人两个摇杆，分别控制履带移动与炮塔；炮弹对消、反弹和可破坏掩体 |
| 井字棋 | 轮流落子，三子连线获胜；支持悔棋与平局 |
| 五子棋 | 预选位置再确认；自由规则、五子连线，不设禁手 |

双方准备后开始，结算后可再战。提供音量调节、各游戏独立选项和本机累计比分。界面主要为中文，README 提供中英双语说明。

### 获取与编译

当前源码版本为 **v4.7**。安装包未纳入 Git，本仓库目前不附带 APK 下载；本机的 `安装包/当前版本/` 也不会随克隆出现。

主要环境：Windows PowerShell、JDK 17、Android SDK Platform 35 和 Build Tools 35.0.0。应用最低支持 Android 6.0（API 23）。现有脚本依赖本机工具目录，不是克隆后立即可用的完整工具链。

```powershell
git clone https://github.com/Zhuo-Lie/mysterious-duo-games.git
cd mysterious-duo-games
# 先准备下述目录中的工具，再执行：
./build-apk.ps1
```

脚本默认工具路径：

```text
.tools/jdk/jdk-17.0.20.1+1/
.tools/android-sdk/platforms/android-35/android.jar
.tools/android-sdk/build-tools/35.0.0/
```

编译结果位于 `app/build/outputs/apk/debug/app-debug.apk`，并复制到本机 `安装包/当前版本/`。这是开发调试签名；请使用自己的密钥，不要索取或上传维护者的签名文件。工具路径和密钥说明见 [Git 版本管理说明](docs/Git版本管理说明.md)。仓库尚未提供 Gradle Wrapper。

### 验证与已知限制

- `tools/test-project.ps1`：规则测试；Node 可用时检查 19 个音效资源。Node 路径目前依赖维护者本机环境，缺失时脚本会提示跳过音频检查。
- `tools/test-android-ui.ps1`：Android 视图测试，依赖额外的本机 Maven/Robolectric 环境，不随仓库分发。
- 自动检查不能替代真机试玩；多指响应、声音和性能仍需在具体设备上验证。
- 已知问题：相扑切到后台会重置当前对局，尚待修复。
- 微信小游戏移植处于规划阶段，当前仓库不是已经发布的微信小游戏。

### 文档与反馈

[开始使用](开始使用.md) · [试玩清单](docs/试玩清单.md) · [v4.7 开发记录](docs/v4.7开发记录.md) · [完整预览](docs/界面预览/README.md) · [微信移植大纲](docs/微信小游戏上架与移植大纲.md)

欢迎通过 [Issues](../../issues) 提交问题或建议。请说明游戏名称、手机型号、Android 版本、复现步骤与期望结果；可以附上截图或录屏，注意隐藏个人信息。部分历史文档描述的是当时版本，当前行为以源码和最新记录为准。

## English

### Games

| Game | Highlights |
| --- | --- |
| Sumo | Tap repeatedly to push your opponent out of the ring; punchy steps and adjustable strength |
| Paddle Duel | Drag your paddle to return the ball; optional gradual acceleration and rally feedback |
| Pool | Ten colored targets plus a cue ball; random opening player, groups assigned by the first legal pot, ball in hand, and limited aiming guides |
| Air Hockey | Drag your mallet to attack and defend; adjustable pace, score target, and puck/mallet size |
| Pinball Duel | Independent left/right flippers; moving bumpers, rotating bars, and changing layouts |
| Racing | One joystick per player; three tracks, ordered checkpoints, engine audio, and dust particles |
| Tank Duel | Two joysticks per player for tracked movement and turret aiming; shell cancellation, ricochets, and destructible cover |
| Tic-Tac-Toe | Take turns to make a line of three; undo and draw support |
| Gomoku | Preview and confirm your move; freestyle five-in-a-row without forbidden moves |

Players ready up before each match. The collection includes rematches, persistent game options, separate audio controls, and local cumulative scores. The in-game interface is primarily Chinese; this README is bilingual.

### Build and test

The current source version is **v4.7**, targeting Android API 35 with a minimum of API 23. This is a Java/Android Canvas project. It has no Gradle Wrapper and does not bundle an APK, JDK, Android SDK, signing key, or the local UI-test dependencies.

On Windows, prepare JDK 17 and Android SDK Platform 35 / Build Tools 35.0.0 at the paths shown above, then run `./build-apk.ps1` from PowerShell. The script produces a debug-signed APK at `app/build/outputs/apk/debug/app-debug.apk`. Keep your signing keys private.

Run `./tools/test-project.ps1` for model tests. Audio validation requires Node at the script's configured local path; otherwise it is explicitly skipped. Android view tests need the additional local Maven/Robolectric setup. Physical-device testing is still required for multi-touch response, audio, and performance.

**Known limitation:** backgrounding Sumo currently resets the ongoing match. A WeChat mini-game port is planned; this repository does not contain a released WeChat version.

### Feedback and project layout

Report bugs or ideas in [Issues](../../issues), in Chinese or English. Include the game, device model, Android version, reproduction steps, and expected behavior. Please remove personal information from screenshots and recordings.

```text
app/           Android source, graphics, and audio assets
tests/         Pure Java rule tests and audio validation
tools/         Test scripts and audio-generation tools
docs/          Design notes, version history, and rendered previews
AGENTS.md      Project collaboration guidelines
build-apk.ps1  Local Windows build script
```

### 权利与素材 · Rights and assets

本仓库公开源码用于展示和交流，目前未附加开源许可证；不应将“公开可见”理解为任意复制、商用或再分发授权。图标含用户提供的角色参考及生成素材；商业发行前需要另行核实素材权利并完善原创品牌。

This repository is publicly viewable for demonstration and discussion, but currently has no open-source license. Public visibility should not be treated as a general grant to copy, commercialize, or redistribute the project. The icon uses a user-supplied character reference and generated artwork; asset rights and original branding need further review before commercial distribution.
