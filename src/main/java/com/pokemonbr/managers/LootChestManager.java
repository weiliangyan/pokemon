package com.pokemonbr.managers;

import com.pokemonbr.Main;
import com.pokemonbr.models.LootCategory;
import com.pokemonbr.models.LootItem;
import com.pokemonbr.models.CustomCategory;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * 战利品箱管理器
 * 负责填充游戏世界中的箱子
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class LootChestManager {

    private final Main plugin;
    private final Map<String, LootCategory> categories;
    private final CustomCategoryManager customCategoryManager;
    private final Random random;

    // 全局设置
    private boolean enabled;
        private boolean clearBeforeFill;

    // GUI管理的物品缓存
    private final Map<String, List<ItemStack>> guiManagedItems;

    // 多世界配置管理器
    private WorldConfigManager worldConfigManager;

    public LootChestManager(Main plugin) {
        this.plugin = plugin;
        this.categories = new HashMap<>();
        this.random = new Random();
        this.customCategoryManager = plugin.getCustomCategoryManager();
        this.guiManagedItems = new HashMap<>();
        this.worldConfigManager = new WorldConfigManager(plugin);

        loadLootConfig();
    }

    /**
     * 加载战利品配置
     */
    private void loadLootConfig() {
        // 使用新的奖励组系统，不再依赖YAML配置
        // 配置现在通过customCategoryManager管理
        FileConfiguration config = plugin.getConfigManager().getMainConfig();

        // 加载全局设置
        enabled = config.getBoolean("global.enabled", true);
        clearBeforeFill = config.getBoolean("global.clear-before-fill", true);

        // 加载GUI管理的物品
        loadGUIManagedItems();

        // 加载物品品类（loot.yml 的传统配置）
        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
        if (categoriesSection == null) {
            plugin.getLogger().warning("§c战利品配置中未找到 categories 节点！");
            return;
        }

        for (String categoryName : categoriesSection.getKeys(false)) {
            int weight = config.getInt("categories." + categoryName + ".weight", 10);
            LootCategory category = new LootCategory(categoryName, weight);

            // 加载该品类的物品
            List<Map<?, ?>> itemsList = config.getMapList("categories." + categoryName + ".items");

            for (Map<?, ?> itemMap : itemsList) {
                try {
                    // 解析物品数据
                    String materialName = (String) itemMap.get("material");

                    Object nameObj = itemMap.get("name");
                    String name = nameObj != null ? (String) nameObj : "";

                    @SuppressWarnings("unchecked")
                    Map<String, Integer> amountMap = (Map<String, Integer>) itemMap.get("amount");
                    int minAmount = amountMap.getOrDefault("min", 1);
                    int maxAmount = amountMap.getOrDefault("max", 1);

                    Object chanceObj = itemMap.get("chance");
                    int chance = chanceObj != null ? ((Number) chanceObj).intValue() : 100;

                    // 解析附魔
                    List<LootItem.EnchantmentData> enchantments = new ArrayList<>();
                    @SuppressWarnings("unchecked")
                    List<String> enchantList = (List<String>) itemMap.get("enchantments");
                    if (enchantList != null) {
                        for (String enchantStr : enchantList) {
                            String[] parts = enchantStr.split(":");
                            if (parts.length == 2) {
                                try {
                                    Enchantment enchant = Enchantment.getByName(parts[0]);
                                    int level = Integer.parseInt(parts[1]);
                                    if (enchant != null) {
                                        enchantments.add(new LootItem.EnchantmentData(enchant, level));
                                    }
                                } catch (Exception e) {
                                    plugin.getLogger().warning("§c无效的附魔配置: " + enchantStr);
                                }
                            }
                        }
                    }

                    // 解析Lore
                    @SuppressWarnings("unchecked")
                    List<String> lore = (List<String>) itemMap.get("lore");

                    // 创建战利品物品
                    LootItem lootItem;

                    // 检查是否为Pixelmon物品
                    if (materialName.startsWith("PIXELMON:")) {
                        // Pixelmon物品
                        String pixelmonItemId = materialName.substring(9); // 移除 "PIXELMON:" 前缀
                        lootItem = new LootItem(pixelmonItemId, name, minAmount, maxAmount, chance, enchantments, lore);
                    } else {
                        // 普通Minecraft物品
                        Material material = Material.valueOf(materialName);
                        lootItem = new LootItem(material, name, minAmount, maxAmount, chance, enchantments, lore);
                    }

                    category.addItem(lootItem);

                } catch (Exception e) {
                    plugin.getLogger().warning("§c解析战利品物品失败: " + e.getMessage());
                }
            }

            categories.put(categoryName, category);
        }

        plugin.getLogger().info("§a已加载 " + categories.size() + " 个战利品品类");
    }

    /**
     * 从 LootGUIManager 加载GUI管理的物品
     * 直接使用中文箱子类型名称:
     * - putong (普通)
     * - youpin (优品)
     * - jipin (极品)
     */
    private void loadGUIManagedItems() {
        LootGUIManager guiManager = plugin.getLootGUIManager();
        if (guiManager == null) {
            plugin.getLogger().warning("§cLootGUIManager 未初始化,跳过GUI物品加载");
            return;
        }

        // 清空缓存
        guiManagedItems.clear();

        // 直接使用中文箱子类型名称
        guiManagedItems.put("putong", guiManager.getItems("putong"));
        guiManagedItems.put("youpin", guiManager.getItems("youpin"));
        guiManagedItems.put("jipin", guiManager.getItems("jipin"));

        int totalItems = guiManagedItems.values().stream()
                .mapToInt(List::size)
                .sum();

        if (totalItems > 0) {
            plugin.getLogger().info("§a已加载 " + totalItems + " 个GUI管理的物品");
            plugin.getLogger().info("§7  - 普通箱子: " + guiManagedItems.get("putong").size() + " 个物品");
            plugin.getLogger().info("§7  - 优品箱子: " + guiManagedItems.get("youpin").size() + " 个物品");
            plugin.getLogger().info("§7  - 极品箱子: " + guiManagedItems.get("jipin").size() + " 个物品");
        }
    }

    /**
     * 填充游戏世界的所有箱子
     * @param world 游戏世界
     */
    public void fillAllChests(World world) {
        if (!enabled) {
            return;
        }

        plugin.getLogger().info("§e正在填充世界 " + world.getName() + " 的箱子...");

        int chestCount = 0;
        int filledCount = 0;

        // 遍历已加载的区块
        for (Chunk chunk : world.getLoadedChunks()) {
            for (BlockState blockState : chunk.getTileEntities()) {
                if (blockState instanceof Chest) {
                    chestCount++;
                    Chest chest = (Chest) blockState;
                    fillChest(chest.getInventory());
                    filledCount++;
                }
            }
        }

        plugin.getLogger().info("§a已填充 " + filledCount + "/" + chestCount + " 个箱子");
    }

    /**
     * 填充单个箱子
     * @param inventory 箱子背包
     */
    public void fillChest(Inventory inventory) {
        fillChest(inventory, "putong"); // 默认普通箱子
    }

    /**
     * 填充单个箱子（指定箱子类型）
     * @param inventory 箱子背包
     * @param chestType 箱子类型 (putong, youpin, jipin)
     */
    public void fillChest(Inventory inventory, String chestType) {
        // 从Inventory获取世界
        World world = inventory.getLocation() != null ? inventory.getLocation().getWorld() : null;
        String worldName = world != null ? world.getName() : "unknown";

        fillChest(inventory, chestType, worldName);
    }

    /**
     * 填充单个箱子（指定箱子类型和世界）
     * @param inventory 箱子背包
     * @param chestType 箱子类型 (putong, youpin, jipin)
     * @param worldName 世界名称
     */
    public void fillChest(Inventory inventory, String chestType, String worldName) {
        // 清空箱子
        if (clearBeforeFill) {
            inventory.clear();
        }

        // 物品数量现在由奖励组系统管理，不再使用固定数量
        // 通过奖励组的 minslots/maxslots 控制填充数量

        // 尝试使用自定义品类系统 (优先级最高)
        if (tryCustomCategoryFill(inventory, chestType, worldName)) {
            return; // 自定义品类系统成功填充
        }

        // 尝试使用增强GUI系统 (物品数量由奖励组控制)
        if (tryEnhancedGUIFill(inventory, chestType)) {
            return; // 增强系统成功填充
        }

        // 回退到传统GUI系统
        if (tryTraditionalGUIFill(inventory, chestType)) {
            return; // 传统GUI系统成功填充
        }

        // 最后回退到基础配置填充
        fallbackToConfigFill(inventory);
    }

    /**
     * 尝试使用自定义品类系统填充
     * 基于TOML配置文件中的自定义品类定义
     */
    private boolean tryCustomCategoryFill(Inventory inventory, String chestType, String worldName) {
        try {
            // 检查是否有自定义品类管理器
            if (customCategoryManager == null) {
                return false;
            }

            // 获取世界的箱子权重配置
            Map<String, Integer> chestWeights = getWorldChestWeights(worldName, chestType);

            // 根据权重选择一个奖励组
            String selectedGroup = selectRewardGroupByWeight(chestWeights);
            if (selectedGroup == null) {
                return false;
            }

            // 尝试匹配自定义品类 (jichu, youpin, xiyou 或其他自定义品类)
            CustomCategory category = customCategoryManager.getCategory(selectedGroup);
            if (category == null || !category.isEnabled() || !category.shouldFill()) {
                return false;
            }

            // 使用自定义品类逻辑填充箱子
            return fillWithCustomCategory(inventory, category);

        } catch (Exception e) {
            plugin.getLogger().warning("§c自定义品类系统填充失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 使用自定义品类填充箱子
     */
    private boolean fillWithCustomCategory(Inventory inventory, CustomCategory category) {
        // 获取品类配置
        int slotCount = category.getRandomSlotCount();  // 随机填充格数
        int stackCount = category.getRandomStackCount();  // 随机堆叠数量

        if (slotCount <= 0 || stackCount <= 0) {
            return false;
        }

        // 获取物品列表 (使用默认物品，后续可以扩展为使用GUI物品)
        List<String> itemIds = new ArrayList<>();
        if (customCategoryManager != null) {
            itemIds = customCategoryManager.getDefaultItems();
        }
        if (itemIds.isEmpty()) {
            return false;
        }

        List<ItemStack> itemsToPlace = new ArrayList<>();

        // 随机选择物品并创建ItemStack
        for (int i = 0; i < slotCount; i++) {
            String itemId = itemIds.get(random.nextInt(itemIds.size()));
            ItemStack item = createItemFromId(itemId, stackCount);
            if (item != null) {
                itemsToPlace.add(item);
            }
        }

        if (itemsToPlace.isEmpty()) {
            return false;
        }

        // 放置物品到箱子
        placeItemsInInventory(inventory, itemsToPlace);

        if (plugin.getConfig().getBoolean("debug.enabled", false)) {
            plugin.getLogger().info("§e[Debug] 使用自定义品类 " + category.getName() + " 填充箱子: " +
                itemsToPlace.size() + " 个物品 (格数: " + slotCount + ", 堆叠: " + stackCount + ")");
        }

        return true;
    }

    /**
     * 根据物品ID创建ItemStack
     */
    private ItemStack createItemFromId(String itemId, int amount) {
        // 完善的参数验证
        if (itemId == null || itemId.trim().isEmpty()) {
            return null;
        }

        if (amount <= 0) {
            return null;
        }

        try {
            String[] parts = itemId.split(":");
            if (parts.length < 2) {
                return null;
            }

            String namespace = parts[0];
            String itemName = parts[1];

            if (itemName == null || itemName.trim().isEmpty()) {
                return null;
            }

            Material material = Material.matchMaterial(itemName.toUpperCase());
            if (material == null) {
                // 尝试一些常见的映射
                material = getMaterialByName(itemName);
            }

            if (material == null || material == Material.AIR) {
                return null;
            }

            ItemStack item = new ItemStack(material);
            item.setAmount(Math.min(amount, item.getMaxStackSize()));

            // 设置物品元数据（可选）
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                // 可以在这里添加自定义Lore等
                meta.setDisplayName(ChatColor.YELLOW + "天充箱子物品");
                item.setItemMeta(meta);
            }

            return item;

        } catch (Exception e) {
            plugin.getLogger().warning("§c无法创建物品: " + itemId + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 根据名称获取Material（简单的映射）
     */
    private Material getMaterialByName(String name) {
        // 常见的物品映射
        Map<String, Material> materialMap = new HashMap<>();
        materialMap.put("stone", Material.STONE);
        materialMap.put("dirt", Material.DIRT);
        materialMap.put("oak_planks", Material.OAK_PLANKS);
        materialMap.put("iron_ingot", Material.IRON_INGOT);
        materialMap.put("gold_ingot", Material.GOLD_INGOT);
        materialMap.put("diamond", Material.DIAMOND);
        materialMap.put("emerald", Material.EMERALD);
        materialMap.put("cooked_beef", Material.COOKED_BEEF);
        materialMap.put("bread", Material.BREAD);
        materialMap.put("golden_apple", Material.GOLDEN_APPLE);
        materialMap.put("iron_sword", Material.IRON_SWORD);
        materialMap.put("diamond_sword", Material.DIAMOND_SWORD);
        materialMap.put("bow", Material.BOW);
        materialMap.put("arrow", Material.ARROW);
        materialMap.put("iron_pickaxe", Material.IRON_PICKAXE);
        materialMap.put("diamond_pickaxe", Material.DIAMOND_PICKAXE);
        materialMap.put("iron_helmet", Material.IRON_HELMET);
        materialMap.put("iron_chestplate", Material.IRON_CHESTPLATE);
        materialMap.put("iron_leggings", Material.IRON_LEGGINGS);
        materialMap.put("iron_boots", Material.IRON_BOOTS);
        materialMap.put("diamond_helmet", Material.DIAMOND_HELMET);
        materialMap.put("diamond_chestplate", Material.DIAMOND_CHESTPLATE);
        materialMap.put("diamond_leggings", Material.DIAMOND_LEGGINGS);
        materialMap.put("diamond_boots", Material.DIAMOND_BOOTS);

        return materialMap.get(name.toLowerCase());
    }

    /**
     * 尝试使用增强GUI系统填充
     */
    private boolean tryEnhancedGUIFill(Inventory inventory, String chestType) {
        // 增强GUI系统暂时禁用，直接返回false
        return false;
    }

    /**
     * 尝试使用传统GUI系统填充
     */
    private boolean tryTraditionalGUIFill(Inventory inventory, String chestType) {
        List<ItemStack> guiItems = guiManagedItems.get(chestType);
        if (guiItems == null || guiItems.isEmpty()) {
            return false;
        }

        // 使用传统等概率随机
        List<ItemStack> selectedItems = new ArrayList<>();
        for (int i = 0; i < guiItems.size() && i < inventory.getSize(); i++) {
            ItemStack randomItem = guiItems.get(random.nextInt(guiItems.size()));
            selectedItems.add(randomItem.clone());
        }

        // 放置物品
        placeItemsInInventory(inventory, selectedItems);

        if (plugin.getConfig().getBoolean("debug.enabled", false)) {
            plugin.getLogger().info("§e[Debug] 使用传统GUI系统填充 " + chestType + " 箱子: " + selectedItems.size() + " 个物品");
        }

        return true;
    }

    /**
     * 回退到配置文件填充
     */
    private void fallbackToConfigFill(Inventory inventory) {
        int fillCount = Math.min(5, inventory.getSize() / 9); // 基础填充5个物品
        for (int i = 0; i < fillCount; i++) {
            LootItem lootItem = selectRandomLootItem();
            if (lootItem == null) {
                continue;
            }

            // 生成物品
            ItemStack itemStack = createItemStack(lootItem);

            // 随机放置位置
            placeItemInInventory(inventory, itemStack);
        }

        if (plugin.getConfig().getBoolean("debug.enabled", false)) {
            plugin.getLogger().info("§e[Debug] 使用基础配置填充箱子: " + fillCount + " 个物品");
        }
    }

    /**
     * 批量放置物品到箱子
     */
    private void placeItemsInInventory(Inventory inventory, List<ItemStack> items) {
        for (ItemStack item : items) {
            placeItemInInventory(inventory, item);
        }
    }

    /**
     * 放置单个物品到箱子（智能寻找空位）
     */
    private void placeItemInInventory(Inventory inventory, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        // 首先尝试堆叠到已有物品
        if (item.getMaxStackSize() > 1) {
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack existingItem = inventory.getItem(i);
                if (existingItem != null && existingItem.isSimilar(item) &&
                    existingItem.getAmount() < existingItem.getMaxStackSize()) {

                    int canAdd = Math.min(item.getAmount(),
                                        existingItem.getMaxStackSize() - existingItem.getAmount());
                    existingItem.setAmount(existingItem.getAmount() + canAdd);
                    item.setAmount(item.getAmount() - canAdd);

                    if (item.getAmount() <= 0) {
                        return; // 物品全部堆叠完成
                    }
                }
            }
        }

        // 寻找空位放置剩余物品
        int attempts = 0;
        while (item.getAmount() > 0 && attempts < inventory.getSize()) {
            int slot = random.nextInt(inventory.getSize());
            if (inventory.getItem(slot) == null) {
                int stackSize = Math.min(item.getAmount(), item.getMaxStackSize());
                ItemStack placeItem = item.clone();
                placeItem.setAmount(stackSize);
                inventory.setItem(slot, placeItem);
                item.setAmount(item.getAmount() - stackSize);
            }
            attempts++;
        }
    }

    /**
     * 获取世界的箱子类型权重配置
     * @param worldName 世界名称
     * @param chestType 箱子类型
     * @return 权重映射 (奖励组名 -> 权重)
     */
    private Map<String, Integer> getWorldChestWeights(String worldName, String chestType) {
        Map<String, Integer> weights = new HashMap<>();

        try {
            // 获取世界配置
            FileConfiguration worldConfig = worldConfigManager.getWorldConfig(worldName);

            if (worldConfig != null) {
                // 检查是否使用自定义权重
                boolean useCustomWeights = worldConfig.getBoolean("chests.use-custom-weights", false);

                if (useCustomWeights) {
                    // 使用世界特定的权重配置
                    ConfigurationSection chestSection = worldConfig.getConfigurationSection("chests.type-weights");
                    if (chestSection != null) {
                        for (String key : chestSection.getKeys(false)) {
                            weights.put(key, chestSection.getInt(key));
                        }
                        plugin.getLogger().info("§e[Debug] 使用世界 " + worldName + " 的自定义箱子权重");
                    }
                } else {
                    plugin.getLogger().info("§e[Debug] 世界 " + worldName + " 使用全局箱子权重");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("§c读取世界箱子权重配置失败: " + e.getMessage());
        }

        // 如果世界配置为空或未启用自定义权重，使用全局默认值
        if (weights.isEmpty()) {
            weights.put("jichu", 70);    // 基础奖励组
            weights.put("youpin", 25);   // 优品奖励组
            weights.put("jipin", 5);     // 极品奖励组
        }

        return weights;
    }

    /**
     * 根据权重选择奖励组
     * @param weights 权重映射 (奖励组名 -> 权重)
     * @return 选中的奖励组名称
     */
    private String selectRewardGroupByWeight(Map<String, Integer> weights) {
        if (weights == null || weights.isEmpty()) {
            return null;
        }

        // 计算总权重
        int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (totalWeight <= 0) {
            return null;
        }

        // 随机选择
        int randomWeight = random.nextInt(totalWeight);
        int currentWeight = 0;

        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            currentWeight += entry.getValue();
            if (randomWeight < currentWeight) {
                return entry.getKey();
            }
        }

        // 如果没选中（理论上不应该发生），返回第一个
        return weights.keySet().iterator().next();
    }

    /**
     * 随机选择一个战利品物品
     * @return 战利品物品
     */
    private LootItem selectRandomLootItem() {
        // 第一步：根据品类权重随机选择品类
        int totalWeight = categories.values().stream()
                .mapToInt(LootCategory::getWeight)
                .sum();

        if (totalWeight == 0) {
            return null;
        }

        int randomWeight = random.nextInt(totalWeight);
        int currentWeight = 0;

        LootCategory selectedCategory = null;

        for (LootCategory category : categories.values()) {
            currentWeight += category.getWeight();
            if (randomWeight < currentWeight) {
                selectedCategory = category;
                break;
            }
        }

        if (selectedCategory == null || selectedCategory.getItems().isEmpty()) {
            return null;
        }

        // 第二步：根据物品概率随机选择物品
        List<LootItem> availableItems = new ArrayList<>();
        for (LootItem item : selectedCategory.getItems()) {
            // 检查概率
            if (random.nextInt(100) < item.getChance()) {
                availableItems.add(item);
            }
        }

        if (availableItems.isEmpty()) {
            // 如果没有符合概率的物品，返回品类中的随机物品
            return selectedCategory.getItems().get(random.nextInt(selectedCategory.getItems().size()));
        }

        return availableItems.get(random.nextInt(availableItems.size()));
    }

    /**
     * 创建物品堆
     * @param lootItem 战利品物品
     * @return 物品堆
     */
    private ItemStack createItemStack(LootItem lootItem) {
        // 随机数量
        int amount = lootItem.getMinAmount();
        if (lootItem.getMaxAmount() > lootItem.getMinAmount()) {
            amount += random.nextInt(lootItem.getMaxAmount() - lootItem.getMinAmount() + 1);
        }

        ItemStack itemStack;

        // 检查是否为Pixelmon物品
        if (lootItem.isPixelmonItem()) {
            // 使用Pixelmon API创建物品
            itemStack = createPixelmonItem(lootItem.getPixelmonItemId(), amount);
            if (itemStack == null) {
                plugin.getLogger().warning("§c无法创建Pixelmon物品: " + lootItem.getPixelmonItemId());
                return null;
            }
        } else {
            // 普通Minecraft物品
            itemStack = new ItemStack(lootItem.getMaterial(), amount);
        }

        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null) {
            // 设置名称
            if (lootItem.getName() != null && !lootItem.getName().isEmpty()) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', lootItem.getName()));
            }

            // 设置Lore
            if (!lootItem.getLore().isEmpty()) {
                List<String> lore = new ArrayList<>();
                for (String line : lootItem.getLore()) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(lore);
            }

            itemStack.setItemMeta(meta);
        }

        // 添加附魔（仅对非Pixelmon物品）
        if (!lootItem.isPixelmonItem()) {
            for (LootItem.EnchantmentData enchantData : lootItem.getEnchantments()) {
                itemStack.addUnsafeEnchantment(enchantData.getEnchantment(), enchantData.getLevel());
            }
        }

        return itemStack;
    }

    /**
     * 创建Pixelmon物品
     * @param itemId Pixelmon物品ID
     * @param amount 数量
     * @return ItemStack
     */
    private ItemStack createPixelmonItem(String itemId, int amount) {
        try {
            // 使用Pixelmon API创建物品
            // ItemStack stack = PixelmonItems.getItem(itemId, amount);
            // 临时实现: 使用反射调用Pixelmon API
            Class<?> pixelmonItemsClass = Class.forName("com.pixelmonmod.pixelmon.items.PixelmonItems");
            java.lang.reflect.Method getItemMethod = pixelmonItemsClass.getMethod("getPixelmonItem", String.class, int.class);
            Object result = getItemMethod.invoke(null, itemId, amount);
            return (ItemStack) result;
        } catch (Exception e) {
            // 如果Pixelmon API调用失败，记录警告并返回null
            plugin.getLogger().warning("§c创建Pixelmon物品失败: " + itemId + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 重载配置
     */
    public void reload() {
        categories.clear();
        loadLootConfig();
    }

    /**
     * 重载配置（别名方法）
     */
    public void reloadConfig() {
        reload();
    }
}
