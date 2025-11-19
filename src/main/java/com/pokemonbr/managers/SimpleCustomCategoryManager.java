package com.pokemonbr.managers;

import com.pokemonbr.Main;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 简化的自定义品类管理器
 * 支持GUI物品直接保存到TOML文件
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class SimpleCustomCategoryManager {

    private final Main plugin;
    private final File configFile;
    private final Map<String, List<ItemStack>> categoryItems; // 品类名 -> 物品列表

    // 默认品类配置
    private static final String[] DEFAULT_CATEGORIES = {"jichu", "youpin", "xiyou"};
    private static final Map<String, int[]> DEFAULT_CONFIG;

    static {
        DEFAULT_CONFIG = new HashMap<>();
        DEFAULT_CONFIG.put("jichu", new int[]{3, 6, 2, 8});    // 最小格数,最大格数,最小堆叠,最大堆叠
        DEFAULT_CONFIG.put("youpin", new int[]{2, 4, 1, 3});
        DEFAULT_CONFIG.put("xiyou", new int[]{1, 2, 1, 1});
    }

    public SimpleCustomCategoryManager(Main plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "tianchonxz.toml");
        this.categoryItems = new HashMap<>();

        initializeDefaultCategories();
        loadConfig();
    }

    /**
     * 初始化默认品类
     */
    private void initializeDefaultCategories() {
        for (String category : DEFAULT_CATEGORIES) {
            categoryItems.put(category, new ArrayList<>());
        }
        plugin.getLogger().info("§a已初始化默认品类: " + Arrays.toString(DEFAULT_CATEGORIES));
    }

    /**
     * 加载TOML配置文件
     */
    private void loadConfig() {
        if (!configFile.exists()) {
            createDefaultTOML();
            return;
        }

        try {
            String content = new String(java.nio.file.Files.readAllBytes(configFile.toPath()));
            parseTOMLContent(content);
        } catch (IOException e) {
            plugin.getLogger().severe("§c读取配置文件失败: " + e.getMessage());
            createDefaultTOML();
        }
    }

    /**
     * 解析TOML内容
     */
    private void parseTOMLContent(String content) {
        // 简化的解析逻辑，假设文件格式固定
        String[] lines = content.split("\n");
        String currentCategory = null;

        for (String line : lines) {
            line = line.trim();

            if (line.startsWith("[[tianchonxz.categories]]")) {
                // 开始新的品类
                currentCategory = extractCategoryName(line);
                if (currentCategory != null) {
                    categoryItems.putIfAbsent(currentCategory, new ArrayList<>());
                }
            } else if (currentCategory != null && line.startsWith("items = [")) {
                // 解析物品列表
                parseItemsList(line, currentCategory);
            }
        }
    }

    /**
     * 从TOML行提取品类名称
     */
    private String extractCategoryName(String line) {
        // 格式: [[tianchonxz.categories]]
        // 提取name = "xxx" 部分
        int nameStart = line.indexOf("name = \"");
        if (nameStart != -1) {
            int nameEnd = line.indexOf("\"", nameStart + 7);
            if (nameEnd != -1) {
                return line.substring(nameStart + 7, nameEnd);
            }
        }
        return null;
    }

    /**
     * 解析物品列表
     */
    private void parseItemsList(String line, String category) {
        // 格式: items = ["item1", "item2", "item3"]
        String itemsPart = line.substring(line.indexOf('[') + 1, line.lastIndexOf(']'));
        if (itemsPart.isEmpty()) {
            return;
        }

        String[] items = itemsPart.split(",");
        for (String item : items) {
            String itemId = item.trim().replace("\"", "");
            if (!itemId.isEmpty()) {
                ItemStack itemStack = createItemFromId(itemId);
                if (itemStack != null) {
                    categoryItems.get(category).add(itemStack);
                }
            }
        }
    }

    /**
     * 根据物品ID创建ItemStack
     */
    private ItemStack createItemFromId(String itemId) {
        try {
            // 简单解析，格式: minecraft:itemname 或 pixelmon:itemname
            String[] parts = itemId.split(":");
            if (parts.length < 2) {
                return null;
            }

            String namespace = parts[0];
            String itemName = parts[1];

            Material material = Material.getMaterial(itemName.toUpperCase());
            if (material == null || material == Material.AIR) {
                // 尝试一些映射
                material = getMaterialMapping(itemName);
            }

            if (material == null) {
                return null;
            }

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + "天充物品: " + itemName);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                item.setItemMeta(meta);
            }

            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("§c无法创建物品: " + itemId + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 材料映射
     */
    private Material getMaterialMapping(String itemName) {
        // Java 8 兼容的静态映射
        switch (itemName.toLowerCase()) {
            case "stone": return Material.STONE;
            case "dirt": return Material.DIRT;
            case "cobblestone": return Material.COBBLESTONE;
            case "oak_planks": return Material.OAK_PLANKS;
            case "iron_ingot": return Material.IRON_INGOT;
            case "gold_ingot": return Material.GOLD_INGOT;
            case "diamond": return Material.DIAMOND;
            case "emerald": return Material.EMERALD;
            case "cooked_beef": return Material.COOKED_BEEF;
            case "bread": return Material.BREAD;
            case "golden_apple": return Material.GOLDEN_APPLE;
            case "iron_sword": return Material.IRON_SWORD;
            case "diamond_sword": return Material.DIAMOND_SWORD;
            case "bow": return Material.BOW;
            case "arrow": return Material.ARROW;
            case "iron_pickaxe": return Material.IRON_PICKAXE;
            case "diamond_pickaxe": return Material.DIAMOND_PICKAXE;
            case "potion": return Material.POTION;
            case "ender_pearl": return Material.ENDER_PEARL;
            default: return null;
        }
    }

    /**
     * 创建默认TOML文件
     */
    private void createDefaultTOML() {
        StringBuilder content = new StringBuilder();

        content.append("# ==================================================\n");
        content.append("#   天充箱子 (tianchonxz) 模组配置文件\n");
        content.append("#   作者: l1ang_Y5n (QQ: 235236127)\n");
        content.append("#   自动生成 - 可通过GUI管理物品\n");
        content.append("# ==================================================\n\n");

        content.append("[tianchonxz]\n");
        content.append("# 自动生成的品类配置\n\n");

        // 为每个默认品类创建配置
        for (String category : DEFAULT_CATEGORIES) {
            int[] config = DEFAULT_CONFIG.get(category);

            content.append("[[tianchonxz.categories]]\n");
            content.append("name = \"").append(category).append("\"\n");
            content.append("minslots = ").append(config[0]).append("\n");
            content.append("maxslots = ").append(config[1]).append("\n");
            content.append("minstack = ").append(config[2]).append("\n");
            content.append("maxstack = ").append(config[3]).append("\n");
            content.append("fill_chance = 1.00\n");
            content.append("enabled = true\n");
            content.append("items = [\n");

            // 添加一些示例物品
            List<String> sampleItems = getSampleItems(category);
            for (int i = 0; i < sampleItems.size(); i++) {
                content.append("    \"").append(sampleItems.get(i)).append("\"");
                if (i < sampleItems.size() - 1) {
                    content.append(",");
                }
                content.append("\n");
            }

            content.append("]\n\n");
        }

        try {
            java.nio.file.Files.write(configFile.toPath(), content.toString().getBytes());
            plugin.getLogger().info("§a已创建默认配置文件: " + configFile.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("§c创建默认配置文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取示例物品列表
     */
    private List<String> getSampleItems(String category) {
        switch (category) {
            case "jichu":
                return Arrays.asList("minecraft:stone", "minecraft:oak_planks", "minecraft:iron_ingot");
            case "youpin":
                return Arrays.asList("minecraft:iron_sword", "minecraft:diamond", "minecraft:golden_apple");
            case "xiyou":
                return Arrays.asList("minecraft:diamond_sword", "minecraft:ender_pearl", "minecraft:totem_of_undying");
            default:
                return Arrays.asList("minecraft:stone", "minecraft:dirt");
        }
    }

    /**
     * 保存TOML文件
     */
    public void saveConfig() {
        StringBuilder content = new StringBuilder();

        content.append("# ==================================================\n");
        content.append("#   天充箱子 (tianchonxz) 模组配置文件\n");
        content.append("#   作者: l1ang_Y5n (QQ: 235236127)\n");
        content.append("#   自动生成 - 可通过GUI管理物品\n");
        content.append("# ==================================================\n\n");

        content.append("[tianchonxz]\n");
        content.append("# 自动生成的品类配置\n\n");

        for (Map.Entry<String, List<ItemStack>> entry : categoryItems.entrySet()) {
            String category = entry.getKey();
            List<ItemStack> items = entry.getValue();

            // 获取配置信息（使用默认值，可后续扩展）
            int[] config = DEFAULT_CONFIG.getOrDefault(category, DEFAULT_CONFIG.get("jichu"));

            content.append("[[tianchonxz.categories]]\n");
            content.append("name = \"").append(category).append("\"\n");
            content.append("minslots = ").append(config[0]).append("\n");
            content.append("maxslots = ").append(config[1]).append("\n");
            content.append("minstack = ").append(config[2]).append("\n");
            content.append("maxstack = ").append(config[3]).append("\n");
            content.append("fill_chance = 1.00\n");
            content.append("enabled = true\n");
            content.append("items = [\n");

            // 保存实际物品
            for (int i = 0; i < items.size(); i++) {
                ItemStack item = items.get(i);
                String itemId = getItemId(item);
                if (itemId != null) {
                    content.append("    \"").append(itemId).append("\"");
                    if (i < items.size() - 1) {
                        content.append(",");
                    }
                    content.append("\n");
                }
            }

            content.append("]\n\n");
        }

        try {
            java.nio.file.Files.write(configFile.toPath(), content.toString().getBytes());
            plugin.getLogger().info("§a已保存配置文件: " + configFile.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("§c保存配置文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取物品的ID
     */
    private String getItemId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        String displayName = null;
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        }

        // 简化的ID生成：优先使用显示名，其次使用材质名
        if (displayName != null && !displayName.isEmpty()) {
            return "minecraft:" + displayName.toLowerCase().replace(" ", "_");
        } else {
            return "minecraft:" + item.getType().name().toLowerCase();
        }
    }

    /**
     * 添加物品到指定品类
     */
    public boolean addItemToCategory(String category, ItemStack item) {
        if (!categoryItems.containsKey(category)) {
            // 如果品类不存在，创建新品类
            categoryItems.put(category, new ArrayList<>());
        }

        categoryItems.get(category).add(item);
        return true;
    }

    /**
     * 从品类获取物品
     */
    public List<ItemStack> getItemsFromCategory(String category) {
        return new ArrayList<>(categoryItems.getOrDefault(category, new ArrayList<>()));
    }

    /**
     * 获取所有品类名称
     */
    public Set<String> getCategoryNames() {
        return new HashSet<>(categoryItems.keySet());
    }

    /**
     * 获取品类数量
     */
    public int getItemCount(String category) {
        return categoryItems.getOrDefault(category, new ArrayList<>()).size();
    }

    /**
     * 获取随机品类
     */
    public String getRandomCategory() {
        List<String> categories = new ArrayList<>(categoryItems.keySet());
        if (categories.isEmpty()) {
            return DEFAULT_CATEGORIES[0]; // 返回第一个默认品类
        }
        return categories.get(new Random().nextInt(categories.size()));
    }

    /**
     * 重载配置
     */
    public void reloadConfig() {
        loadConfig();
        plugin.getLogger().info("§a配置文件已重载");
    }

    /**
     * 检查品类是否存在
     */
    public boolean hasCategory(String category) {
        return categoryItems.containsKey(category);
    }
}