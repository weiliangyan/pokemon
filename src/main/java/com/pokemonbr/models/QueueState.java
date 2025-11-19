package com.pokemonbr.models;

/**
 * 队列状态枚举
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public enum QueueState {
    /**
     * 等待中 - 等待玩家加入
     */
    WAITING,

    /**
     * 倒计时 - 人数已满，准备开始
     */
    COUNTDOWN,

    /**
     * 游戏中 - 游戏正在进行
     */
    PLAYING,

    /**
     * 已结束 - 游戏已结束
     */
    FINISHED
}
