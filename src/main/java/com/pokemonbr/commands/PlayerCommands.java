package com.pokemonbr.commands;

import com.pokemonbr.Main;
import com.pokemonbr.models.Game;
import com.pokemonbr.models.GamePlayer;
import com.pokemonbr.models.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * 玩家指令处理器
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class PlayerCommands implements CommandExecutor {

    private final Main plugin;

    public PlayerCommands(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 检查是否为玩家
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        String cmdName = command.getName().toLowerCase();

        switch (cmdName) {
            case "pbr":
                // 主指令,显示帮助
                if (args.length == 0) {
                    sendHelp(player);
                    return true;
                }
                // 根据子指令分发
                String subCmd = args[0].toLowerCase();
                switch (subCmd) {
                    case "join":
                        return handleJoin(player);
                    case "leave":
                        return handleLeave(player);
                    case "lobby":
                        return handleLobby(player);
                    case "stats":
                        return handleStats(player, args.length > 1 ? new String[]{args[1]} : new String[0]);
                    case "spectate":
                        return handleSpectate(player, args.length > 1 ? new String[]{args[1]} : new String[0]);
                    case "help":
                        sendHelp(player);
                        return true;
                    default:
                        sendHelp(player);
                        return true;
                }

            case "pbrjoin":
                return handleJoin(player);

            case "pbrleave":
                return handleLeave(player);

            case "pbrlobby":
                return handleLobby(player);

            case "pbrstats":
                return handleStats(player, args);

            case "pbrspectate":
                return handleSpectate(player, args);

            default:
                return false;
        }
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "========== 宝可梦大逃杀 ==========");
        player.sendMessage(ChatColor.YELLOW + "/pbr join " + ChatColor.GRAY + "或 " + ChatColor.YELLOW + "/pbrjoin " + ChatColor.GRAY + "- 加入匹配队列");
        player.sendMessage(ChatColor.YELLOW + "/pbr leave " + ChatColor.GRAY + "或 " + ChatColor.YELLOW + "/pbrleave " + ChatColor.GRAY + "- 退出匹配队列");
        player.sendMessage(ChatColor.YELLOW + "/pbr lobby " + ChatColor.GRAY + "或 " + ChatColor.YELLOW + "/pbrlobby " + ChatColor.GRAY + "- 返回大厅(淘汰后)");
        player.sendMessage(ChatColor.YELLOW + "/pbr stats [玩家] " + ChatColor.GRAY + "或 " + ChatColor.YELLOW + "/pbrstats [玩家] " + ChatColor.GRAY + "- 查看统计数据");
        player.sendMessage(ChatColor.YELLOW + "/pbr spectate <玩家> " + ChatColor.GRAY + "或 " + ChatColor.YELLOW + "/pbrspectate <玩家> " + ChatColor.GRAY + "- 观战玩家");
        player.sendMessage(ChatColor.YELLOW + "/pbr help " + ChatColor.GRAY + "- 显示此帮助");

        // 显示当前剩余游玩次数
        if (plugin.getConfig().getBoolean("play-limit.enabled", true)) {
            int remaining = plugin.getPlayerDataManager().getRemainingPlays(player);
            if (remaining < 999) {
                player.sendMessage(ChatColor.AQUA + "今日剩余次数: " + ChatColor.WHITE + remaining);
            }
        }

        player.sendMessage(ChatColor.GOLD + "================================");
    }

    /**
     * 处理加入队列指令
     */
    private boolean handleJoin(Player player) {
        // 检查权限
        if (!player.hasPermission("pbr.player.join")) {
            player.sendMessage(getMessage("no-permission"));
            return true;
        }

        // 检查是否已在游戏中
        if (plugin.getGameManager().isInGame(player)) {
            player.sendMessage(getMessage("error.already-in-game"));
            return true;
        }

        // 尝试加入队列
        boolean success = plugin.getQueueManager().joinQueue(player);

        return true;
    }

    /**
     * 处理退出队列指令
     */
    private boolean handleLeave(Player player) {
        // 检查权限
        if (!player.hasPermission("pbr.player.leave")) {
            player.sendMessage(getMessage("no-permission"));
            return true;
        }

        // 尝试退出队列
        plugin.getQueueManager().leaveQueue(player);

        return true;
    }

    /**
     * 处理返回大厅指令
     */
    private boolean handleLobby(Player player) {
        // 检查权限
        if (!player.hasPermission("pbr.player.lobby")) {
            player.sendMessage(getMessage("no-permission"));
            return true;
        }

        // 检查是否在游戏中
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            player.sendMessage(getMessage("error.not-in-game"));
            return true;
        }

        // 检查玩家状态
        GamePlayer gamePlayer = game.getGamePlayer(player.getUniqueId());
        if (gamePlayer == null) {
            return true;
        }

        // 只有已淘汰的玩家才能返回大厅
        if (gamePlayer.isAlive()) {
            player.sendMessage(getMessage("error.cannot-lobby-alive"));
            return true;
        }

        // 传送回大厅
        String worldName = plugin.getConfig().getString("queue.lobby-world", "world");
        org.bukkit.World lobbyWorld = org.bukkit.Bukkit.getWorld(worldName);

        if (lobbyWorld != null) {
            double x = plugin.getConfig().getDouble("queue.lobby-location.x", 0.5);
            double y = plugin.getConfig().getDouble("queue.lobby-location.y", 100.0);
            double z = plugin.getConfig().getDouble("queue.lobby-location.z", 0.5);
            float yaw = (float) plugin.getConfig().getDouble("queue.lobby-location.yaw", 0.0);
            float pitch = (float) plugin.getConfig().getDouble("queue.lobby-location.pitch", 0.0);

            org.bukkit.Location lobby = new org.bukkit.Location(lobbyWorld, x, y, z, yaw, pitch);
            player.teleport(lobby);
            player.sendMessage(getMessage("lobby.teleported"));
        }

        return true;
    }

    /**
     * 处理查看统计指令
     */
    private boolean handleStats(Player player, String[] args) {
        // 检查权限
        if (!player.hasPermission("pbr.player.stats")) {
            player.sendMessage(getMessage("no-permission"));
            return true;
        }

        // 获取目标玩家
        Player target = player;
        if (args.length > 0) {
            target = org.bukkit.Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(getMessage("player-not-found")
                        .replace("{player}", args[0]));
                return true;
            }
        }

        // 获取玩家数据
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(target);

        // 发送统计信息
        player.sendMessage(getMessage("stats.title"));

        if (target == player) {
            player.sendMessage(getMessage("stats.self"));
        } else {
            player.sendMessage(getMessage("stats.other")
                    .replace("{player}", target.getName()));
        }

        // 获取统计信息列表
        FileConfiguration config = plugin.getConfigManager().getMessagesConfig();
        for (String line : config.getStringList("stats.info")) {
            String message = ChatColor.translateAlternateColorCodes('&', line)
                    .replace("{games}", String.valueOf(data.getTotalGames()))
                    .replace("{points}", String.valueOf(data.getTotalPoints()))
                    .replace("{kills}", String.valueOf(data.getTotalKills()))
                    .replace("{wins}", String.valueOf(data.getTotalWins()))
                    .replace("{winrate}", String.format("%.2f", data.getWinRate()))
                    .replace("{avg_kills}", String.format("%.2f", data.getAverageKills()));

            player.sendMessage(message);
        }

        player.sendMessage(getMessage("stats.footer"));

        return true;
    }

    /**
     * 处理观战传送指令
     */
    private boolean handleSpectate(Player player, String[] args) {
        // 检查权限
        if (!player.hasPermission("pbr.player.spectate")) {
            player.sendMessage(getMessage("no-permission"));
            return true;
        }

        // 检查参数
        if (args.length < 1) {
            player.sendMessage(getMessage("command-usage")
                    .replace("{usage}", "/pbrspectate <玩家名>"));
            return true;
        }

        // 检查玩家是否在游戏中
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            player.sendMessage(getMessage("error.not-in-game"));
            return true;
        }

        // 检查玩家是否已淘汰（只有淘汰的玩家才能观战）
        GamePlayer gamePlayer = game.getGamePlayer(player.getUniqueId());
        if (gamePlayer == null || gamePlayer.isAlive()) {
            player.sendMessage(getMessage("error.spectate-alive"));
            return true;
        }

        // 获取目标玩家
        Player target = org.bukkit.Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(getMessage("player-not-found")
                    .replace("{player}", args[0]));
            return true;
        }

        // 检查目标玩家是否在同一游戏中
        Game targetGame = plugin.getGameManager().getPlayerGame(target);
        if (targetGame == null || !targetGame.getGameUuid().equals(game.getGameUuid())) {
            player.sendMessage(getMessage("error.spectate-different-game"));
            return true;
        }

        // 检查目标玩家是否存活
        GamePlayer targetGamePlayer = game.getGamePlayer(target.getUniqueId());
        if (targetGamePlayer == null || !targetGamePlayer.isAlive()) {
            player.sendMessage(getMessage("error.spectate-target-eliminated"));
            return true;
        }

        // 传送到目标玩家
        player.teleport(target.getLocation());
        player.sendMessage(getMessage("spectate.teleported")
                .replace("{player}", target.getName()));

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
