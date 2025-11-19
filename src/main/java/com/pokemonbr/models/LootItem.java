package com.pokemonbr.models;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
     * 创建ItemStack物品
     * @return ItemStack
     */
    public ItemStack createItemStack() {
        Random random = new Random();
        int amount = minAmount;
        if (maxAmount > minAmount) {
            amount = minAmount + random.nextInt(maxAmount - minAmount + 1);
        }

        if (isPixelmonItem) {
            // TODO: 实现Pixelmon物品创建
            // 目前返回空气，表示需要通过反射创建
            return new ItemStack(Material.AIR);
        }

        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // 设置名称
            if (name != null && !name.isEmpty()) {
                meta.setDisplayName(name);
            }

            // 设置Lore
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }

            // 添加附魔
            for (EnchantmentData enchantData : enchantments) {
                if (enchantData != null && enchantData.getEnchantment() != null) {
                    meta.addEnchant(enchantData.getEnchantment(), enchantData.getLevel(), true);
                }
            }

            item.setItemMeta(meta);
        }

        return item;
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
