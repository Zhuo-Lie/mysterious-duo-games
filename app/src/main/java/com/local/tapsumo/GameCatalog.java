package com.local.tapsumo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** One registry for lobby cards, previews and future game entry points. */
final class GameCatalog {
    enum Id { SUMO, PING_PONG, POOL, TIC_TAC_TOE, AIR_HOCKEY, PINBALL, RACING, GOMOKU, TANK }
    enum Category {
        ALL("全部"), ACTION("拼反应"), BALL("球类"), BOARD("棋类");
        final String label;
        Category(String label) { this.label=label; }
    }
    static final class Game {
        final Id id;
        final String name, tagline, controls, goal, rule;
        final Category category;
        final boolean playable;
        Game(Id id,String name,String tagline,Category category,boolean playable,String controls,String goal,String rule) {
            this.id=id;this.name=name;this.tagline=tagline;this.category=category;
            this.playable=playable;this.controls=controls;this.goal=goal;this.rule=rule;
        }
    }
    static final List<Game> ALL=Collections.unmodifiableList(Arrays.asList(
        new Game(Id.SUMO,"相扑","一人一边，较量手速",Category.ACTION,true,
            "两人面对面，各自连续点击自己的按钮。长按不会连发。",
            "把对方推出相扑垫，就赢下这一局。",
            "每次点击都向前发力。双方持续角力，点击频率和节奏决定推进；推力可在场内调节。"),
        new Game(Id.PING_PONG,"弹球","接住，再漂亮地回击",Category.BALL,true,
            "两人各守一端，左右拖动球拍接球。",
            "把球打过对方的底线得分，先得 5 分获胜。",
            "球拍接球会逐渐加速；击球位置与移动速度影响回球方向，先找准落点再反击。"),
        new Game(Id.POOL,"台球","瞄准这一杆",Category.BALL,true,
            "随机先手。轮流点或拖桌面瞄准，拖自己一端的力度条，再点击球。白球落袋后，对方可自由选位置，确认摆球后再击球。",
            "共十个目标球。首颗合法进球决定目标颜色，每方清空所属五球获胜。",
            "玩家红蓝身份与球色无关。进目标球继续，未进换人，白球落袋由对方自由摆球；未分色时犯规进的球放回。清台白球同落判负。预瞄限长、最多一次反弹，可调或关闭。"),
        new Game(Id.TIC_TAC_TOE,"井字棋","三步成线，也要防守",Category.BOARD,true,
            "两人轮流点击空格，分别落下圈和叉。",
            "横、竖或斜向连成 3 个获胜，棋盘填满则平局。",
            "经典 3 × 3 棋盘。同一部手机轮流落子，适合随手来一局。"),
        new Game(Id.AIR_HOCKEY,"冰上桌球","守住球门，反弹进攻",Category.BALL,true,
            "各自在半场内拖动球槌，击打中央的球。",
            "把球打进对方球门得分，先得 5 分获胜。",
            "利用边墙反弹创造机会。双方球槌限制在各自半场，减少手指遮挡。"),
        new Game(Id.PINBALL,"机关弹珠","左右出击，每回合有变局",Category.BALL,true,
            "各自在自己一端点击左侧或右侧，分别弹起对应挡板；两边可同时按。",
            "把球打进对方中央失球口得分，先得 5 分获胜。",
            "更宽的得分口，三组对称障碍；默认每个得分回合随机换组。看准时机接球，长按不连发。速度、目标分数与变局可调。"),
        new Game(Id.RACING,"赛车","摇杆驾驶，抢先冲线",Category.ACTION,true,
            "每人一个摇杆。朝想去的方向推动车辆，松手减速；大幅转向需要提前留量。",
            "依次通过自己的圆环检查点，先完成设定圈数获胜。",
            "三条不同赛道。驶出道路会减速，必须按顺序过检查点；默认两圈，可调车速、灵敏度和场地轮换。"),
        new Game(Id.GOMOKU,"五子棋","落一子，想下一步",Category.BOARD,true,
            "两人轮流点选棋盘交点，再点确认落子。先手执黑，后手执白；再战时交换先手。",
            "横、竖或斜向，率先连成 5 子获胜。",
            "15 × 15 自由规则，不设禁手，超过五子也获胜。双方同意可以撤回最近一手；再战会交换先手。"),
        new Game(Id.TANK,"坦克对战","履带走位，转炮反击",Category.ACTION,true,
            "每人两个摇杆：左侧控制履带移动，右侧瞄准并持续开火。炮塔与车身独立转动。",
            "击毁对方得分，先达到设定分数获胜；默认三分，每车三格装甲。",
            "车身沿朝向行驶，可原地转向、倒车，不能横着平移。利用掩体和一次反弹射击；每回合安全换地图，复活短暂保护，开火会解除保护。")
    ));
    static Game find(Id id) {
        for(Game game:ALL)if(game.id==id)return game;
        throw new IllegalArgumentException("Unknown game: "+id);
    }
    private GameCatalog() {}
}
