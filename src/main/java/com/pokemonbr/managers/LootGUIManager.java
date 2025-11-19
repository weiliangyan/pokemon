package com.pokemonbr.managers;

import com.pokemonbr.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 物品管理GUI系统
 * 类似CMI的物品配置功能
 * 支持完整NBT保存到loot-system.yml配置文件
 * 移除了TOML依赖，统一使用YAML格式
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class LootGUIManager {

    private final Main plugin;
    private FileConfiguration config;
    private File configFile;
    private File lootSystemConfigFile;
    private FileConfiguration lootSystemConfig;

    // 当前打开GUI的玩家 -> 分类名称
    private final Map<UUID, String> openGUIs;

    public LootGUIManager(Main plugin) {
        this.plugin = plugin;
        this.openGUIs = new HashMap<>();

        loadConfig();
        plugin.getLogger().info("§a物品管理GUI系统已加载 (基于loot-system.yml)");
    }

    /**
     * 加载配置文件
     */
    private void loadConfig() {
        // 更新路径到 loot 子目录
        File lootDir = new File(plugin.getDataFolder(), "loot");
        configFile = new File(lootDir, "loot-gui.yml");
        lootSystemConfigFile = new File(lootDir, "loot-system.yml");

        // 确保loot目录存在
        if (!lootDir.exists()) {
            lootDir.mkdirs();
        }

        // 释放 loot-gui.yml
        if (!configFile.exists()) {
            try {
                // 从JAR包保存默认文件（使用正确的子目录路径）
                plugin.saveResource("loot/loot-gui.yml", false);
                plugin.getLogger().info("§a已从JAR包释放loot-gui.yml到loot目录");
            } catch (IllegalArgumentException e) {
                // 如果JAR包中没有该文件，创建空文件
                plugin.getLogger().info("§eJAR包中未找到loot/loot-gui.yml，创建空配置文件");
                createEmptyLootGUIFile();
            }
        }

        // 释放或创建 loot-system.yml（奖励组配置文件）
        if (!lootSystemConfigFile.exists()) {
            try {
                plugin.saveResource("loot/loot-system.yml", false);
                plugin.getLogger().info("§a已从JAR包释放loot-system.yml到loot目录");
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("§eJAR包中未找到loot/loot-system.yml，创建默认配置");
                createEmptyLootSystemFile();
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        lootSystemConfig = YamlConfiguration.loadConfiguration(lootSystemConfigFile);
        plugin.getLogger().info("§a已加载LootGUI配置文件");
    }

    /**
     * 创建空的loot-gui.yml文件
     */
    private void createEmptyLootGUIFile() {
        try {
            // 确保loot目录存在
            File lootDir = new File(plugin.getDataFolder(), "loot");
            lootDir.mkdirs();

            // 创建新文件
            if (configFile.createNewFile()) {
                plugin.getLogger().info("§a已创建空loot-gui.yml配置文件");

                // 写入默认内容
                FileConfiguration config = new YamlConfiguration();
                createDefaultLootGUIConfig(config);
                config.save(configFile);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("§c创建loot-gui.yml配置文件失败");
            e.printStackTrace();
        }
    }

    /**
     * 创建默认的loot-gui配置
     */
    private void createDefaultLootGUIConfig(FileConfiguration config) {
        config.set("gui.title-prefix", "&6&l奖励组管理 &8» &e");
        config.set("gui.main-title", "&6&l奖励组系统");
        config.set("gui.size", 54);
        config.set("gui.clear-on-open", false);

        config.set("gui.messages.opened", "&a已打开 {category} 奖励组管理界面");
        config.set("gui.messages.saved", "&a成功保存 {count} 个物品到 {category}");
        config.set("gui.messages.cleared", "&c已清空 {category} 的所有物品");

        config.set("permissions.open-gui", "pbr.lootgui.open");
        config.set("permissions.edit-groups", "pbr.lootgui.edit");
        config.set("permissions.admin", "pbr.lootgui.admin");

        plugin.getLogger().info("§a已创建默认LootGUI配置");
    }

    /**
     * 创建空的loot-system.yml文件
     */
    private void createEmptyLootSystemFile() {
        try {
            if (lootSystemConfigFile.createNewFile()) {
                plugin.getLogger().info("§a已创建空loot-system.yml配置文件");

                // 写入默认内容
                FileConfiguration config = new YamlConfiguration();
                createDefaultLootSystemConfig(config);
                config.save(lootSystemConfigFile);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("§c创建loot-system.yml配置文件失败");
            e.printStackTrace();
        }
    }

    /**
     * 创建默认的loot-system.yml配置
     */
    private void createDefaultLootSystemConfig(FileConfiguration config) {
        config.set("普通.MinSlots", 1);
        config.set("普通.MaxSlots", 3);
        config.set("普通.MinStack", 1);
        config.set("普通.MaxStack", 1);
        config.set("普通.Weight", 10);
        config.set("普通.enabled", true);

        config.set("优品.MinSlots", 1);
        config.set("优品.MaxSlots", 2);
        config.set("优品.MinStack", 1);
        config.set("优品.MaxStack", 1);
        config.set("优品.Weight", 5);
        config.set("优品.enabled", true);

        config.set("极品.MinSlots", 1);
        config.set("极品.MaxSlots", 1);
        config.set("极品.MinStack", 1);
        config.set("极品.MaxStack", 1);
        config.set("极品.Weight", 2);
        config.set("极品.enabled", true);

        plugin.getLogger().info("§a已创建默认loot-system配置");
    }

    /**
     * 保存配置文件
     */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("§c保存 loot-gui.yml 失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 重载配置
     */
    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        lootSystemConfig = YamlConfiguration.loadConfiguration(lootSystemConfigFile);
        plugin.getLogger().info("§a物品管理GUI配置已重载");
    }

    /**
     * 打开物品管理GUI
     * @param player 玩家
     * @param category 分类名称 (如 "优品", "极品", "普通")
     */
    public void openGUI(Player player, String category) {
        // 检查分类是否存在（从loot-system.yml读取）
        if (!lootSystemConfig.contains(category)) {
            player.sendMessage(ChatColor.RED + "分类 " + category + " 不存在！");
            player.sendMessage(ChatColor.GRAY + "可用分类: " + String.join(", ", getCategories()));
            return;
        }

        // 检查权限
        String permission = config.getString("permissions.edit-groups", "pbr.lootgui.edit");
        if (!permission.isEmpty() && !player.hasPermission(permission)) {
            String message = "&c你没有权限打开此物品管理界面";
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            return;
        }

        // 创建GUI
        String titlePrefix = config.getString("gui.title-prefix", "&6&l奖励组管理 &8» &e");
        String title = ChatColor.translateAlternateColorCodes('&', titlePrefix + category);

        int size = config.getInt("gui.size", 54);
        Inventory gui = Bukkit.createInventory(null, size, title);

        // 加载已保存的物品（从loot-system.yml的专用GUI节点）
        List<String> itemsNBT = lootSystemConfig.getStringList(category + ".gui-items");
        for (int i = 0; i < itemsNBT.size() && i < size; i++) {
            ItemStack item = itemFromBase64(itemsNBT.get(i));
            if (item != null) {
                gui.setItem(i, item);
            }
        }

        // 打开GUI
        player.openInventory(gui);
        openGUIs.put(player.getUniqueId(), category);

        // 发送消息
        String message = config.getString("gui.messages.opened", "&a已打开 {category} 奖励组管理界面");
        message = message.replace("{category}", category);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    /**
     * 保存GUI中的物品到loot-system.yml配置
     * @param player 玩家
     * @param inventory GUI物品栏
     */
    public void saveGUI(Player player, Inventory inventory) {
        String category = openGUIs.get(player.getUniqueId());
        if (category == null) {
            return;
        }

        // 收集所有非空物品
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }

        int count = items.size();

        // 保存到loot-system.yml配置（专用GUI节点）
        if (!items.isEmpty()) {
            List<String> itemData = new ArrayList<>();
            for (ItemStack item : items) {
                String base64 = itemToBase64(item);
                if (base64 != null) {
                    itemData.add(base64);
                }
            }

            // 保存物品数据到专用GUI节点，避免与手写配置冲突
            lootSystemConfig.set(category + ".gui-items", itemData);
            saveLootSystemConfig();

            plugin.getLogger().info("§a[GUI保存] 已保存 " + count + " 个物品到 " + category + ".gui-items");
        }

        // 发送消息
        String message = config.getString("gui.messages.saved", "&a成功保存 {count} 个物品到 {category}");
        message = message.replace("{count}", String.valueOf(count)).replace("{category}", category);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));

        player.sendMessage(ChatColor.YELLOW + "配置已保存到 loot-system.yml 文件");
        player.sendMessage(ChatColor.GRAY + "该分类的配置信息：" + getFillChanceInfo(category));

        // 移除记录
        openGUIs.remove(player.getUniqueId());

        plugin.getLogger().info("§a[GUI保存] 玩家 " + player.getName() + " 保存了 " + count + " 个物品到 " + category);
    }

    /**
     * 保存loot-system.yml配置文件
     */
    private void saveLootSystemConfig() {
        try {
            lootSystemConfig.save(lootSystemConfigFile);
        } catch (IOException e) {
            plugin.getLogger().severe("§c保存 loot-system.yml 失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 清空指定分类的所有物品
     * @param player 执行者
     * @param category 分类名称
     */
    public void clearCategory(Player player, String category) {
        if (!lootSystemConfig.contains(category)) {
            player.sendMessage(ChatColor.RED + "分类 " + category + " 不存在！");
            return;
        }

        lootSystemConfig.set(category + ".gui-items", new ArrayList<>());
        saveLootSystemConfig();

        String message = config.getString("gui.messages.cleared", "&c已清空 {category} 的所有物品");
        message = message.replace("{category}", category);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    /**
     * 获取指定分类的物品列表
     * @param category 分类名称
     * @return 物品列表
     */
    public List<ItemStack> getItems(String category) {
        List<ItemStack> items = new ArrayList<>();

        // 从loot-system.yml中获取GUI保存的物品
        if (lootSystemConfig.contains(category + ".gui-items")) {
            List<String> itemsNBT = lootSystemConfig.getStringList(category + ".gui-items");
            for (String nbt : itemsNBT) {
                ItemStack item = itemFromBase64(nbt);
                if (item != null) {
                    items.add(item);
                }
            }
        }

        return items;
    }

    /**
     * 获取所有分类名称（从loot-system.yml读取）
     * @return 分类列表
     */
    public Set<String> getCategories() {
        Set<String> allCategories = new HashSet<>();

        // 从loot-system.yml获取分类
        for (String key : lootSystemConfig.getKeys(false)) {
            // 排除配置项，只获取分类名称
            if (!key.equals("settings") && !key.equals("config") && lootSystemConfig.contains(key + ".items")) {
                allCategories.add(key);
            }
        }

        // 如果没有配置任何分类，使用默认分类
        if (allCategories.isEmpty()) {
            allCategories.add("普通");
            allCategories.add("优品");
            allCategories.add("极品");
        }

        return allCategories;
    }

    
    /**
     * 获取品类填充概率信息
     * @param category 品类名称
     * @return 概率信息字符串
     */
    private String getFillChanceInfo(String category) {
        // 从loot-system.yml获取配置信息
        if (lootSystemConfig.contains(category)) {
            int minSlots = lootSystemConfig.getInt(category + ".MinSlots", 1);
            int maxSlots = lootSystemConfig.getInt(category + ".MaxSlots", 3);
            int minStack = lootSystemConfig.getInt(category + ".MinStack", 1);
            int maxStack = lootSystemConfig.getInt(category + ".MaxStack", 1);
            int weight = lootSystemConfig.getInt(category + ".Weight", 1);

            return String.format("权重 %d, 格数 %d-%d, 堆叠 %d-%d",
                weight, minSlots, maxSlots, minStack, maxStack);
        }
        return "默认配置";
    }

    /**
     * 获取loot-system.yml配置文件实例
     * @return FileConfiguration
     */
    public FileConfiguration getLootSystemConfig() {
        return lootSystemConfig;
    }

    /**
     * 检查玩家是否正在编辑GUI
     * @param player 玩家
     * @return 是否在编辑
     */
    public boolean isEditingGUI(Player player) {
        return openGUIs.containsKey(player.getUniqueId());
    }

    /**
     * 获取玩家正在编辑的分类
     * @param player 玩家
     * @return 分类名称 或 null
     */
    public String getEditingCategory(Player player) {
        return openGUIs.get(player.getUniqueId());
    }

    /**
     * 将物品序列化为Base64字符串（包含完整NBT）
     * @param item 物品
     * @return Base64字符串
     */
    private String itemToBase64(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            dataOutput.writeObject(item);
            dataOutput.close();

            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("§c无法序列化物品: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从Base64字符串反序列化物品（包含完整NBT）
     * @param data Base64字符串
     * @return 物品 或 null
     */
    private ItemStack itemFromBase64(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();

            return item;
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().warning("§c无法反序列化物品: " + e.getMessage());
            return null;
        }
    }

    /**
     * 关闭所有玩家的GUI并保存
     */
    public void closeAll() {
        for (UUID uuid : new HashSet<>(openGUIs.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.closeInventory();
            }
        }
        openGUIs.clear();
    }
}
