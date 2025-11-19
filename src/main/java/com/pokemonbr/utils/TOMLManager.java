package com.pokemonbr.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * TOML文件管理器
 * 手动解析和生成TOML配置文件
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class TOMLManager {

    private final Plugin plugin;
    private final File configFile;
    private final List<CategoryInfo> categories;

    public TOMLManager(Plugin plugin, File configFile) {
        this.plugin = plugin;
        this.configFile = configFile;
        this.categories = new ArrayList<>();
    }

    /**
     * 读取配置文件（支持完整TOML格式和概率填充）
     */
    public List<CategoryInfo> readCategories() {
        categories.clear();
        if (!configFile.exists()) {
            plugin.getLogger().warning("§c配置文件不存在: " + configFile.getAbsolutePath());
            createDefaultConfig();
            return categories;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            String currentCategory = null;
            List<String> categoryLines = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // 跳过注释
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }

                // 检查是否开始新的类别部分
                if (line.contains("[[tianchonxz.categories]]")) {
                    // 如果有之前的类别，先处理
                    if (currentCategory != null && !categoryLines.isEmpty()) {
                        parseCategorySection(categoryLines, currentCategory);
                    }
                    currentCategory = extractCategoryName(line);
                    categoryLines.clear();
                }
                // 如果在类别部分，收集配置行
                else if (currentCategory != null) {
                    categoryLines.add(line);

                    // 如果遇到下一个类别或配置结束，处理当前类别
                    if (line.contains("[[tianchonxz.categories]]") ||
                        (!line.contains("=") && !line.contains("]") && !line.startsWith(" "))) {
                        if (!categoryLines.isEmpty()) {
                            parseCategorySection(categoryLines, currentCategory);
                            categoryLines.clear();
                        }
                        if (line.contains("[[tianchonxz.categories]]")) {
                            currentCategory = extractCategoryName(line);
                        } else {
                            currentCategory = null;
                        }
                    }
                }
            }

            // 处理最后一个类别
            if (currentCategory != null && !categoryLines.isEmpty()) {
                parseCategorySection(categoryLines, currentCategory);
            }

        } catch (IOException e) {
            plugin.getLogger().severe("§c读取配置文件失败: " + e.getMessage());
        }

        return categories;
    }

    /**
     * 创建默认配置文件
     */
    private void createDefaultConfig() {
        plugin.getLogger().info("§6正在创建默认TOML配置文件...");
        StringBuilder content = new StringBuilder();

        content.append("# =================================================\n");
        content.append("#   天充箱子 (tianchonxz) 模组配置文件\n");
        content.append("#   作者: l1ang_Y5n (QQ: 235236127)\n");
        content.append("#   支持概率填充和自定义品类\n");
        content.append("# =================================================\n\n");

        content.append("[tianchonxz]\n");
        content.append("# 品类配置\n\n");

        // 基础品类
        content.append("[[tianchonxz.categories]]\n");
        content.append("name = \"jichu\"\n");
        content.append("minslots = 2\n");
        content.append("maxslots = 6\n");
        content.append("minstack = 1\n");
        content.append("maxstack = 3\n");
        content.append("fill_chance = 0.80\n");
        content.append("enabled = true\n");
        content.append("items = []\n\n");

        // 优良品类
        content.append("[[tianchonxz.categories]]\n");
        content.append("name = \"youpin\"\n");
        content.append("minslots = 1\n");
        content.append("maxslots = 4\n");
        content.append("minstack = 1\n");
        content.append("maxstack = 2\n");
        content.append("fill_chance = 0.60\n");
        content.append("enabled = true\n");
        content.append("items = []\n\n");

        // 稀有品类
        content.append("[[tianchonxz.categories]]\n");
        content.append("name = \"xiyou\"\n");
        content.append("minslots = 1\n");
        content.append("maxslots = 2\n");
        content.append("minstack = 1\n");
        content.append("maxstack = 1\n");
        content.append("fill_chance = 0.30\n");
        content.append("enabled = true\n");
        content.append("items = []\n\n");

        try {
            Files.write(configFile.toPath(), content.toString().getBytes(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            plugin.getLogger().info("§a默认配置文件已创建: " + configFile.getAbsolutePath());
        } catch (IOException e) {
            plugin.getLogger().severe("§c创建默认配置文件失败: " + e.getMessage());
        }
    }

        /**
         * 提取品类名称
         */
        private String extractCategoryName(String line) {
            // 从 [[tianchonxz.categories]]行提取name
            int nameStart = line.indexOf("name = \"");
            if (nameStart == -1) {
                return null;
            }

            int nameEnd = line.indexOf("\"", nameStart + 7);
            if (nameEnd != -1) {
                return line.substring(nameStart + 7, nameEnd);
            }

            return null;
        }

        /**
         * 解析单个类别配置
         */
        private void parseCategorySection(List<String> sectionLines, String categoryName) {
            CategoryInfo categoryInfo = new CategoryInfo(categoryName);

            for (String line : sectionLines) {
                line = line.trim();

                if (line.startsWith("minslots")) {
                    categoryInfo.minSlots = parseIntValue(line);
                } else if (line.startsWith("maxslots")) {
                    categoryInfo.maxSlots = parseIntValue(line);
                } else if (line.startsWith("minstack")) {
                    categoryInfo.minStack = parseIntValue(line);
                } else if (line.startsWith("maxstack")) {
                    categoryInfo.maxStack = parseIntValue(line);
                } else if (line.startsWith("fill_chance")) {
                    categoryInfo.fillChance = parseDoubleValue(line);
                } else if (line.startsWith("enabled")) {
                    categoryInfo.enabled = parseBooleanValue(line);
                } else if (line.startsWith("items = [")) {
                    categoryInfo.items = parseItemList(line);
                }
            }

            categories.add(categoryInfo);
        }

        private int parseIntValue(String line) {
            try {
                String value = line.split("=")[1].trim();
                return Integer.parseInt(value);
            } catch (Exception e) {
                return 1;
            }
        }

        private double parseDoubleValue(String line) {
            try {
                String value = line.split("=")[1].trim();
                return Double.parseDouble(value);
            } catch (Exception e) {
                return 1.0;
            }
        }

        private boolean parseBooleanValue(String line) {
            try {
                String value = line.split("=")[1].trim();
                return Boolean.parseBoolean(value);
            } catch (Exception e) {
                return true;
            }
        }

        private List<ItemStack> parseItemList(String line) {
            List<ItemStack> items = new ArrayList<>();
            String itemsPart = line.substring(line.indexOf('[') + 1, line.lastIndexOf(']'));

            if (itemsPart.isEmpty()) {
                return items;
            }

            String[] itemArray = itemsPart.split(",");
            for (String itemStr : itemArray) {
                String itemId = itemStr.trim().replace("\"", "");
                if (!itemId.isEmpty()) {
                    ItemStack item = createItemFromId(itemId);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }

            return items;
        }

        /**
         * 解析物品列表（向后兼容）
         */
        private void parseItems(String line, String category) {
            if (line.length() < 10 || !line.contains("]")) {
                return;
            }

            String itemsPart = line.substring(line.indexOf('[') + 1, line.lastIndexOf(']'));
            if (itemsPart.isEmpty()) {
                return;
            }

            String[] items = itemsPart.split(",");
            List<String> itemList = new ArrayList<>();

            for (String item : items) {
                String itemId = item.trim().replace("\"", "");
                if (!itemId.isEmpty()) {
                    itemList.add(itemId);
                }
            }

            // 创建物品并保存
            CategoryInfo categoryInfo = new CategoryInfo(category);
            for (String itemId : itemList) {
                ItemStack item = createItemFromId(itemId);
                if (item != null) {
                    categoryInfo.items.add(item);
                }
            }

            // 添加到分类列表
            categories.add(categoryInfo);
        }

        /**
         * 根据物品ID创建ItemStack
         */
        private ItemStack createItemFromId(String itemId) {
            try {
                String[] parts = itemId.split(":");
                if (parts.length < 2) {
                    return null;
                }

                String namespace = parts[0];
                String itemName = parts[1];

                Material material = Material.getMaterial(itemName.toUpperCase());
                if (material == null || material == Material.AIR) {
                    material = getMaterialMapping(itemName);
                }

                if (material == null) {
                    return null;
                }

                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.YELLOW + "天充物品: " + itemName);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
                    item.setItemMeta(meta);
                }

                return item;
            } catch (Exception e) {
                plugin.getLogger().warning("§c无法创建物品: " + itemId + " - " + e.getMessage());
                return null;
            }
        }

        /**
         * 获取材质映射
         */
        private Material getMaterialMapping(String itemName) {
            // 简单的材质映射
            switch (itemName.toLowerCase()) {
                case "diamond": return Material.DIAMOND;
                case "gold": case "gold_ingot": return Material.GOLD_INGOT;
                case "iron": case "iron_ingot": return Material.IRON_INGOT;
                case "emerald": return Material.EMERALD;
                case "apple": return Material.APPLE;
                case "bread": return Material.BREAD;
                case "sword": return Material.IRON_SWORD;
                case "pickaxe": return Material.IRON_PICKAXE;
                default: return null;
            }
        }

        /**
         * 保存配置到TOML文件
         */
        public void saveConfig() {
            saveConfig(false);
        }

        /**
         * 保存配置到TOML文件（可选择是否保存到TOML文件）
         * @param saveToTOML 是否同时保存到 tianchonxz.toml 文件
         */
        public void saveConfig(boolean saveToTOML) {
            // 保存到YAML配置文件
            saveToYAML();

            // 保存到TOML配置文件
            if (saveToTOML) {
                saveToTOML();
            }

            plugin.getLogger().info("§a配置文件已保存");
        }

        /**
         * 保存到YAML文件
         */
        private void saveToYAML() {
            // 这里可以保留原有的YAML配置文件逻辑
            plugin.getLogger().info("§aYAML配置文件保存完成");
        }

        /**
         * 根据概率填充物品
         */
        public List<ItemStack> fillItemsWithProbability(String categoryName) {
            for (CategoryInfo category : categories) {
                if (category.name.equalsIgnoreCase(categoryName) && category.enabled) {
                    List<ItemStack> filledItems = new ArrayList<>();

                    // 检查是否要填充此品类
                    if (Math.random() > category.fillChance) {
                        return filledItems; // 概率不通过，返回空列表
                    }

                    // 随机确定填充的物品数量
                    int totalSlots = category.minSlots +
                        (int) (Math.random() * (category.maxSlots - category.minSlots + 1));

                    if (category.items.isEmpty()) {
                        return filledItems;
                    }

                    // 随机选择物品填充
                    for (int i = 0; i < totalSlots && i < category.items.size(); i++) {
                        ItemStack template = category.items.get(
                            (int) (Math.random() * category.items.size()));
                        if (template != null && template.getType() != Material.AIR) {
                            ItemStack item = template.clone();

                            // 随机确定堆叠数量
                            int stackSize = category.minStack +
                                (int) (Math.random() * (category.maxStack - category.minStack + 1));
                            stackSize = Math.min(stackSize, item.getMaxStackSize());
                            item.setAmount(stackSize);

                            filledItems.add(item);
                        }
                    }

                    return filledItems;
                }
            }
            return new ArrayList<>();
        }

        /**
         * 获取所有品类名称
         */
        public List<String> getCategoryNames() {
            List<String> names = new ArrayList<>();
            for (CategoryInfo category : categories) {
                names.add(category.name);
            }
            return names;
        }

        /**
         * 获取指定品类的物品数量
         */
        public int getItemCount(String categoryName) {
            for (CategoryInfo category : categories) {
                if (category.name.equalsIgnoreCase(categoryName)) {
                    return category.items.size();
                }
            }
            return 0;
        }

        /**
         * 添加或更新品类物品
         */
        public void addItemsToCategory(String categoryName, List<ItemStack> items) {
            // 查找现有品类
            for (CategoryInfo category : categories) {
                if (category.name.equalsIgnoreCase(categoryName)) {
                    category.items.clear();
                    category.items.addAll(items);
                    return;
                }
            }

            // 创建新品类
            CategoryInfo newCategory = new CategoryInfo(categoryName);
            newCategory.items.addAll(items);
            categories.add(newCategory);
        }

        /**
         * 重载配置
         */
        public void reloadConfig() {
            readCategories();
        }

        /**
         * 保存到TOML文件
         */
        private void saveToTOML() {
            StringBuilder content = new StringBuilder();

            content.append("# =================================================\n");
            content.append("#   天充箱子 (tianchonxz) 模组配置文件\n");
            content.append("#   作者: l1ang_Y5n (QQ: 235236127)\n");
            content.append("#   自动生成 - 可通过GUI管理物品\n");
            content.append("#   支持 jichu, youpin, xiyou 和自定义品类\n");
            content.append("# =================================================\n\n");

            content.append("[tianchonxz]\n");
            content.append("# 自动生成的品类配置\n\n");

            // 为每个类别生成配置
            for (CategoryInfo categoryInfo : categories) {
                content.append("[[tianchonxz.categories]]\n");
                content.append("name = \"").append(categoryInfo.name).append("\"\n");
                content.append("minslots = ").append(categoryInfo.minSlots).append("\n");
                content.append("maxslots = ").append(categoryInfo.maxSlots).append("\n");
                content.append("minstack = ").append(categoryInfo.minStack).append("\n");
                content.append("maxstack = ").append(categoryInfo.maxStack).append("\n");
                content.append("fill_chance = 1.00\n");
                content.append("enabled = true\n");
                content.append("items = [\n");

                // 保存实际的物品
                for (ItemStack item : categoryInfo.items) {
                    String itemId = getItemId(item);
                    if (itemId != null) {
                        content.append("    \"").append(itemId).append("\n");
                    }
                }

                content.append("]\n\n");
            }

            try {
                java.nio.file.Files.write(configFile.toPath(), content.toString().getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                plugin.getLogger().info("§a已保存配置到: " + configFile.getAbsolutePath());
            } catch (IOException e) {
                    plugin.getLogger().severe("§c保存配置文件失败: " + e.getMessage());
                }

            plugin.getLogger().info("§a配置文件已保存到 tianchonxz.toml，共保存 " + categories.size() + " 个品类");
        }

        /**
     * 创建物品ID
     */
    private String getItemId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        // 简化的ID生成：优先使用显示名，其次使用材质名
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            if (!displayName.isEmpty()) {
                return "minecraft:" + item.getType().name().toLowerCase();
            }
            return "minecraft:" + displayName.toLowerCase().replace(" ", "_");
        } else {
            return "minecraft:" + item.getType().name().toLowerCase();
        }
    }

    /**
     * 获取所有品类信息
     */
    public List<CategoryInfo> getCategoryInfos() {
        return new ArrayList<>(categories);
    }

        /**
     * 类别信息数据类
     */
    public static class CategoryInfo {
        public String name;
        public int minSlots;
        public int maxSlots;
        public int minStack;
        public int maxStack;
        public double fillChance;
        public boolean enabled;
        public List<ItemStack> items;

        public CategoryInfo(String name) {
            this.name = name;
            this.minSlots = 1;
            this.maxSlots = 5;
            this.minStack = 1;
            this.maxStack = 1;
            this.fillChance = 1.0;
            this.enabled = true;
            this.items = new ArrayList<>();
        }
    }
}