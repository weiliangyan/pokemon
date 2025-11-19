package com.pokemonbr.managers;

import com.pokemonbr.Main;
import com.pokemonbr.models.CustomCategory;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义品类管理器
 * 使用 night-config TOML 库
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class CustomCategoryManager {

    private final Main plugin;
    private final Map<String, CustomCategory> categories;
    private final FileConfig configFile;

    public CustomCategoryManager(Main plugin) {
        this.plugin = plugin;
        this.categories = new HashMap<>();
        this.configFile = FileConfig.of(new File(plugin.getDataFolder(), "tianchonxz.toml"));

        loadConfig();
        plugin.getLogger().info("§a自定义品类管理器已加载");
    }

    /**
     * 加载配置文件
     */
    private void loadConfig() {
        try {
            configFile.load();

            // 检查是否有tianchonxz.categories
            if (configFile.contains("tianchonxz.categories")) {
                List<? extends Config> categoryConfigs = configFile.get("tianchonxz.categories");

                for (Config categoryConfig : categoryConfigs) {
                    if (categoryConfig.contains("name")) {
                        String name = categoryConfig.get("name");
                        CustomCategory category = new CustomCategory(name, 1, 1, 1, 1);

                        // 加载配置参数
                        if (categoryConfig.contains("minslots")) {
                            category.setMinSlots((int)categoryConfig.get("minslots"));
                        }
                        if (categoryConfig.contains("maxslots")) {
                            category.setMaxSlots((int) categoryConfig.get("maxslots"));
                        }
                        if (categoryConfig.contains("minstack")) {
                            category.setMinStack((int) categoryConfig.get("minstack"));
                        }
                        if (categoryConfig.contains("maxstack")) {
                            category.setMaxStack((int) categoryConfig.get("maxstack"));
                        }
                        if (categoryConfig.contains("fill_chance")) {
                            category.setFillChance((double) categoryConfig.get("fill_chance"));
                        }
                        if (categoryConfig.contains("enabled")) {
                            category.setEnabled((boolean) categoryConfig.get("enabled"));
                        }

                        categories.put(name, category);
                        plugin.getLogger().info("§a已加载品类: " + name);
                    }
                }
            }

            if (categories.isEmpty()) {
                createDefaultConfig();
            }

            plugin.getLogger().info("§a共加载 " + categories.size() + " 个自定义品类");

        } catch (Exception e) {
            plugin.getLogger().severe("§c加载TOML配置失败: " + e.getMessage());
            createDefaultConfig();
        }
    }

    /**
     * 创建默认配置文件
     */
    private void createDefaultConfig() {
        try {
            // 创建默认的TOML结构
            configFile.clear();

            // 添加默认品类配置
            List<Map<String, Object>> categories = new ArrayList<>();

            // 基础品类
            Map<String, Object> jichu = new HashMap<>();
            jichu.put("name", "jichu");
            jichu.put("minslots", 2);
            jichu.put("maxslots", 6);
            jichu.put("minstack", 1);
            jichu.put("maxstack", 3);
            jichu.put("fill_chance", 0.8);
            jichu.put("enabled", true);
            categories.add(jichu);

            // 优良品类
            Map<String, Object> youpin = new HashMap<>();
            youpin.put("name", "youpin");
            youpin.put("minslots", 1);
            youpin.put("maxslots", 4);
            youpin.put("minstack", 1);
            youpin.put("maxstack", 2);
            youpin.put("fill_chance", 0.6);
            youpin.put("enabled", true);
            categories.add(youpin);

            // 稀有品类
            Map<String, Object> xiyou = new HashMap<>();
            xiyou.put("name", "xiyou");
            xiyou.put("minslots", 1);
            xiyou.put("maxslots", 2);
            xiyou.put("minstack", 1);
            xiyou.put("maxstack", 1);
            xiyou.put("fill_chance", 0.3);
            xiyou.put("enabled", true);
            categories.add(xiyou);

            configFile.set("tianchonxz.categories", categories);
            configFile.save();

            // 重新加载配置
            loadConfig();

            plugin.getLogger().info("§a已创建默认TOML配置文件");

        } catch (Exception e) {
            plugin.getLogger().severe("§c创建默认配置文件失败: " + e.getMessage());
        }
    }

    /**
     * 保存配置文件
     */
    public void saveConfig() {
        try {
            List<Map<String, Object>> categoryList = new ArrayList<>();

            for (Map.Entry<String, CustomCategory> entry : categories.entrySet()) {
                String categoryName = entry.getKey();
                CustomCategory category = entry.getValue();

                Map<String, Object> categoryMap = new HashMap<>();
                categoryMap.put("name", categoryName);
                categoryMap.put("minslots", category.getMinSlots());
                categoryMap.put("maxslots", category.getMaxSlots());
                categoryMap.put("minstack", category.getMinStack());
                categoryMap.put("maxstack", category.getMaxStack());
                categoryMap.put("fill_chance", category.getFillChance());
                categoryMap.put("enabled", category.isEnabled());

                categoryList.add(categoryMap);
            }

            configFile.set("tianchonxz.categories", categoryList);
            configFile.save();

            plugin.getLogger().info("§aTOML配置已保存");

        } catch (Exception e) {
            plugin.getLogger().severe("§c保存TOML配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取品类
     */
    public CustomCategory getCategory(String name) {
        return categories.get(name);
    }

    /**
     * 获取所有品类
     */
    public Map<String, CustomCategory> getAllCategories() {
        return new HashMap<>(categories);
    }

    /**
     * 添加品类
     */
    public void addCategory(CustomCategory category) {
        categories.put(category.getName(), category);
        saveConfig();
    }

    /**
     * 移除品类
     */
    public void removeCategory(String name) {
        categories.remove(name);
        saveConfig();
    }

    /**
     * 检查品类是否存在
     */
    public boolean hasCategory(String name) {
        return categories.containsKey(name);
    }

    /**
     * 获取所有品类名称
     */
    public List<String> getCategoryNames() {
        return new ArrayList<>(categories.keySet());
    }

    /**
     * 重新加载配置
     */
    public void reloadConfig() {
        categories.clear();
        loadConfig();
    }

    /**
     * 获取默认物品列表
     */
    public List<String> getDefaultItems() {
        List<String> defaultItems = new ArrayList<>();
        defaultItems.add("diamond");
        defaultItems.add("iron_ingot");
        defaultItems.add("gold_ingot");
        defaultItems.add("bread");
        defaultItems.add("apple");
        defaultItems.add("stone");
        defaultItems.add("wood");
        defaultItems.add("coal");
        return defaultItems;
    }
}