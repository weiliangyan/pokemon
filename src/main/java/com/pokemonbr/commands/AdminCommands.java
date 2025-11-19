package com.pokemonbr.commands;

import com.pokemonbr.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 管理员指令处理器
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class AdminCommands implements CommandExecutor {

    private final Main plugin;

    public AdminCommands(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 检查权限
        if (!sender.hasPermission("pbr.admin")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "start":
                return handleForceStart(sender, args);

            case "stop":
                return handleForceStop(sender, args);

            case "reload":
                return handleReload(sender);

            case "setlobby":
                return handleSetLobby(sender);

            case "setcenter":
            case "ssq": // 简写: set shrink queue
                return handleSetCenter(sender, args);

            case "setspawn":
            case "csd": // 简写: corner spawn diagonal
                return handleSetSpawn(sender, args);

            case "backup":
                return handleBackupWorld(sender, args);

            case "restore":
                return handleRestoreWorld(sender, args);

            default:
                sendHelp(sender);
                return true;
        }
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== PBR管理员指令 ==========");
        sender.sendMessage(ChatColor.YELLOW + "/pbradmin start <队列ID> " + ChatColor.GRAY + "- 强制开始游戏");
        sender.sendMessage(ChatColor.YELLOW + "/pbradmin stop <游戏ID> " + ChatColor.GRAY + "- 强制结束游戏");
        sender.sendMessage(ChatColor.YELLOW + "/pbradmin reload " + ChatColor.GRAY + "- 重载配置");
        sender.sendMessage(ChatColor.YELLOW + "/pbradmin setlobby " + ChatColor.GRAY + "- 设置大厅位置");
        sender.sendMessage(ChatColor.YELLOW + "/pbradmin setcenter <世界名> " + ChatColor.GRAY + "- 设置缩圈中心");
        sender.sendMessage(ChatColor.AQUA + "  简写: /pbr ssq <世界名>");
        sender.sendMessage(ChatColor.YELLOW + "/pbradmin setspawn <世界名> <1|2> " + ChatColor.GRAY + "- 设置出生点");
        sender.sendMessage(ChatColor.AQUA + "  简写: /pbr csd <世界名> <1|2>");
        sender.sendMessage(ChatColor.YELLOW + "/pbradmin backup <世界名> " + ChatColor.GRAY + "- 备份游戏世界");
        sender.sendMessage(ChatColor.YELLOW + "/pbradmin restore <世界名> " + ChatColor.GRAY + "- 还原游戏世界");
        sender.sendMessage(ChatColor.GOLD + "================================");
    }

    /**
     * 强制开始游戏
     */
    private boolean handleForceStart(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /pbradmin start <队列ID>");
            return true;
        }

        try {
            int queueId = Integer.parseInt(args[1]);

            boolean success = plugin.getQueueManager().forceStartGame(queueId);

            if (success) {
                sender.sendMessage(getMessage("admin.force-start-success")
                        .replace("{queue}", String.valueOf(queueId)));
            } else {
                sender.sendMessage(getMessage("admin.force-start-failed")
                        .replace("{queue}", String.valueOf(queueId)));
            }

        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "队列ID必须是数字！");
        }

        return true;
    }

    /**
     * 强制结束游戏
     */
    private boolean handleForceStop(CommandSender sender, String[] args) {
        // 检查权限
        if (!sender.hasPermission("pbr.admin.stop")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /pbradmin stop <游戏UUID>");
            sender.sendMessage(ChatColor.GRAY + "提示: 使用 /pbradmin list 查看所有游戏");
            return true;
        }

        // TODO: 实现强制结束游戏
        sender.sendMessage(ChatColor.GREEN + "强制结束功能暂未实现");

        return true;
    }

    /**
     * 重载配置
     */
    private boolean handleReload(CommandSender sender) {
        try {
            sender.sendMessage(ChatColor.YELLOW + "正在重载配置文件...");

            // 1. 重载主配置文件
            plugin.reloadConfig();

            // 2. 重载所有子配置文件 (ConfigManager 管理的)
            plugin.getConfigManager().reloadConfigs();

            // 3. 重载 LootGUI 配置
            if (plugin.getLootGUIManager() != null) {
                plugin.getLootGUIManager().reloadConfig();
                plugin.getLogger().info("§a已重载 LootGUI 配置");
            }

            // 4. 重载世界配置（WorldConfigManager）
            if (plugin.getWorldConfigManager() != null) {
                plugin.getWorldConfigManager().reloadAllWorldConfigs();
                plugin.getLogger().info("§a已重载所有世界配置");
            }

            // 5. 自定义品类配置已整合到LootGUI中，通过LootGUI重载即可

            // 6. 重载战利品箱配置
            if (plugin.getLootChestManager() != null) {
                plugin.getLootChestManager().reloadConfig();
                plugin.getLogger().info("§a已重载战利品箱配置");
            }

            // 7. 显示当前存储模式
            String storageType = plugin.getDatabaseManager().getStorageType();
            sender.sendMessage(ChatColor.GREEN + "✓ 配置文件重载成功！");
            sender.sendMessage(ChatColor.GRAY + "当前存储模式: " + ChatColor.AQUA + storageType);

            // 8. 如果更改了存储模式，给出提示
            String configStorageType = plugin.getConfig().getString("storage.type", "YAML").toUpperCase();
            if (!storageType.equals(configStorageType)) {
                sender.sendMessage(ChatColor.YELLOW + "⚠ 检测到存储模式已更改");
                sender.sendMessage(ChatColor.YELLOW + "  当前运行: " + storageType + " | 配置文件: " + configStorageType);
                sender.sendMessage(ChatColor.YELLOW + "  如需切换存储模式,请重启服务器");
            }

        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "重载配置失败: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    /**
     * 设置大厅位置
     */
    private boolean handleSetLobby(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        org.bukkit.Location loc = player.getLocation();

        // 保存到配置
        plugin.getConfig().set("queue.lobby-world", loc.getWorld().getName());
        plugin.getConfig().set("queue.lobby-location.x", loc.getX());
        plugin.getConfig().set("queue.lobby-location.y", loc.getY());
        plugin.getConfig().set("queue.lobby-location.z", loc.getZ());
        plugin.getConfig().set("queue.lobby-location.yaw", loc.getYaw());
        plugin.getConfig().set("queue.lobby-location.pitch", loc.getPitch());
        plugin.saveConfig();

        sender.sendMessage(getMessage("admin.lobby-set"));

        return true;
    }

    /**
     * 设置缩圈中心点
     * 用法: /pbradmin setcenter <世界名> 或 /pbr ssq <世界名>
     */
    private boolean handleSetCenter(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("player-only"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /pbradmin setcenter <世界名>");
            sender.sendMessage(ChatColor.AQUA + "简写: /pbr ssq <世界名>");
            sender.sendMessage(ChatColor.GRAY + "提示: 站在你想设置为中心的位置执行此指令");
            return true;
        }

        Player player = (Player) sender;
        String worldName = args[1];
        org.bukkit.Location loc = player.getLocation();

        // 使用新的 WorldConfigManager
        plugin.getWorldConfigManager().setShrinkCenter(worldName, loc.getX(), loc.getZ());

        sender.sendMessage(ChatColor.GREEN + "✓ 已设置世界 " + ChatColor.YELLOW + worldName + ChatColor.GREEN + " 的缩圈中心");
        sender.sendMessage(ChatColor.GRAY + "  坐标: X=" + String.format("%.2f", loc.getX()) + ", Z=" + String.format("%.2f", loc.getZ()));
        sender.sendMessage(ChatColor.GRAY + "  配置已保存到: worlds/" + worldName + ".yml");

        return true;
    }

    /**
     * 设置出生点（对角点）
     * 用法: /pbradmin setspawn <世界名> <1|2> 或 /pbr csd <世界名> <1|2>
     */
    private boolean handleSetSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("player-only"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "用法: /pbradmin setspawn <世界名> <1|2>");
            sender.sendMessage(ChatColor.AQUA + "简写: /pbr csd <世界名> <1|2>");
            sender.sendMessage(ChatColor.GRAY + "提示: 1=第一个对角点, 2=第二个对角点");
            sender.sendMessage(ChatColor.GRAY + "说明: 出生点将在这两个对角之间随机生成");
            return true;
        }

        Player player = (Player) sender;
        String worldName = args[1];
        String pointNum = args[2];

        if (!pointNum.equals("1") && !pointNum.equals("2")) {
            sender.sendMessage(ChatColor.RED + "对角点编号必须是 1 或 2！");
            return true;
        }

        org.bukkit.Location loc = player.getLocation();

        // 获取或创建世界配置
        org.bukkit.configuration.file.FileConfiguration worldConfig =
            plugin.getWorldConfigManager().getWorldConfig(worldName);

        if (worldConfig == null) {
            worldConfig = plugin.getWorldConfigManager().createWorldConfig(worldName);
        }

        // 设置对角点
        String cornerPath = pointNum.equals("1") ?
            "spawn.fixed-diagonal.corner1" : "spawn.fixed-diagonal.corner2";

        worldConfig.set(cornerPath + ".x", loc.getX());
        worldConfig.set(cornerPath + ".y", loc.getY());
        worldConfig.set(cornerPath + ".z", loc.getZ());

        // 切换到固定对角模式
        worldConfig.set("spawn.mode", "fixed");

        // 保存配置
        plugin.getWorldConfigManager().saveWorldConfig(worldName);

        sender.sendMessage(ChatColor.GREEN + "✓ 已设置世界 " + ChatColor.YELLOW + worldName +
            ChatColor.GREEN + " 的出生点对角 #" + pointNum);
        sender.sendMessage(ChatColor.GRAY + "  坐标: X=" + String.format("%.2f", loc.getX()) +
            ", Y=" + String.format("%.2f", loc.getY()) + ", Z=" + String.format("%.2f", loc.getZ()));

        // 检查是否已设置两个对角点
        if (worldConfig.contains("spawn.fixed-diagonal.corner1") &&
            worldConfig.contains("spawn.fixed-diagonal.corner2")) {
            sender.sendMessage(ChatColor.AQUA + "✓ 该世界的两个对角点已全部设置完成！");
            sender.sendMessage(ChatColor.GRAY + "  配置已保存到: worlds/" + worldName + ".yml");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "⚠ 请记得设置另一个对角点");
        }

        return true;
    }

    /**
     * 备份游戏世界
     */
    private boolean handleBackupWorld(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /pbradmin backup <世界名>");
            return true;
        }

        String worldName = args[1];

        sender.sendMessage(ChatColor.YELLOW + "正在备份世界 " + worldName + "...");

        // 异步执行备份
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = plugin.getWorldManager().backupWorld(worldName);

            if (success) {
                sender.sendMessage(ChatColor.GREEN + "世界 " + worldName + " 备份成功！");
            } else {
                sender.sendMessage(ChatColor.RED + "世界 " + worldName + " 备份失败！请查看控制台日志");
            }
        });

        return true;
    }

    /**
     * 还原游戏世界
     */
    private boolean handleRestoreWorld(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /pbradmin restore <世界名>");
            return true;
        }

        String worldName = args[1];

        sender.sendMessage(ChatColor.YELLOW + "正在还原世界 " + worldName + "...");
        sender.sendMessage(ChatColor.GRAY + "注意: 该世界中的所有玩家将被传送到主世界");

        // 异步执行还原
        plugin.getWorldManager().restoreWorldAsync(worldName);

        sender.sendMessage(ChatColor.GREEN + "还原任务已提交！请等待完成");

        return true;
    }

    /**
     * 获取消息
     */
    private String getMessage(String key) {
        String prefix = plugin.getConfigManager().getMessagesConfig().getString("prefix", "");
        String message = plugin.getConfigManager().getMessagesConfig().getString(key, key);
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }
}
