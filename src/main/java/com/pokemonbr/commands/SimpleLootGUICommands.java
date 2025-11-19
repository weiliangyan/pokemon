package com.pokemonbr.commands;

import com.pokemonbr.Main;
import com.pokemonbr.managers.LootGUIManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 简化的物品管理指令
 * 支持GUI管理和TOML配置
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class SimpleLootGUICommands implements CommandExecutor {

    private final Main plugin;
    private final LootGUIManager lootGUIManager;

    public SimpleLootGUICommands(Main plugin, LootGUIManager lootGUIManager) {
        this.plugin = plugin;
        this.lootGUIManager = lootGUIManager;
    }

    @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 检查权限
        if (!sender.hasPermission("pbr.admin.lootgui")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此指令");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "open":
                return handleOpen(sender, args);
            case "list":
                return handleList(sender);
            case "reload":
                return handleReload(sender);
            case "tianchonxz":
                return handleTianchonxz(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    /**
     * 处理打开GUI指令
     */
    private boolean handleOpen(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此指令只能由玩家执行");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /lootgui open <分类>");
            sender.sendMessage(ChatColor.GRAY + "可用分类: jichu, youpin, xiyou, 或通过GUI自动识别");
            return true;
        }

        Player player = (Player) sender;
        String category = args[1].toLowerCase();

        lootGUIManager.openGUI(player, category);
        return true;
    }

    /**
     * 处理列表指令
     */
    private boolean handleList(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== 可用品类 ==========");

        // 获取所有可用分类
        for (String category : lootGUIManager.getCategories()) {
            int count = lootGUIManager.getItems(category).size();
            String status = count > 0 ? ChatColor.GREEN + (count + " 个物品") : ChatColor.GRAY + "0 个物品";
            sender.sendMessage(ChatColor.YELLOW + "• " + ChatColor.WHITE + category + ": " + status);
        }

        if (lootGUIManager.getCategories().isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "没有配置的品类，使用默认分类");
        }

        sender.sendMessage(ChatColor.GOLD + "================================");
        return true;
    }

    /**
     * 处理重载配置指令
     */
    private boolean handleReload(CommandSender sender) {
        lootGUIManager.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "✓ 配置已重载");

        // 重载自定义品类配置
        if (plugin.getSimpleCustomCategoryManager() != null) {
            plugin.getSimpleCustomCategoryManager().reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "✓ TOML配置已重载");
        }

        return true;
    }

    /**
     * 处理天充箱子信息指令
     */
    private boolean handleTianchonxz(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== 天充箱子系统 ==========");

        if (plugin.getSimpleCustomCategoryManager() != null) {
            com.pokemonbr.managers.SimpleCustomCategoryManager categoryManager = plugin.getSimpleCustomCategoryManager();
            sender.sendMessage(ChatColor.YELLOW + "配置文件: tianchonxz.toml");
            sender.sendMessage(ChatColor.YELLOW + "默认品类: jichu, youpin, xiyou");

            sender.sendMessage(ChatColor.AQUA + "当前配置的品类:");
            for (String category : categoryManager.getCategoryNames()) {
                int count = categoryManager.getItemCount(category);
                sender.sendMessage(ChatColor.WHITE + "  • " + category + ": " + ChatColor.GREEN + count + " 个物品");
            }

            if (categoryManager.getCategoryNames().isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "当前没有保存任何物品到配置文件");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "天充箱子系统未初始化");
        }

        sender.sendMessage(ChatColor.GOLD + "================================");
        return true;
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== 物品管理GUI指令 ==========");
        sender.sendMessage(ChatColor.YELLOW + "/lootgui open <分类> " + ChatColor.GRAY + "- 打开物品管理界面");
        sender.sendMessage(ChatColor.GRAY + "  可用分类: jichu, youpin, xiyou");
        sender.sendMessage(ChatColor.GRAY + "  或者使用GUI自动识别功能");
        sender.sendMessage(ChatColor.YELLOW + "/lootgui list " + ChatColor.GRAY + "- 查看所有品类及物品数量");
        sender.sendMessage(ChatColor.YELLOW + "/lootgui reload " + ChatColor.GRAY + "- 重载配置文件");
        sender.sendMessage(ChatColor.YELLOW + "/lootgui tianchonxz " + ChatColor.GRAY + "- 查看天充箱子系统信息");
        sender.sendMessage(ChatColor.GOLD + "======================================");
        sender.sendMessage(ChatColor.GRAY + "提示: 将物品放入GUI后会自动保存到配置文件");
        sender.sendMessage(ChatColor.GRAY + "      配置文件: loot-gui.yml (YAML) 和 tianchonxz.toml (TOML)");
    }
}