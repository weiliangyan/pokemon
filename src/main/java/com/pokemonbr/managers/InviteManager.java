package com.pokemonbr.managers;

import com.pokemonbr.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邀请管理器
 * 管理玩家之间的组队邀请
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class InviteManager {

    private final Main plugin;

    // 邀请记录: 被邀请者UUID -> 邀请者UUID
    private final Map<UUID, UUID> invites;

    // 邀请过期时间映射
    private final Map<UUID, Long> inviteExpireTime;

    // 邀请过期时间（毫秒）
    private static final long INVITE_EXPIRE_TIME = 60000; // 60秒

    public InviteManager(Main plugin) {
        this.plugin = plugin;
        this.invites = new ConcurrentHashMap<>();
        this.inviteExpireTime = new ConcurrentHashMap<>();

        // 启动邀请过期检查任务
        startExpireCheckTask();
    }

    /**
     * 发送邀请
     * @param inviter 邀请者
     * @param invitee 被邀请者
     * @return 是否成功
     */
    public boolean sendInvite(Player inviter, Player invitee) {
        // 检查是否邀请自己
        if (inviter.getUniqueId().equals(invitee.getUniqueId())) {
            inviter.sendMessage(getMessage("invite.cannot-invite-self"));
            return false;
        }

        // 检查邀请者是否在游戏中
        if (plugin.getGameManager().isInGame(inviter)) {
            inviter.sendMessage(getMessage("error.already-in-game"));
            return false;
        }

        // 检查被邀请者是否在游戏中
        if (plugin.getGameManager().isInGame(invitee)) {
            inviter.sendMessage(getMessage("invite.already-in-game")
                    .replace("{player}", invitee.getName()));
            return false;
        }

        // 检查邀请者是否在队列中
        if (plugin.getQueueManager().getPlayerQueue(inviter) != null) {
            inviter.sendMessage(getMessage("queue.already-in-queue"));
            return false;
        }

        // 检查被邀请者是否在队列中
        if (plugin.getQueueManager().getPlayerQueue(invitee) != null) {
            inviter.sendMessage(ChatColor.RED + invitee.getName() + " 已经在匹配队列中！");
            return false;
        }

        // 检查被邀请者是否已有待处理的邀请
        if (invites.containsKey(invitee.getUniqueId())) {
            inviter.sendMessage(ChatColor.RED + invitee.getName() + " 已有待处理的邀请！");
            return false;
        }

        // 记录邀请
        invites.put(invitee.getUniqueId(), inviter.getUniqueId());
        inviteExpireTime.put(invitee.getUniqueId(), System.currentTimeMillis() + INVITE_EXPIRE_TIME);

        // 发送消息
        inviter.sendMessage(getMessage("invite.sent")
                .replace("{player}", invitee.getName()));

        invitee.sendMessage(getMessage("invite.received")
                .replace("{player}", inviter.getName()));
        invitee.sendMessage(getMessage("invite.received-tip")
                .replace("{player}", inviter.getName()));

        return true;
    }

    /**
     * 接受邀请
     * @param invitee 被邀请者
     * @param inviterName 邀请者名称
     * @return 是否成功
     */
    public boolean acceptInvite(Player invitee, String inviterName) {
        UUID inviterUUID = invites.get(invitee.getUniqueId());

        if (inviterUUID == null) {
            invitee.sendMessage(getMessage("invite.no-invite")
                    .replace("{player}", inviterName));
            return false;
        }

        Player inviter = Bukkit.getPlayer(inviterUUID);

        // 检查邀请者是否在线
        if (inviter == null || !inviter.isOnline()) {
            invites.remove(invitee.getUniqueId());
            inviteExpireTime.remove(invitee.getUniqueId());
            invitee.sendMessage(ChatColor.RED + "邀请者已离线！");
            return false;
        }

        // 检查邀请者是否在游戏或队列中
        if (plugin.getGameManager().isInGame(inviter)) {
            invites.remove(invitee.getUniqueId());
            inviteExpireTime.remove(invitee.getUniqueId());
            invitee.sendMessage(getMessage("invite.already-in-game")
                    .replace("{player}", inviter.getName()));
            return false;
        }

        if (plugin.getQueueManager().getPlayerQueue(inviter) != null) {
            invites.remove(invitee.getUniqueId());
            inviteExpireTime.remove(invitee.getUniqueId());
            invitee.sendMessage(ChatColor.RED + inviter.getName() + " 已经在匹配队列中！");
            return false;
        }

        // 移除邀请记录
        invites.remove(invitee.getUniqueId());
        inviteExpireTime.remove(invitee.getUniqueId());

        // 发送消息
        invitee.sendMessage(getMessage("invite.accepted")
                .replace("{player}", inviter.getName()));
        inviter.sendMessage(getMessage("invite.accepted-sender")
                .replace("{player}", invitee.getName()));

        // 双方加入队列
        plugin.getQueueManager().joinQueue(inviter);
        plugin.getQueueManager().joinQueue(invitee);

        return true;
    }

    /**
     * 拒绝邀请
     * @param invitee 被邀请者
     * @param inviterName 邀请者名称
     * @return 是否成功
     */
    public boolean declineInvite(Player invitee, String inviterName) {
        UUID inviterUUID = invites.get(invitee.getUniqueId());

        if (inviterUUID == null) {
            invitee.sendMessage(getMessage("invite.no-invite")
                    .replace("{player}", inviterName));
            return false;
        }

        Player inviter = Bukkit.getPlayer(inviterUUID);

        // 移除邀请记录
        invites.remove(invitee.getUniqueId());
        inviteExpireTime.remove(invitee.getUniqueId());

        // 发送消息
        invitee.sendMessage(getMessage("invite.declined")
                .replace("{player}", inviterName));

        if (inviter != null && inviter.isOnline()) {
            inviter.sendMessage(getMessage("invite.declined-sender")
                    .replace("{player}", invitee.getName()));
        }

        return true;
    }

    /**
     * 检查邀请是否存在
     * @param invitee 被邀请者
     * @return 是否存在
     */
    public boolean hasInvite(Player invitee) {
        return invites.containsKey(invitee.getUniqueId());
    }

    /**
     * 获取邀请者
     * @param invitee 被邀请者
     * @return 邀请者UUID
     */
    public UUID getInviter(Player invitee) {
        return invites.get(invitee.getUniqueId());
    }

    /**
     * 清除玩家的邀请记录
     * @param player 玩家
     */
    public void clearInvites(Player player) {
        invites.remove(player.getUniqueId());
        inviteExpireTime.remove(player.getUniqueId());

        // 同时清除该玩家发出的邀请
        invites.entrySet().removeIf(entry -> entry.getValue().equals(player.getUniqueId()));
    }

    /**
     * 启动邀请过期检查任务
     */
    private void startExpireCheckTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();

            // 检查过期的邀请
            List<UUID> expiredInvites = new ArrayList<>();

            for (Map.Entry<UUID, Long> entry : inviteExpireTime.entrySet()) {
                if (now >= entry.getValue()) {
                    expiredInvites.add(entry.getKey());
                }
            }

            // 移除过期的邀请
            for (UUID inviteeUUID : expiredInvites) {
                UUID inviterUUID = invites.get(inviteeUUID);

                Player invitee = Bukkit.getPlayer(inviteeUUID);
                Player inviter = Bukkit.getPlayer(inviterUUID);

                if (invitee != null && invitee.isOnline()) {
                    String inviterName = inviter != null ? inviter.getName() : "未知玩家";
                    invitee.sendMessage(getMessage("invite.expired")
                            .replace("{player}", inviterName));
                }

                invites.remove(inviteeUUID);
                inviteExpireTime.remove(inviteeUUID);
            }

        }, 20L, 20L); // 每秒检查一次
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
