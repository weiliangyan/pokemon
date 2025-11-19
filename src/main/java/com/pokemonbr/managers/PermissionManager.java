package com.pokemonbr.managers;

import com.pokemonbr.Main;
import org.bukkit.entity.Player;

/**
 * 权限管理器
 * 简化版本，仅使用Bukkit基本权限系统
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class PermissionManager {

    private final Main plugin;

    public PermissionManager(Main plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("§a权限管理器已初始化（使用Bukkit基础权限系统）");
    }

    /**
     * 检查玩家是否有指定权限
     * @param player 玩家
     * @param permission 权限节点
     * @return 是否有权限
     */
    public boolean hasPermission(Player player, String permission) {
        if (player == null) return false;
        return player.hasPermission(permission);
    }

    /**
     * 获取玩家主要权限组（简化版本）
     * @param player 玩家
     * @return 权限组名称
     */
    public String getPlayerGroup(Player player) {
        if (player == null) return "default";

        // 简化版本：根据OP状态判断
        if (player.isOp()) {
            return "admin";
        }

        return "default";
    }

    /**
     * 给玩家添加权限组（简化版本）
     * @param player 玩家
     * @param groupName 权限组名
     */
    public void addPlayerToGroup(Player player, String groupName) {
        // 简化版本：仅记录日志，实际权限管理需要权限插件
        plugin.getLogger().info("§e[权限] 玩家 " + player.getName() + " 被添加到组: " + groupName);
        plugin.getLogger().warning("§c注意: 当前使用简化权限系统，实际权限设置需要安装权限插件");
    }

    /**
     * 从玩家权限组中移除（简化版本）
     * @param player 玩家
     * @param groupName 权限组名
     */
    public void removePlayerFromGroup(Player player, String groupName) {
        // 简化版本：仅记录日志
        plugin.getLogger().info("§e[权限] 玩家 " + player.getName() + " 被从组 " + groupName + " 中移除");
        plugin.getLogger().warning("§c注意: 当前使用简化权限系统，实际权限设置需要安装权限插件");
    }
}