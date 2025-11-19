package com.pokemonbr.managers;

import com.pokemonbr.Main;
import me.lucko.luckperms.api.LuckPermsProvider;
import me.lucko.luckperms.api.cacheddata.CachedPermissionData;
import me.lucko.luckperms.api.context.ContextManager;
import me.lucko.luckperms.api.model.user.User;
import me.lucko.luckperms.api.node.Node;
import me.lucko.luckperms.api.node.types.InheritanceNode;
import me.lucko.luckperms.api.query.QueryOptions;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * 权限管理器
 * 统一管理 LuckPerms 和基础权限系统
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class PermissionManager {

    private final Main plugin;
    private boolean luckPermsEnabled = false;
    private LuckPerms luckPerms = null;

    public PermissionManager(Main plugin) {
        this.plugin = plugin;
        initializeLuckPerms();
    }

    /**
     * 初始化 LuckPerms
     */
    private void initializeLuckPerms() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                // 尝试获取 LuckPerms API
                Optional<LuckPermsProvider> provider = LuckPermsProvider.getProvider();
                if (provider.isPresent()) {
                    luckPerms = provider.get();
                    luckPermsEnabled = true;
                    plugin.getLogger().info("§a✓ LuckPerms 权限系统集成成功");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("§cLuckPerms API 加载失败: " + e.getMessage());
                luckPermsEnabled = false;
            }
        } else {
            plugin.getLogger().info("§7未检测到 LuckPerms，将使用基础权限系统");
        }
    }

    /**
     * 检查玩家是否有指定权限
     * @param player 玩家
     * @param permission 权限节点
     * @return 是否有权限
     */
    public boolean hasPermission(Player player, String permission) {
        if (player == null) return false;

        // 优先使用 LuckPerms
        if (luckPermsEnabled && luckPerms != null) {
            return hasLuckPermsPermission(player, permission);
        }

        // 回退到 Bukkit 权限系统
        return player.hasPermission(permission);
    }

    /**
     * 使用 LuckPerms 检查权限
     */
    private boolean hasLuckPermsPermission(Player player, String permission) {
        try {
            UUID uuid = player.getUniqueId();
            User user = luckPerms.getUserManager().getUser(uuid);

            if (user == null) {
                return false;
            }

            ContextManager contextManager = luckPerms.getContextManager();
            QueryOptions queryOptions = contextManager.getQueryOptions(user);

            CachedPermissionData permissionData = user.getCachedData().getPermissionData(queryOptions);
            return permissionData.checkPermission(permission).asBoolean();

        } catch (Exception e) {
            plugin.getLogger().warning("§cLuckPerms 权限检查失败: " + e.getMessage());
            return player.hasPermission(permission); // 回退到 Bukkit 权限
        }
    }

    /**
     * 检查玩家是否属于指定权限组
     * @param player 玩家
     * @param group 组名
     * @return 是否属于该组
     */
    public boolean isInGroup(Player player, String group) {
        if (player == null) return false;

        // 优先使用 LuckPerms
        if (luckPermsEnabled && luckPerms != null) {
            return isInLuckPermsGroup(player, group);
        }

        // 回退到 Vault
        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            return isInVaultGroup(player, group);
        }

        return false;
    }

    /**
     * 使用 LuckPerms 检查组
     */
    private boolean isInLuckPermsGroup(Player player, String group) {
        try {
            UUID uuid = player.getUniqueId();
            User user = luckPerms.getUserManager().getUser(uuid);

            if (user == null) {
                return false;
            }

            ContextManager contextManager = luckPerms.getContextManager();
            QueryOptions queryOptions = contextManager.getQueryOptions(user);

            // 检查用户是否有该组的继承节点
            return user.getNodes(queryOptions).stream()
                    .filter(node -> node instanceof InheritanceNode)
                    .map(node -> (InheritanceNode) node)
                    .anyMatch(node -> node.getGroupName().equalsIgnoreCase(group));

        } catch (Exception e) {
            plugin.getLogger().warning("§cLuckPerms 组检查失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 使用 Vault 检查组
     */
    private boolean isInVaultGroup(Player player, String group) {
        try {
            net.milkbowl.vault.permission.Permission perms =
                plugin.getServer().getServicesManager()
                    .getRegistration(net.milkbowl.vault.permission.Permission.class)
                    .getProvider();

            if (perms != null) {
                return perms.playerInGroup(player, group);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("§cVault 组检查失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 检查玩家是否为管理员
     */
    public boolean isAdmin(Player player) {
        // 检查 OP
        if (player.isOp()) {
            return true;
        }

        // 检查管理员权限
        return hasPermission(player, "pbr.admin");
    }

    /**
     * 检查玩家是否为VIP
     */
    public boolean isVip(Player player) {
        return isInGroup(player, "vip") || isInGroup(player, "premium") ||
               hasPermission(player, "pbr.vip");
    }

    /**
     * 获取玩家的主要组名
     */
    public String getPlayerGroup(Player player) {
        if (player == null) return "default";

        // 优先使用 LuckPerms
        if (luckPermsEnabled && luckPerms != null) {
            return getLuckPermsPrimaryGroup(player);
        }

        // 回退到 Vault
        return getVaultPrimaryGroup(player);
    }

    /**
     * 获取 LuckPerms 主组
     */
    private String getLuckPermsPrimaryGroup(Player player) {
        try {
            UUID uuid = player.getUniqueId();
            User user = luckPerms.getUserManager().getUser(uuid);

            if (user == null) {
                return "default";
            }

            ContextManager contextManager = luckPerms.getContextManager();
            QueryOptions queryOptions = contextManager.getQueryOptions(user);

            // 获取主要继承节点
            Optional<InheritanceNode> primaryGroup = user.getNodes(queryOptions).stream()
                    .filter(node -> node instanceof InheritanceNode)
                    .map(node -> (InheritanceNode) node)
                    .filter(InheritanceNode::isPrimary)
                    .findFirst();

            return primaryGroup.map(InheritanceNode::getGroupName).orElse("default");

        } catch (Exception e) {
            plugin.getLogger().warning("§c获取 LuckPerms 主组失败: " + e.getMessage());
            return "default";
        }
    }

    /**
     * 获取 Vault 主组
     */
    private String getVaultPrimaryGroup(Player player) {
        try {
            net.milkbowl.vault.permission.Permission perms =
                plugin.getServer().getServicesManager()
                    .getRegistration(net.milkbowl.vault.permission.Permission.class)
                    .getProvider();

            if (perms != null) {
                return perms.getPrimaryGroup(player);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("§c获取 Vault 主组失败: " + e.getMessage());
        }
        return "default";
    }

    /**
     * 获取玩家前缀
     */
    public String getPlayerPrefix(Player player) {
        String group = getPlayerGroup();

        // 基础前缀映射
        switch (group.toLowerCase()) {
            case "admin":
                return "&4[管理员]&r ";
            case "vip":
            case "premium":
                return "&6[VIP]&r ";
            case "moderator":
                return "&2[MOD]&r ";
            default:
                return "&7[玩家]&r ";
        }
    }

    /**
     * 是否启用了 LuckPerms
     */
    public boolean isLuckPermsEnabled() {
        return luckPermsEnabled;
    }
}