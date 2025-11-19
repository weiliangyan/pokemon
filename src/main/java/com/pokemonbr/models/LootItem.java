package com.pokemonbr.models;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.List;

/**
 * 战利品物品模型
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class LootItem {

    private final Material material;
    private final String name;
    private final int minAmount;
    private final int maxAmount;
    private final int chance;
    private final List<EnchantmentData> enchantments;
    private final List<String> lore;

    // Pixelmon物品支持
    private final boolean isPixelmonItem;
    private final String pixelmonItemId;

    public LootItem(Material material, String name, int minAmount, int maxAmount, int chance,
                    List<EnchantmentData> enchantments, List<String> lore) {
        this.material = material;
        this.name = name;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.chance = chance;
        this.enchantments = enchantments != null ? enchantments : new ArrayList<>();
        this.lore = lore != null ? lore : new ArrayList<>();
        this.isPixelmonItem = false;
        this.pixelmonItemId = null;
    }

    /**
     * Pixelmon物品构造函数
     */
    public LootItem(String pixelmonItemId, String name, int minAmount, int maxAmount, int chance,
                    List<EnchantmentData> enchantments, List<String> lore) {
        this.material = null;
        this.name = name;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.chance = chance;
        this.enchantments = enchantments != null ? enchantments : new ArrayList<>();
        this.lore = lore != null ? lore : new ArrayList<>();
        this.isPixelmonItem = true;
        this.pixelmonItemId = pixelmonItemId;
    }

    public Material getMaterial() {
        return material;
    }

    public boolean isPixelmonItem() {
        return isPixelmonItem;
    }

    public String getPixelmonItemId() {
        return pixelmonItemId;
    }

    public String getName() {
        return name;
    }

    public int getMinAmount() {
        return minAmount;
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    public int getChance() {
        return chance;
    }

    public List<EnchantmentData> getEnchantments() {
        return enchantments;
    }

    public List<String> getLore() {
        return lore;
    }

    /**
     * 附魔数据类
     */
    public static class EnchantmentData {
        private final Enchantment enchantment;
        private final int level;

        public EnchantmentData(Enchantment enchantment, int level) {
            this.enchantment = enchantment;
            this.level = level;
        }

        public Enchantment getEnchantment() {
            return enchantment;
        }

        public int getLevel() {
            return level;
        }
    }
}
