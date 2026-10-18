# 本机安卓视图渲染预览

这些图片由 Robolectric 的安卓 View 与原生图形渲染生成，使用项目实际代码和已编译 APK 的资源。**这是桌面测试环境的渲染图，不是真机截图。** 新游戏预览采用测试静音状态，手机默认声音仍以本地设置为准。

| 大厅 | 相扑 |
| --- | --- |
| ![大厅](lobby-native-test.png) | ![相扑](sumo.png) |

| 弹球（原乒乓球） | 冰上桌球 |
| --- | --- |
| ![弹球](ping_pong.png) | ![冰上桌球](air_hockey.png) |

| 台球 | 机关弹珠 |
| --- | --- |
| ![台球](pool.png) | ![机关弹珠](pinball.png) |

| 赛车 | 井字棋 |
| --- | --- |
| ![赛车](racing.png) | ![井字棋](tic_tac_toe.png) |

![五子棋](gomoku.png)

开发者可以运行 `tools/test-android-ui.ps1` 重新生成。此检查会验证 Android 9 与 Android 15，测试准备、触摸、暂停、导航和本地设置等行为，不替代真机试玩。

v4.0 面对面台球瞄准状态（按钮跟随当前玩家换端）：

![首次碰撞瞄准提示](pool-aim.png)

![双端结算和再战](facing-result.png)

## v4.0 新场景

![坦克对战](tank.png)

| 台球分色示例（蓝方打红球） | 纸卡玩法调节 |
| --- | --- |
| ![分色](pool-assigned.png) | ![调节](game-options.png) |

| 赛车三图 | 坦克三图 | 机关弹珠三组 |
| --- | --- | --- |
| ![环线](racing-map-0.png) | ![交错掩体](tank-map-0.png) | ![双星](pinball-map-0.png) |
| ![街区](racing-map-1.png) | ![十字回廊](tank-map-1.png) | ![四角](pinball-map-1.png) |
| ![沙丘](racing-map-2.png) | ![双岛](tank-map-2.png) | ![双塔](pinball-map-2.png) |

## v4.1 移动机关瞬间

![移动机关](pinball-moving.png)

台球、赛车与各地图图片已由 v4.1 的原生 View 测试重新生成。

## v4.2 动态反馈

![车尾尘埃](racing-dust.png)

![旋转横杆](pinball-moving.png)

大厅已包含 5 秒推荐轮播和暂停按钮。以上仍为原生 View 桌面测试渲染。

## v4.4 图片轮播

![台球推荐](lobby-carousel-pool.png)

![坦克推荐](lobby-carousel-tank.png)

卡片使用对应游戏插画，标题旁保留缩小的小鸡封面；这些是桌面原生 View 渲染图。

## v4.7 自由球与破坏掩体

![自由球摆放](pool-free-ball-blue.png)

![掩体裂纹](tank-cover-cracked.png)

![掩体打碎](tank-cover-destroyed.png)
