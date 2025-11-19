package com.pokemonbr;

import com.pokemonbr.database.DatabaseManager;
import com.pokemonbr.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * PokemonBattleRoyale - 宝可梦大逃杀插件主类
 *
 * @author l1ang_Y5n
 * @qq 235236127
 * @version 1.0.0
 */
public class Main extends JavaPlugin {

    // 插件实例
    private static Main instance;

    // 管理器实例
    private DatabaseManager databaseManager;
    private ConfigManager configManager;
    private GameManager gameManager;
    private QueueManager queueManager;
    private PlayerDataManager playerDataManager;
    private WorldManager worldManager;
    private WorldTemplateManager worldTemplateManager;
    private RewardManager rewardManager;
    private ScoreboardManager scoreboardManager;
    private BorderShrinkManager borderShrinkManager;
    private VictoryEffectManager victoryEffectManager;
    private InviteManager inviteManager;
    private LootChestManager lootChestManager;
    private LootGUIManager lootGUIManager;
    private CustomCategoryManager customCategoryManager;
    private com.pokemonbr.managers.SimpleCustomCategoryManager simpleCustomCategoryManager;
    private WorldConfigManager worldConfigManager;
    private PermissionManager permissionManager;
    private com.pokemonbr.listeners.PixelmonBattleListener pixelmonBattleListener;

    @Override
    public void onEnable() {
        instance = this;

        // 输出炫酷启动信息
        printEnableMessage();

        // 初始化插件
        if (!initializePlugin()) {
            getLogger().severe("§c插件初始化失败！插件将被禁用");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("§a[PokemonBattleRoyale] 插件启动完成！");
    }

    @Override
    public void onDisable() {
        // 输出关闭信息
        printDisableMessage();

        // 清理资源
        cleanup();

        getLogger().info("§c[PokemonBattleRoyale] 插件已卸载！");
    }

    /**
     * 初始化插件
     * @return 是否成功
     */
    private boolean initializePlugin() {
        try {
            // 0. 检查必需依赖
            getLogger().info("§e[0/8] 正在检查插件依赖...");
            if (!checkDependencies()) {
                return false;
            }

            // 1. 加载配置文件
            getLogger().info("§e[1/8] 正在加载配置文件...");
            configManager = new ConfigManager(this);
            configManager.loadConfigs();

            // 2. 初始化数据库连接
            getLogger().info("§e[2/8] 正在连接数据库...");
            databaseManager = new DatabaseManager(this);
            if (!databaseManager.initialize()) {
                getLogger().severe("§c数据库连接失败！");
                return false;
            }

            // 3. 初始化玩家数据管理器
            getLogger().info("§e[3/15] 正在初始化玩家数据管理器...");
            playerDataManager = new PlayerDataManager(this);

            // 4. 初始化世界配置管理器（必须在其他管理器之前）
            getLogger().info("§e[4/15] 正在初始化世界配置管理器...");
            worldConfigManager = new WorldConfigManager(this);

            // 5. 初始化世界管理器
            getLogger().info("§e[5/15] 正在初始化世界管理器...");
            worldManager = new WorldManager(this);

            // 6. 初始化世界模板管理器
            getLogger().info("§e[6/15] 正在初始化世界模板管理器...");
            worldTemplateManager = new WorldTemplateManager(this);

            // 7. 初始化匹配队列管理器
            getLogger().info("§e[7/15] 正在初始化匹配队列管理器...");
            queueManager = new QueueManager(this, worldTemplateManager);

            // 8. 初始化游戏管理器
            getLogger().info("§e[8/15] 正在初始化游戏管理器...");
            gameManager = new GameManager(this);

            // 9. 初始化奖励管理器
            getLogger().info("§e[9/15] 正在初始化奖励管理器...");
            rewardManager = new RewardManager(this);

            // 10. 初始化计分板管理器
            getLogger().info("§e[10/15] 正在初始化计分板管理器...");
            scoreboardManager = new ScoreboardManager(this);

            // 11. 初始化边界缩圈管理器
            getLogger().info("§e[11/15] 正在初始化边界缩圈管理器...");
            borderShrinkManager = new BorderShrinkManager(this);

            // 12. 初始化胜利特效管理器
            getLogger().info("§e[12/15] 正在初始化胜利特效管理器...");
            victoryEffectManager = new VictoryEffectManager(this);

            // 13. 初始化邀请管理器
            getLogger().info("§e[13/15] 正在初始化邀请管理器...");
            inviteManager = new InviteManager(this);

            // 14. 初始化战利品系统（GUI + 箱子 + 品类管理）
            getLogger().info("§e[14/15] 正在初始化战利品系统...");
            lootGUIManager = new LootGUIManager(this);
            customCategoryManager = new CustomCategoryManager(this);
            simpleCustomCategoryManager = new com.pokemonbr.managers.SimpleCustomCategoryManager(this);
            lootChestManager = new LootChestManager(this);

            // 15. 注册指令和监听器
            getLogger().info("§e[15/15] 正在注册指令和监听器...");
            registerCommands();
            registerListeners();

            // 注册集成(PlaceholderAPI等)
            registerIntegrations();

            return true;

        } catch (Exception e) {
            getLogger().severe("§c插件初始化过程中发生错误：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检查必需依赖
     * @return 是否满足依赖要求
     */
    private boolean checkDependencies() {
        boolean allDependenciesPresent = true;

        // 检查 Pixelmon (Forge Mod)
        try {
            Class.forName("com.pixelmonmod.pixelmon.Pixelmon");
            getLogger().info("§a✓ Pixelmon Mod 已检测到");
        } catch (ClassNotFoundException e) {
            getLogger().severe("§c✗ 未检测到 Pixelmon Mod!");
            getLogger().severe("§c  请确保服务器已安装 Pixelmon Reforged 9.1.13 或更高版本");
            allDependenciesPresent = false;
        }

        // 检查 Vault
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            getLogger().info("§a✓ Vault 插件已检测到");
        } else {
            getLogger().severe("§c✗ 未检测到 Vault 插件!");
            getLogger().severe("§c  请从 https://www.spigotmc.org/resources/vault.34315/ 下载并安装");
            allDependenciesPresent = false;
        }

        // 检查 PlaceholderAPI (可选)
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("§a✓ PlaceholderAPI 插件已检测到 (可选)");
        } else {
            getLogger().warning("§e⚠ 未检测到 PlaceholderAPI 插件 (可选功能)");
            getLogger().warning("§e  部分变量功能将无法使用");
        }

        // 检查 LuckPerms (推荐，提供更好的权限管理)
        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            getLogger().info("§a✓ LuckPerms 权限系统已检测到 (推荐)");
        } else {
            getLogger().info("§7→ 未检测到 LuckPerms，将使用基础权限系统");
            getLogger().info("§7→ 建议安装 LuckPerms 获得更好的权限管理功能");
            getLogger().info("§7→ 下载地址: https://luckperms.net/");
        }

        if (!allDependenciesPresent) {
            getLogger().severe("§c========================================");
            getLogger().severe("§c  插件缺少必需依赖,无法启动!");
            getLogger().severe("§c  请安装上述缺失的依赖后重启服务器");
            getLogger().severe("§c========================================");
        }

        return allDependenciesPresent;
    }

    /**
     * 注册指令
     */
    private void registerCommands() {
        // 创建 TabCompleter 实例
        com.pokemonbr.commands.TabCompleter tabCompleter = new com.pokemonbr.commands.TabCompleter(this);

        // 注册玩家指令
        com.pokemonbr.commands.PlayerCommands playerCommands = new com.pokemonbr.commands.PlayerCommands(this);

        // 注册主指令 /pbr
        getCommand("pbr").setExecutor(playerCommands);
        getCommand("pbr").setTabCompleter(tabCompleter);

        // 注册独立指令
        getCommand("pbrjoin").setExecutor(playerCommands);
        getCommand("pbrleave").setExecutor(playerCommands);
        getCommand("pbrlobby").setExecutor(playerCommands);
        getCommand("pbrstats").setExecutor(playerCommands);
        getCommand("pbrstats").setTabCompleter(tabCompleter);
        getCommand("pbrspectate").setExecutor(playerCommands);
        getCommand("pbrspectate").setTabCompleter(tabCompleter);

        // 注册邀请指令
        getCommand("pbrinvite").setExecutor(new com.pokemonbr.commands.InviteCommands(this));
        getCommand("pbrinvite").setTabCompleter(tabCompleter);

        // 注册管理员指令
        com.pokemonbr.commands.AdminCommands adminCommands = new com.pokemonbr.commands.AdminCommands(this);
        getCommand("pbradmin").setExecutor(adminCommands);
        getCommand("pbradmin").setTabCompleter(tabCompleter);

        // 注册物品管理GUI指令
        com.pokemonbr.commands.LootGUICommands lootGUICommands = new com.pokemonbr.commands.LootGUICommands(this, lootGUIManager);
        getCommand("lootgui").setExecutor(lootGUICommands);

        // 注册快速物品管理GUI指令 /xxopen
        com.pokemonbr.commands.XXOpenCommand xxOpenCommand = new com.pokemonbr.commands.XXOpenCommand(this, lootGUIManager);
        getCommand("xxopen").setExecutor(xxOpenCommand);

        getLogger().info("§a指令注册完成");
    }

    /**
     * 注册监听器
     */
    private void registerListeners() {
        // 注册玩家监听器
        getServer().getPluginManager().registerEvents(
                new com.pokemonbr.listeners.PlayerListener(this), this);

        // 注册游戏限制监听器
        getServer().getPluginManager().registerEvents(
                new com.pokemonbr.listeners.GameRestrictionListener(this), this);

        // 注册Pixelmon战斗监听器（如果Pixelmon可用）
        if (getServer().getPluginManager().getPlugin("Pixelmon") != null) {
            pixelmonBattleListener = new com.pokemonbr.listeners.PixelmonBattleListener(this);
            getServer().getPluginManager().registerEvents(pixelmonBattleListener, this);
            getLogger().info("§aPixelmon战斗监听器已注册");
        }

        // 注册幸运方块兼容性监听器（如果检测到幸运方块插件）
        if (getServer().getPluginManager().getPlugin("LuckyBlock") != null ||
                getServer().getPluginManager().getPlugin("CrazyCrates") != null) {
            getServer().getPluginManager().registerEvents(
                    new com.pokemonbr.listeners.LuckyBlockListener(this), this);
            getLogger().info("§a幸运方块兼容性监听器已注册");
        }

        // 注册物品管理GUI监听器
        getServer().getPluginManager().registerEvents(
                new com.pokemonbr.listeners.LootGUIListener(lootGUIManager), this);

        getLogger().info("§a监听器注册完成");
    }

    /**
     * 检查必需依赖并注册集成
     * 注意: 新方法已整合到初始化流程,这个方法仅用于注册 PlaceholderAPI
     */
    private void registerIntegrations() {
        // 注册 PlaceholderAPI 变量(如果可用)
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.pokemonbr.integration.PBRPlaceholderExpansion(this).register();
            getLogger().info("§aPlaceholderAPI 变量已注册");
        }
    }

    /**
     * 清理资源
     */
    private void cleanup() {
        try {
            // 取消所有定时任务
            getServer().getScheduler().cancelTasks(this);

            // 保存所有玩家数据
            if (playerDataManager != null) {
                playerDataManager.saveAllCachedData();
            }

            // 关闭数据库连接
            if (databaseManager != null) {
                databaseManager.close();
            }

            // 保存所有配置
            if (configManager != null) {
                configManager.saveConfigs();
            }

            // 结束所有进行中的游戏
            if (gameManager != null) {
                gameManager.stopAllGames();
            }

            // 停止边界缩圈管理器
            if (borderShrinkManager != null) {
                borderShrinkManager.shutdown();
            }

            // 关闭所有玩家的物品管理GUI
            if (lootGUIManager != null) {
                lootGUIManager.closeAll();
            }

            getLogger().info("§a资源清理完成");

        } catch (Exception e) {
            getLogger().severe("§c资源清理过程中发生错误：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 输出炫酷的启动信息
     */
    private void printEnableMessage() {
        String version = getDescription().getVersion();

        getLogger().info("§6§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        getLogger().info("");
        getLogger().info("§b§l    ⚡ PokemonBattleRoyale v" + version + " ⚡");
        getLogger().info("§b§l         宝可梦大逃杀插件");
        getLogger().info("");
        getLogger().info("§a  ✓ §f插件已成功启动！");
        getLogger().info("");
        getLogger().info("§d  ★ §f感谢您的使用！");
        getLogger().info("");
        getLogger().info("§6§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * 输出关闭信息
     */
    private void printDisableMessage() {
        getLogger().info("§6§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        getLogger().info("§c§l  PokemonBattleRoyale §f已安全卸载");
        getLogger().info("§6§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ==================== Getter 方法 ====================

    public static Main getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public WorldTemplateManager getWorldTemplateManager() {
        return worldTemplateManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public BorderShrinkManager getBorderShrinkManager() {
        return borderShrinkManager;
    }

    public VictoryEffectManager getVictoryEffectManager() {
        return victoryEffectManager;
    }

    public InviteManager getInviteManager() {
        return inviteManager;
    }

    public LootChestManager getLootChestManager() {
        return lootChestManager;
    }

    public LootGUIManager getLootGUIManager() {
        return lootGUIManager;
    }

    
    public SimpleCustomCategoryManager getSimpleCustomCategoryManager() {
        return simpleCustomCategoryManager;
    }

    public WorldConfigManager getWorldConfigManager() {
        return worldConfigManager;
    }

    /**
     * 获取权限管理器
     */
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    /**
     * 获取自定义品类管理器
     */
    public CustomCategoryManager getCustomCategoryManager() {
        return customCategoryManager;
    }

    /**
     * 获取Pixelmon战斗监听器
     */
    public com.pokemonbr.listeners.PixelmonBattleListener getPixelmonBattleListener() {
        return pixelmonBattleListener;
    }
}
