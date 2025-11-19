package com.pokemonbr.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 战利品品类模型
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class LootCategory {

    private final String name;
    private final int weight;
    private final List<LootItem> items;
    // 新增字段，支持loot-system.yml配置
    private final int minSlots;
    private final int maxSlots;
    private final int minStack;
    private final int maxStack;

    // 原有构造器（向后兼容）
    public LootCategory(String name, int weight) {
        this(name, weight, 1, 3, 1, 1);
    }

    // 新的完整构造器，支持loot-system.yml配置
    public LootCategory(String name, int weight, int minSlots, int maxSlots, int minStack, int maxStack) {
        this.name = name;
        this.weight = weight;
        this.items = new ArrayList<>();
        this.minSlots = minSlots;
        this.maxSlots = maxSlots;
        this.minStack = minStack;
        this.maxStack = maxStack;
    }

    public String getName() {
        return name;
    }

    public int getWeight() {
        return weight;
    }

    public List<LootItem> getItems() {
        return items;
    }

    public void addItem(LootItem item) {
        items.add(item);
    }

    // 新增的getter方法
    public int getMinSlots() {
        return minSlots;
    }

    public int getMaxSlots() {
        return maxSlots;
    }

    public int getMinStack() {
        return minStack;
    }

    public int getMaxStack() {
        return maxStack;
    }

    /**
     * 计算随机槽位数量
     * @return 槽位数量
     */
    public int getRandomSlotCount() {
        if (minSlots >= maxSlots) {
            return minSlots;
        }
        return minSlots + (int)(Math.random() * (maxSlots - minSlots + 1));
    }

    /**
     * 计算随机堆叠数量
     * @return 堆叠数量
     */
    public int getRandomStackCount() {
        if (minStack >= maxStack) {
            return minStack;
        }
        return minStack + (int)(Math.random() * (maxStack - minStack + 1));
    }

    /**
     * 计算类别中的总物品权重
     * @return 总权重
     */
    public int calculateTotalItems() {
        int total = 0;
        for (LootItem item : items) {
            total += item.getChance();
        }
        return total;
    }

    /**
     * 根据权重随机选择指定数量的物品
     * @param count 要选择的物品数量
     * @return 选择的物品列表
     */
    public List<LootItem> selectRandomItems(int count) {
        List<LootItem> selectedItems = new ArrayList<>();
        List<LootItem> availableItems = new ArrayList<>(items);

        Random random = new Random();

        for (int i = 0; i < count && !availableItems.isEmpty(); i++) {
            int totalWeight = availableItems.stream().mapToInt(LootItem::getChance).sum();
            int randomWeight = random.nextInt(totalWeight);

            int currentWeight = 0;
            LootItem selectedItem = null;

            for (LootItem item : availableItems) {
                currentWeight += item.getChance();
                if (randomWeight < currentWeight) {
                    selectedItem = item;
                    break;
                }
            }

            if (selectedItem != null) {
                selectedItems.add(selectedItem);
                // 避免重复选择同一物品（可选）
                // availableItems.remove(selectedItem);
            } else {
                break; // 没有找到合适的物品
            }
        }

        return selectedItems;
    }
}
