package com.pokemonbr.models;

/**
 * 游戏状态枚举
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public enum GameState {
    /**
     * 准备中 - 玩家正在传送到地图
     */
    PREPARING,

    /**
     * 无敌时间 - 游戏开始，玩家处于无敌状态
     */
    INVINCIBILITY,

    /**
     * 进行中 - 正常游戏状态
     */
    PLAYING,

    /**
     * 最终缩圈 - 进入最后阶段
     */
    FINAL_STAGE,

    /**
     * 结束中 - 游戏结束，正在处理奖励
     */
    ENDING,

    /**
     * 已结束 - 游戏完全结束
     */
    FINISHED
}
