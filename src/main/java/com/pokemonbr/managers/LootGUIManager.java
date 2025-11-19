package com.pokemonbr.managers;

import com.pokemonbr.Main;
import com.pokemonbr.utils.TOMLManager;
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
 * 支持完整NBT保存到配置文件
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class LootGUIManager {

    private final Main plugin;
    private FileConfiguration config;
    private File configFile;
    private TOMLManager tomlManager;

    // 当前打开GUI的玩家 -> 分类名称
    private final Map<UUID, String> openGUIs;

    // 简化的自定义品类管理器
    private final SimpleCustomCategoryManager simpleCategoryManager;

    public LootGUIManager(Main plugin) {
        this.plugin = plugin;
        this.openGUIs = new HashMap<>();
        this.simpleCategoryManager = new SimpleCustomCategoryManager(plugin);

        // 初始化TOML管理器
        this.tomlManager = new TOMLManager(plugin, new File(plugin.getDataFolder(), "tianchonxz.toml"));

        loadConfig();
        plugin.getLogger().info("§a物品管理GUI系统已加载");
    }

    /**
     * 加载配置文件
     */
    private void loadConfig() {
        // 更新路径到 loot 子目录
        File lootDir = new File(plugin.getDataFolder(), "loot");
        configFile = new File(lootDir, "loot-gui.yml");

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

        // 释放 loot-system.yml（奖励组配置文件）
        File lootSystemFile = new File(lootDir, "loot-system.yml");
        if (!lootSystemFile.exists()) {
            try {
                plugin.saveResource("loot/loot-system.yml", false);
                plugin.getLogger().info("§a已从JAR包释放loot-system.yml到loot目录");
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("§eJAR包中未找到loot/loot-system.yml");
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
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

        // 添加categories节点以避免警告
        config.createSection("categories.jichu.items");
        config.createSection("categories.youpin.items");
        config.createSection("categories.jipin.items");
        config.createSection("categories.pokemon.items");

        config.set("auto-detection.config-file", "loot-system.yml");
        config.set("auto-detection.pattern", "^[a-zA-Z0-9_]+$");

        config.set("permissions.open-gui", "pbr.lootgui.open");
        config.set("permissions.edit-groups", "pbr.lootgui.edit");
        config.set("permissions.admin", "pbr.lootgui.admin");

        plugin.getLogger().info("§a已创建默认LootGUI配置");
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
        plugin.getLogger().info("§a物品管理GUI配置已重载");
    }

    /**
     * 打开物品管理GUI
     * @param player 玩家
     * @param category 分类名称 (如 "youpin", "jipin", "putong")
     */
    public void openGUI(Player player, String category) {
        // 检查分类是否存在
        if (!config.contains("categories." + category)) {
            player.sendMessage(ChatColor.RED + "分类 " + category + " 不存在！");
            player.sendMessage(ChatColor.GRAY + "可用分类: youpin, jipin, putong");
            return;
        }

        // 检查权限
        String permission = config.getString("categories." + category + ".permission", "");
        if (!permission.isEmpty() && !player.hasPermission(permission)) {
            String message = config.getString("gui.messages.no-permission", "&c你没有权限打开此物品管理界面");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            return;
        }

        // 创建GUI
        String displayName = config.getString("categories." + category + ".display-name", category);
        String titlePrefix = config.getString("gui.title-prefix", "&6&l物品管理 &8» &e");
        String title = ChatColor.translateAlternateColorCodes('&', titlePrefix + displayName);

        int size = config.getInt("gui.size", 54);
        Inventory gui = Bukkit.createInventory(null, size, title);

        // 加载已保存的物品
        List<String> itemsNBT = config.getStringList("categories." + category + ".items");
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
        String message = config.getString("gui.messages.opened", "&a已打开 &e{category} &a物品管理界面");
        message = message.replace("{category}", displayName);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    /**
     * 保存GUI中的物品到配置（同时保存到YAML和TOML）
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

        // 保存到YAML配置（向后兼容）
        if (!items.isEmpty()) {
            List<String> itemData = new ArrayList<>();
            for (ItemStack item : items) {
                itemData.add(itemToBase64(item));
            }
            config.set("categories." + category, itemData);
            saveConfig();
        }

        // 保存到TOML管理器（支持自动识别）
        if (tomlManager != null && !items.isEmpty()) {
            tomlManager.addItemsToCategory(category, items);
            tomlManager.saveConfig(true); // 保存到TOML文件
        }

        // 同时保存到简化自定义品类管理器（兼容性）
        if (simpleCategoryManager != null) {
            for (ItemStack item : items) {
                simpleCategoryManager.addItemToCategory(category, item);
            }
            simpleCategoryManager.saveConfig();
        }

        // 发送消息
        player.sendMessage(ChatColor.GREEN + "✓ 成功保存 " + count + " 个物品到品类: " + category);
        player.sendMessage(ChatColor.YELLOW + "配置已保存到 YAML 和 TOML 文件");
        player.sendMessage(ChatColor.GRAY + "TOML配置支持概率填充：" + getFillChanceInfo(category));

        // 移除记录
        openGUIs.remove(player.getUniqueId());

        if (config.getBoolean("advanced.debug", false)) {
            plugin.getLogger().info("§e[Debug] 玩家 " + player.getName() + " 保存了 " + count + " 个物品到 " + category);
        }
    }

    /**
     * 清空指定分类的所有物品
     * @param player 执行者
     * @param category 分类名称
     */
    public void clearCategory(Player player, String category) {
        if (!config.contains("categories." + category)) {
            player.sendMessage(ChatColor.RED + "分类 " + category + " 不存在！");
            return;
        }

        config.set("categories." + category + ".items", new ArrayList<>());
        saveConfig();

        String displayName = config.getString("categories." + category + ".display-name", category);
        String message = config.getString("gui.messages.cleared", "&c已清空 &e{category} &c的所有物品");
        message = message.replace("{category}", displayName);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    /**
     * 获取指定分类的物品列表
     * @param category 分类名称
     * @return 物品列表
     */
    public List<ItemStack> getItems(String category) {
        List<ItemStack> items = new ArrayList<>();

        // 优先从TOML配置中获取
        if (simpleCategoryManager != null && simpleCategoryManager.hasCategory(category)) {
            items = simpleCategoryManager.getItemsFromCategory(category);
        }

        // 如果TOML中没有，回退到YAML配置
        if (items.isEmpty() && config.contains("categories." + category)) {
            List<String> itemsNBT = config.getStringList("categories." + category + ".items");
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
     * 获取所有分类名称（自动识别TOML和YAML配置）
     * @return 分类列表
     */
    public Set<String> getCategories() {
        Set<String> allCategories = new HashSet<>();

        // 从YAML配置获取分类
        ConfigurationSection section = config.getConfigurationSection("categories");
        if (section != null) {
            allCategories.addAll(section.getKeys(false));
        }

        // 从TOML配置获取分类
        if (tomlManager != null) {
            tomlManager.readCategories(); // 重载TOML配置
            allCategories.addAll(tomlManager.getCategoryNames());
        }

        // 如果没有配置任何分类，使用默认分类
        if (allCategories.isEmpty()) {
            allCategories.add("jichu");
            allCategories.add("youpin");
            allCategories.add("xiyou");
        }

        return allCategories;
    }

    
    /**
     * 获取品类填充概率信息
     * @param category 品类名称
     * @return 概率信息字符串
     */
    private String getFillChanceInfo(String category) {
        if (tomlManager != null) {
            // 从TOML管理器获取概率信息
            for (TOMLManager.CategoryInfo catInfo : tomlManager.getCategoryInfos()) {
                if (catInfo.name.equalsIgnoreCase(category)) {
                    return String.format("填充概率 %.0f%%, 格数 %d-%d, 堆叠 %d-%d",
                        catInfo.fillChance * 100, catInfo.minSlots, catInfo.maxSlots,
                        catInfo.minStack, catInfo.maxStack);
                }
            }
        }
        return "默认配置";
    }

    /**
     * 获取TOML管理器实例
     * @return TOMLManager
     */
    public TOMLManager getTomlManager() {
        return tomlManager;
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
