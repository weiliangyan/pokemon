package com.pokemonbr.commands;

import com.pokemonbr.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 邀请指令处理器
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class InviteCommands implements CommandExecutor {

    private final Main plugin;

    public InviteCommands(Main plugin) {
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
            case "pbrinvite":
                return handleInvite(player, args);

            default:
                return false;
        }
    }

    /**
     * 处理邀请指令
     */
    private boolean handleInvite(Player player, String[] args) {
        // 检查权限
        if (!player.hasPermission("pbr.player.invite")) {
            player.sendMessage(getMessage("no-permission"));
            return true;
        }

        // 检查参数
        if (args.length < 1) {
            player.sendMessage(getMessage("command-usage")
                    .replace("{usage}", "/pbrinvite <玩家名>"));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // /pbrinvite <玩家名> - 发送邀请
        if (!subCommand.equals("accept") && !subCommand.equals("decline")) {
            // 获取目标玩家
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage(getMessage("player-not-found")
                        .replace("{player}", args[0]));
                return true;
            }

            // 发送邀请
            plugin.getInviteManager().sendInvite(player, target);
            return true;
        }

        // /pbrinvite accept <玩家名> - 接受邀请
        if (subCommand.equals("accept")) {
            if (args.length < 2) {
                player.sendMessage(getMessage("command-usage")
                        .replace("{usage}", "/pbrinvite accept <玩家名>"));
                return true;
            }

            plugin.getInviteManager().acceptInvite(player, args[1]);
            return true;
        }

        // /pbrinvite decline <玩家名> - 拒绝邀请
        if (subCommand.equals("decline")) {
            if (args.length < 2) {
                player.sendMessage(getMessage("command-usage")
                        .replace("{usage}", "/pbrinvite decline <玩家名>"));
                return true;
            }

            plugin.getInviteManager().declineInvite(player, args[1]);
            return true;
        }

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
