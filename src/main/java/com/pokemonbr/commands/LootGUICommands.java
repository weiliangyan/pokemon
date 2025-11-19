package com.pokemonbr.commands;

import com.pokemonbr.Main;
import com.pokemonbr.managers.LootGUIManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 物品管理GUI指令处理器
 * 指令: /lootgui <open|clear|list|reload> [分类]
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class LootGUICommands implements CommandExecutor {

    private final Main plugin;
    private final LootGUIManager guiManager;

    public LootGUICommands(Main plugin, LootGUIManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
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

            case "clear":
                return handleClear(sender, args);

            case "list":
                return handleList(sender);

            case "reload":
                return handleReload(sender);

            default:
                sendHelp(sender);
                return true;
        }
    }

    /**
     * 处理 open 子指令
     * 用法: /lootgui open <分类>
     */
    private boolean handleOpen(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此指令只能由玩家执行");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /lootgui open <分类>");
            sender.sendMessage(ChatColor.GRAY + "可用分类: 普通, 优品, 极品");
            return true;
        }

        Player player = (Player) sender;
        String category = args[1]; // 保持原始大小写，支持中文名称

        guiManager.openGUI(player, category);
        return true;
    }

    /**
     * 处理 clear 子指令
     * 用法: /lootgui clear <分类>
     */
    private boolean handleClear(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此指令只能由玩家执行");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /lootgui clear <分类>");
            sender.sendMessage(ChatColor.GRAY + "可用分类: 普通, 优品, 极品");
            return true;
        }

        Player player = (Player) sender;
        String category = args[1]; // 保持原始大小写，支持中文名称

        guiManager.clearCategory(player, category);
        return true;
    }

    /**
     * 处理 list 子指令
     * 用法: /lootgui list
     */
    private boolean handleList(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== 物品管理分类 ==========");

        for (String category : guiManager.getCategories()) {
            int itemCount = guiManager.getItems(category).size();
            sender.sendMessage(ChatColor.YELLOW + "▸ " + ChatColor.AQUA + category +
                    ChatColor.GRAY + " (物品数: " + itemCount + ")");
        }

        sender.sendMessage(ChatColor.GOLD + "================================");
        sender.sendMessage(ChatColor.GRAY + "使用 /lootgui open <分类> 打开GUI");
        return true;
    }

    /**
     * 处理 reload 子指令
     * 用法: /lootgui reload
     */
    private boolean handleReload(CommandSender sender) {
        guiManager.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "✓ 物品管理GUI配置已重载");
        return true;
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== 物品管理GUI指令 ==========");
        sender.sendMessage(ChatColor.YELLOW + "/lootgui open <分类> " + ChatColor.GRAY + "- 打开物品管理GUI");
        sender.sendMessage(ChatColor.AQUA + "  示例: /lootgui open 优品");
        sender.sendMessage(ChatColor.YELLOW + "/lootgui clear <分类> " + ChatColor.GRAY + "- 清空指定分类物品");
        sender.sendMessage(ChatColor.YELLOW + "/lootgui list " + ChatColor.GRAY + "- 查看所有分类");
        sender.sendMessage(ChatColor.YELLOW + "/lootgui reload " + ChatColor.GRAY + "- 重载配置");
        sender.sendMessage(ChatColor.GOLD + "==================================");
        sender.sendMessage(ChatColor.GRAY + "可用分类: " + ChatColor.AQUA + "普通, 优品, 极品");
        sender.sendMessage(ChatColor.GRAY + "支持保存完整NBT数据 (包括Pixelmon物品)");
    }
}
