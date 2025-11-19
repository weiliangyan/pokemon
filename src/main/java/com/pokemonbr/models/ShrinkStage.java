package com.pokemonbr.models;

/**
 * 缩圈阶段配置
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class ShrinkStage {

    private final int stageNumber;
    private final int targetSize;
    private final int duration;
    private final int delay;

    public ShrinkStage(int stageNumber, int targetSize, int duration, int delay) {
        this.stageNumber = stageNumber;
        this.targetSize = targetSize;
        this.duration = duration;
        this.delay = delay;
    }

    public int getStageNumber() {
        return stageNumber;
    }

    public int getTargetSize() {
        return targetSize;
    }

    public int getDuration() {
        return duration;
    }

    public int getDelay() {
        return delay;
    }

    @Override
    public String toString() {
        return "ShrinkStage{" +
                "stage=" + stageNumber +
                ", size=" + targetSize +
                ", duration=" + duration + "s" +
                ", delay=" + delay + "s" +
                '}';
    }
}
