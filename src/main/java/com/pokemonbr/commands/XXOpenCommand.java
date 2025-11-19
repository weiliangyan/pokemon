package com.pokemonbr.commands;

import com.pokemonbr.Main;
import com.pokemonbr.managers.LootGUIManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * /xxopen 快速物品管理GUI指令
 * 支持中文分类名称快速打开
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class XXOpenCommand implements CommandExecutor {

    private final Main plugin;
    private final LootGUIManager guiManager;

    // 中文名称映射到内部分类ID
    private static final Map<String, String> CATEGORY_MAPPING = new HashMap<>();

    static {
        CATEGORY_MAPPING.put("优品", "youpin");
        CATEGORY_MAPPING.put("极品", "jipin");
        CATEGORY_MAPPING.put("普通", "putong");
    }

    public XXOpenCommand(Main plugin, LootGUIManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 权限检查
        if (!sender.hasPermission("pbr.admin.lootgui")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此指令");
            return true;
        }

        // 必须是玩家
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此指令只能由玩家执行");
            return true;
        }

        // 参数检查
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        Player player = (Player) sender;
        String categoryName = args[0];

        // 查找映射
        String categoryId = CATEGORY_MAPPING.get(categoryName);

        if (categoryId == null) {
            // 尝试直接使用英文ID
            if (CATEGORY_MAPPING.containsValue(categoryName)) {
                categoryId = categoryName;
            } else {
                sender.sendMessage(ChatColor.RED + "未知的物品分类: " + categoryName);
                sendHelp(sender);
                return true;
            }
        }

        // 打开GUI
        guiManager.openGUI(player, categoryId);
        return true;
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== 快速物品管理 ==========");
        sender.sendMessage(ChatColor.YELLOW + "/xxopen 优品 " + ChatColor.GRAY + "- 打开优品物品管理GUI");
        sender.sendMessage(ChatColor.YELLOW + "/xxopen 极品 " + ChatColor.GRAY + "- 打开极品物品管理GUI");
        sender.sendMessage(ChatColor.YELLOW + "/xxopen 普通 " + ChatColor.GRAY + "- 打开普通物品管理GUI");
        sender.sendMessage(ChatColor.GOLD + "================================");
        sender.sendMessage(ChatColor.GRAY + "提示: 也支持英文名称 (youpin, jipin, putong)");
        sender.sendMessage(ChatColor.AQUA + "放入物品后关闭GUI自动保存到配置文件");
    }
}
