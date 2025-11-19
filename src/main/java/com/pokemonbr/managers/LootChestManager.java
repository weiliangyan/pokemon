package com.pokemonbr.managers;

import com.pokemonbr.Main;
import com.pokemonbr.models.LootCategory;
import com.pokemonbr.models.LootItem;
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
import java.util.concurrent.ThreadLocalRandom;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

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
    private final Random random;

    // 全局设置
    private boolean enabled;
    private boolean clearBeforeFill;

    // loot-system.yml 奖励组缓存
    private final Map<String, LootCategory> lootSystemCategories = new HashMap<>();

    // 多世界配置管理器
    private WorldConfigManager worldConfigManager;

    public LootChestManager(Main plugin) {
        this.plugin = plugin;
        this.categories = new HashMap<>();
        this.random = new Random();
        this.worldConfigManager = plugin.getWorldConfigManager();

        loadLootConfig();
    }

    /**
     * 加载战利品配置
     */
    private void loadLootConfig() {
        try {
            // 读取 loot-system.yml 配置文件
            FileConfiguration config = plugin.getConfigManager().getConfig("loot/loot-system.yml");

            // 加载全局设置
            enabled = config.getBoolean("global.enabled", true);
            clearBeforeFill = config.getBoolean("global.clear-before-fill", true);

            // 清空之前的缓存
            categories.clear();
            lootSystemCategories.clear();

            // 加载奖励组
            for (String groupName : config.getKeys(false)) {
                // 跳过全局配置节点
                if (groupName.equals("global") || groupName.equals("chest-types") ||
                    groupName.equals("chest-identification") || groupName.equals("fill-rules") ||
                    groupName.equals("fallback-items")) {
                    continue;
                }

                // 检查奖励组是否启用
                if (!config.getBoolean(groupName + ".enabled", true)) {
                    continue;
                }

                // 获取奖励组权重（用于 weighted random）
                int fillChance = (int) (config.getDouble(groupName + ".fill-chance", 0.5) * 100);

                // 获取物品数量范围
                int minSlots = config.getInt(groupName + ".min-slots", 2);
                int maxSlots = config.getInt(groupName + ".max-slots", 6);
                int minStack = config.getInt(groupName + ".min-stack", 1);
                int maxStack = config.getInt(groupName + ".max-stack", 8);

                // 创建奖励组
                LootCategory category = new LootCategory(groupName, fillChance, minSlots, maxSlots, minStack, maxStack);

                // 1. 加载手写配置的物品（items 节点 - MapList格式）
                List<Map<?, ?>> itemsList = config.getMapList(groupName + ".items");
                for (Map<?, ?> itemMap : itemsList) {
                    try {
                        String materialName = (String) itemMap.get("material");
                        if (materialName == null) {
                            plugin.getLogger().warning("§c奖励组 " + groupName + " 中存在无材质的物品配置，已跳过");
                            continue;
                        }

                        // 解析物品（手写配置格式）
                        LootItem lootItem;
                        if (materialName.startsWith("PIXELMON:")) {
                            // Pixelmon物品
                            String pixelmonItemId = materialName.substring(9);
                            lootItem = new LootItem(pixelmonItemId, "", 1, 1, 100, new ArrayList<>(), new ArrayList<>());
                        } else {
                            // 普通Minecraft物品
                            Material material = Material.valueOf(materialName);
                            lootItem = new LootItem(material, "", 1, 1, 100, new ArrayList<>(), new ArrayList<>());
                        }

                        category.addItem(lootItem);

                    } catch (Exception e) {
                        plugin.getLogger().warning("§c解析奖励组 " + groupName + " 中的手写物品失败: " + e.getMessage());
                    }
                }

                // 2. 加载GUI保存的物品（gui-items 节点 - Base64字符串列表）
                List<String> guiItemsList = config.getStringList(groupName + ".gui-items");
                if (!guiItemsList.isEmpty()) {
                    plugin.getLogger().info("§7[GUI加载] 为奖励组 " + groupName + " 加载 " + guiItemsList.size() + " 个GUI保存的物品");

                    for (String base64Item : guiItemsList) {
                        try {
                            // 从Base64反序列化ItemStack
                            ItemStack itemStack = itemFromBase64(base64Item);
                            if (itemStack != null && itemStack.getType() != Material.AIR) {
                                // 将ItemStack转换为LootItem
                                // 这里使用简化的构造，因为GUI保存的物品已经包含了完整信息
                                LootItem lootItem = new LootItem(
                                    itemStack.getType(),
                                    itemStack.getItemMeta() != null ? itemStack.getItemMeta().getDisplayName() : "",
                                    itemStack.getAmount(),
                                    itemStack.getAmount(),
                                    100, // GUI物品默认100%概率
                                    new ArrayList<>(),
                                    itemStack.getItemMeta() != null && itemStack.getItemMeta().hasLore() ?
                                        itemStack.getItemMeta().getLore() : new ArrayList<>()
                                );

                                category.addItem(lootItem);
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("§c解析奖励组 " + groupName + " 中的GUI物品失败: " + e.getMessage());
                        }
                    }
                }

                // 3. 记录加载统计
                int totalItems = category.getItems().size();
                if (totalItems > 0) {
                    String sourceInfo = itemsList.isEmpty() ? "仅GUI物品" :
                                      guiItemsList.isEmpty() ? "仅手写配置" :
                                      "手写配置 + GUI物品";
                    plugin.getLogger().info("§a奖励组 " + groupName + " 已加载 " + totalItems + " 个物品 (" + sourceInfo + ")");
                } else {
                    plugin.getLogger().warning("§c奖励组 " + groupName + " 没有加载到任何物品");
                }

                // 同时添加到两个缓存中（保持向后兼容）
                categories.put(groupName, category);
                lootSystemCategories.put(groupName, category);
            }

            plugin.getLogger().info("§a已加载 " + lootSystemCategories.size() + " 个 loot-system 奖励组");

        } catch (Exception e) {
            plugin.getLogger().severe("§c加载 loot-system.yml 配置文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 传统GUI系统已移除，现在统一使用loot-system.yml

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

        // 使用 loot-system.yml 填充系统 (统一入口)
        if (tryEnhancedGUIFill(inventory, chestType)) {
            return; // loot-system 系统成功填充
        }

        // 回退到基础配置填充
        fallbackToConfigFill(inventory);
    }

    // TOML自定义品类系统已移除，现在统一使用 loot-system.yml

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
     * 尝试使用 loot-system.yml 填充
     */
    private boolean tryEnhancedGUIFill(Inventory inventory, String chestType) {
        try {
            // 读取 loot-system.yml 中的箱子类型配置
            FileConfiguration config = plugin.getConfigManager().getConfig("loot/loot-system.yml");
            ConfigurationSection chestTypesSection = config.getConfigurationSection("chest-types");

            if (chestTypesSection == null) {
                plugin.getLogger().warning("§cloot-system.yml 中未找到 chest-types 配置，使用默认配置");
                return tryDefaultLootSystemFill(inventory);
            }

            ConfigurationSection chestTypeSection = chestTypesSection.getConfigurationSection(chestType);
            if (chestTypeSection == null) {
                plugin.getLogger().warning("§c未找到箱子类型 " + chestType + " 的配置，使用默认配置");
                return tryDefaultLootSystemFill(inventory);
            }

            // 获取该箱子类型的奖励组权重配置
            Map<String, Object> rewardGroups = chestTypeSection.getConfigurationSection("reward-groups").getValues(false);

            if (rewardGroups.isEmpty()) {
                plugin.getLogger().warning("§c箱子类型 " + chestType + " 没有配置奖励组，使用默认配置");
                return tryDefaultLootSystemFill(inventory);
            }

            // 加权随机选择一个奖励组
            String selectedGroup = selectWeightedRewardGroup(rewardGroups);
            if (selectedGroup == null) {
                return false;
            }

            // 获取选中的奖励组
            LootCategory category = lootSystemCategories.get(selectedGroup);
            if (category == null) {
                plugin.getLogger().warning("§c未找到奖励组 " + selectedGroup);
                return false;
            }

            // 确定要填充的物品数量
            int minItems = plugin.getConfig().getInt("global.items-per-chest.min", 3);
            int maxItems = plugin.getConfig().getInt("global.items-per-chest.max", 8);
            int totalItems = category.calculateTotalItems();
            int itemsToFill = Math.min(Math.max(minItems, totalItems / 3), Math.min(maxItems, inventory.getSize()));

            // 随机选择物品
            List<LootItem> selectedItems = category.selectRandomItems(itemsToFill);
            if (selectedItems.isEmpty()) {
                return false;
            }

            // 将 LootItem 转换为 ItemStack 并填充
            List<ItemStack> itemStacks = new ArrayList<>();
            for (LootItem lootItem : selectedItems) {
                ItemStack itemStack = lootItem.createItemStack();
                if (itemStack != null) {
                    itemStacks.add(itemStack);
                }
            }

            // 放置物品到箱子
            placeItemsInInventory(inventory, itemStacks);

            if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                plugin.getLogger().info("§e[Debug] 使用 loot-system 填充 " + chestType + " 箱子: " +
                    selectedGroup + " 奖励组，" + itemStacks.size() + " 个物品");
            }

            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("§c使用 loot-system.yml 填充失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 使用默认的 loot-system 配置填充（当找不到特定配置时）
     */
    private boolean tryDefaultLootSystemFill(Inventory inventory) {
        try {
            // 如果有基础的奖励组，使用 jichu 作为默认
            LootCategory defaultCategory = lootSystemCategories.get("jichu");
            if (defaultCategory != null) {
                int minItems = plugin.getConfig().getInt("global.items-per-chest.min", 3);
                int maxItems = plugin.getConfig().getInt("global.items-per-chest.max", 8);
                int totalItems = defaultCategory.calculateTotalItems();
                int itemsToFill = Math.min(Math.max(minItems, totalItems / 3), Math.min(maxItems, inventory.getSize()));

                List<LootItem> selectedItems = defaultCategory.selectRandomItems(itemsToFill);
                if (!selectedItems.isEmpty()) {
                    List<ItemStack> itemStacks = new ArrayList<>();
                    for (LootItem lootItem : selectedItems) {
                        ItemStack itemStack = lootItem.createItemStack();
                        if (itemStack != null) {
                            itemStacks.add(itemStack);
                        }
                    }
                    placeItemsInInventory(inventory, itemStacks);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("§c使用默认 loot-system 配置填充失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 根据权重随机选择奖励组
     */
    private String selectWeightedRewardGroup(Map<String, Object> rewardGroups) {
        int totalWeight = rewardGroups.values().stream()
                .mapToInt(obj -> (obj instanceof Number) ? ((Number) obj).intValue() : 0)
                .sum();

        if (totalWeight == 0) {
            return null;
        }

        int randomWeight = ThreadLocalRandom.current().nextInt(totalWeight);
        int currentWeight = 0;

        for (Map.Entry<String, Object> entry : rewardGroups.entrySet()) {
            int weight = (entry.getValue() instanceof Number) ? ((Number) entry.getValue()).intValue() : 0;
            currentWeight += weight;

            if (randomWeight < currentWeight) {
                return entry.getKey();
            }
        }

        return null;
    }

    // 传统GUI系统已删除

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
        } catch (Exception e) {
            plugin.getLogger().warning("§c无法反序列化物品: " + e.getMessage());
            return null;
        }
    }
}
