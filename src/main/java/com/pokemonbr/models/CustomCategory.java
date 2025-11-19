package com.pokemonbr.models;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义品类数据模型
 * 对应 tianchonxz.toml 中的品类配置
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class CustomCategory {

    private String name;           // 品类名称
    private int minSlots;         // 最小填充格数
    private int maxSlots;         // 最大填充格数
    private int minStack;         // 最小堆叠数量
    private int maxStack;         // 最大堆叠数量
    private double fillChance;     // 填充概率 (0.0-1.0)
    private boolean enabled;       // 是否启用

    public CustomCategory() {
        this.enabled = true;
        this.fillChance = 1.0;  // 默认100%填充
    }

    public CustomCategory(String name, int minSlots, int maxSlots, int minStack, int maxStack) {
        this.name = name;
        this.minSlots = Math.max(0, Math.min(999, minSlots));
        this.maxSlots = Math.max(0, Math.min(999, maxSlots));
        this.minStack = Math.max(0, Math.min(999, minStack));
        this.maxStack = Math.max(0, Math.min(999, maxStack));
        this.enabled = true;
        this.fillChance = 1.0;
    }

    // ==================== Getter和Setter方法 ====================

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMinSlots() {
        return minSlots;
    }

    public void setMinSlots(int minSlots) {
        this.minSlots = Math.max(0, Math.min(999, minSlots));
    }

    public int getMaxSlots() {
        return maxSlots;
    }

    public void setMaxSlots(int maxSlots) {
        this.maxSlots = Math.max(0, Math.min(999, maxSlots));
    }

    public int getMinStack() {
        return minStack;
    }

    public void setMinStack(int minStack) {
        this.minStack = Math.max(0, Math.min(999, minStack));
    }

    public int getMaxStack() {
        return maxStack;
    }

    public void setMaxStack(int maxStack) {
        this.maxStack = Math.max(0, Math.min(999, maxStack));
    }

    public double getFillChance() {
        return fillChance;
    }

    public void setFillChance(double fillChance) {
        this.fillChance = Math.max(0.0, Math.min(1.0, fillChance));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // ==================== 实用方法 ====================

    /**
     * 验证配置是否有效
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() &&
               minSlots <= maxSlots &&
               minStack <= maxStack &&
               minSlots >= 0 && maxSlots <= 999 &&
               minStack >= 0 && maxStack <= 999;
    }

    /**
     * 随机生成填充格数
     */
    public int getRandomSlotCount() {
        if (minSlots == maxSlots) {
            return minSlots;
        }
        return minSlots + (int)(Math.random() * (maxSlots - minSlots + 1));
    }

    /**
     * 随机生成堆叠数量
     */
    public int getRandomStackCount() {
        if (minStack == maxStack) {
            return minStack;
        }
        return minStack + (int)(Math.random() * (maxStack - minStack + 1));
    }

    /**
     * 检查是否应该填充
     */
    public boolean shouldFill() {
        return enabled && Math.random() < fillChance;
    }

    @Override
    public String toString() {
        return String.format("CustomCategory{name='%s', slots=%d-%d, stack=%d-%d, chance=%.1f%%, enabled=%s}",
                name, minSlots, maxSlots, minStack, maxStack, fillChance * 100, enabled);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomCategory that = (CustomCategory) o;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    /**
     * 获取默认物品列表
     */
    public List<String> getDefaultItems() {
        return new ArrayList<>(); // 返回空列表，可以在需要时扩展
    }
}
