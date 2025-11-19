package com.pokemonbr.listeners;

import com.pokemonbr.Main;
import com.pokemonbr.models.Game;
import com.pokemonbr.models.GamePlayer;
import com.pokemonbr.models.GameState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * Pixelmon战斗事件监听器
 *
 * 注意: 此类使用反射调用Pixelmon API，以兼容不同版本
 * 你需要根据实际使用的Pixelmon版本调整事件名称
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class PixelmonBattleListener implements Listener {

    private final Main plugin;

    public PixelmonBattleListener(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * 监听战斗结束事件
     *
     * Pixelmon事件路径参考:
     * - com.pixelmonmod.pixelmon.api.events.battles.BattleEndEvent (Pixelmon 9.x)
     * - com.pixelmonmod.pixelmon.battles.api.event.BattleEndedEvent (旧版本)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBattleEnd(Object event) {
        try {
            // 使用反射获取战斗结果
            Class<?> eventClass = event.getClass();

            // 获取战败方
            Object loser = eventClass.getMethod("getLoser").invoke(event);
            if (loser == null) {
                return;
            }

            // 获取战败玩家UUID
            UUID loserUUID = getPlayerUUIDFromBattleParticipant(loser);
            if (loserUUID == null) {
                return;
            }

            Player loserPlayer = Bukkit.getPlayer(loserUUID);
            if (loserPlayer == null || !loserPlayer.isOnline()) {
                return;
            }

            // 检查玩家是否在游戏中
            Game game = plugin.getGameManager().getPlayerGame(loserPlayer);
            if (game == null) {
                return;
            }

            // 检查游戏状态
            if (game.getState() != GameState.PLAYING && game.getState() != GameState.FINAL_STAGE) {
                return;
            }

            // 获取胜利方（用于记录击败者）
            Object winner = eventClass.getMethod("getWinner").invoke(event);
            UUID winnerUUID = winner != null ? getPlayerUUIDFromBattleParticipant(winner) : null;

            // 判断是否应该淘汰
            boolean shouldEliminate = shouldEliminateOnDefeat(game);

            if (shouldEliminate) {
                // 淘汰玩家
                handlePlayerElimination(game, loserUUID, winnerUUID, "战斗失败");

                // 如果是最终阶段，治疗胜利者的宝可梦
                if (game.getState() == GameState.FINAL_STAGE && winnerUUID != null) {
                    healWinnerPokemon(winnerUUID);
                }
            } else {
                // 仅发送消息
                loserPlayer.sendMessage(getMessage("battle.defeat"));
            }

        } catch (Exception e) {
            plugin.getLogger().warning("§c处理Pixelmon战斗事件时出错: " + e.getMessage());
            // 不打印完整堆栈，避免日志刷屏
        }
    }

    /**
     * 监听战斗逃跑事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBattleFlee(Object event) {
        try {
            Class<?> eventClass = event.getClass();
            Object fleer = eventClass.getMethod("getFleer").invoke(event);

            if (fleer == null) {
                return;
            }

            UUID fleerUUID = getPlayerUUIDFromBattleParticipant(fleer);
            Player fleerPlayer = Bukkit.getPlayer(fleerUUID);

            if (fleerPlayer == null) {
                return;
            }

            Game game = plugin.getGameManager().getPlayerGame(fleerPlayer);
            if (game == null) {
                return;
            }

            boolean shouldEliminate = shouldEliminateOnFlee(game);

            if (shouldEliminate) {
                handlePlayerElimination(game, fleerUUID, null, "逃跑");
            } else {
                fleerPlayer.sendMessage(getMessage("battle.fled"));
            }

        } catch (Exception e) {
            plugin.getLogger().warning("§c处理逃跑事件时出错: " + e.getMessage());
        }
    }

    /**
     * 监听战斗强制结束事件 (endbattle指令)
     *
     * 注意：这可能需要监听Pixelmon的特定事件或指令
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBattleForceEnd(Object event) {
        try {
            // 尝试获取战斗参与者
            Class<?> eventClass = event.getClass();

            // 可能的方法名：getPlayers, getParticipants, getBattle
            Object battle = null;
            try {
                battle = eventClass.getMethod("getBattle").invoke(event);
            } catch (Exception e) {
                // 如果没有getBattle方法，尝试直接获取玩家
            }

            // 检查是否是强制结束
            Boolean forced = false;
            try {
                forced = (Boolean) eventClass.getMethod("isForced").invoke(event);
            } catch (Exception e) {
                // 可能没有isForced方法
            }

            if (!forced) {
                return;
            }

            // 获取战斗中的玩家
            // 这部分需要根据实际Pixelmon API调整

        } catch (Exception e) {
            plugin.getLogger().warning("§c处理强制结束战斗事件时出错: " + e.getMessage());
        }
    }

    /**
     * 监听拒绝对战事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBattleDecline(Object event) {
        try {
            Class<?> eventClass = event.getClass();

            // 获取拒绝对战的玩家
            Object decliner = eventClass.getMethod("getPlayer").invoke(event);
            if (decliner == null) {
                return;
            }

            UUID declinerUUID = null;
            if (decliner instanceof Player) {
                declinerUUID = ((Player) decliner).getUniqueId();
            }

            if (declinerUUID == null) {
                return;
            }

            Player declinerPlayer = Bukkit.getPlayer(declinerUUID);
            if (declinerPlayer == null) {
                return;
            }

            Game game = plugin.getGameManager().getPlayerGame(declinerPlayer);
            if (game == null) {
                return;
            }

            boolean shouldEliminate = shouldEliminateOnDecline(game);

            if (shouldEliminate) {
                handlePlayerElimination(game, declinerUUID, null, "拒绝对战");
            } else {
                declinerPlayer.sendMessage(getMessage("battle.declined"));
            }

        } catch (Exception e) {
            plugin.getLogger().warning("§c处理拒绝对战事件时出错: " + e.getMessage());
        }
    }

    /**
     * 检查玩家是否还有宝可梦
     * 这个方法需要定时执行或在特定事件触发
     */
    public void checkPlayerPokemonCount(Player player) {
        try {
            Game game = plugin.getGameManager().getPlayerGame(player);
            if (game == null) {
                return;
            }

            // 使用反射检查玩家的宝可梦数量
            // 具体实现需要根据Pixelmon API调整
            int pokemonCount = getPokemonCount(player);

            if (pokemonCount == 0) {
                boolean shouldEliminate = shouldEliminateOnNoPokemon(game);

                if (shouldEliminate) {
                    handlePlayerElimination(game, player.getUniqueId(), null, "背包无宝可梦");
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("§c检查宝可梦数量时出错: " + e.getMessage());
        }
    }

    /**
     * 获取玩家的宝可梦数量（使用反射）
     */
    private int getPokemonCount(Player player) {
        try {
            // 使用Pixelmon API获取玩家的宝可梦存储
            // 示例代码（需要根据实际API调整）:
            // PlayerPartyStorage storage = Pixelmon.storageManager.getParty(player.getUniqueId());
            // return storage.countPokemon();

            // 使用反射实现
            Class<?> pixelmonClass = Class.forName("com.pixelmonmod.pixelmon.Pixelmon");
            Object storageManager = pixelmonClass.getField("storageManager").get(null);

            Object partyStorage = storageManager.getClass()
                    .getMethod("getParty", java.util.UUID.class)
                    .invoke(storageManager, player.getUniqueId());

            if (partyStorage != null) {
                Integer count = (Integer) partyStorage.getClass()
                        .getMethod("countPokemon")
                        .invoke(partyStorage);
                return count != null ? count : 0;
            }

        } catch (Exception e) {
            plugin.getLogger().warning("§c无法获取玩家宝可梦数量: " + e.getMessage());
        }

        return 1; // 默认返回1，避免误判
    }

    /**
     * 从战斗参与者对象中获取玩家UUID
     */
    private UUID getPlayerUUIDFromBattleParticipant(Object participant) {
        try {
            // 尝试获取玩家对象
            Object playerObject = participant.getClass().getMethod("getPlayer").invoke(participant);

            if (playerObject instanceof Player) {
                return ((Player) playerObject).getUniqueId();
            }

            // 尝试直接获取UUID
            Object uuid = participant.getClass().getMethod("getUUID").invoke(participant);
            if (uuid instanceof UUID) {
                return (UUID) uuid;
            }

        } catch (Exception e) {
            // 忽略反射异常
        }

        return null;
    }

    /**
     * 处理玩家淘汰
     */
    private void handlePlayerElimination(Game game, UUID victim, UUID killer, String reason) {
        Player victimPlayer = Bukkit.getPlayer(victim);

        // 淘汰玩家
        game.eliminatePlayer(victim, killer);

        // 广播淘汰消息
        String victimName = victimPlayer != null ? victimPlayer.getName() : "未知玩家";

        if (killer != null) {
            Player killerPlayer = Bukkit.getPlayer(killer);
            String killerName = killerPlayer != null ? killerPlayer.getName() : "未知玩家";

            game.broadcastMessage(getMessage("elimination.broadcast")
                    .replace("{player}", victimName)
                    .replace("{alive}", String.valueOf(game.getAlivePlayerCount())));

            // 发送击败消息给击败者
            if (killerPlayer != null && killerPlayer.isOnline()) {
                GamePlayer gamePlayer = game.getGamePlayer(killer);
                killerPlayer.sendMessage(getMessage("elimination.killed-player")
                        .replace("{player}", victimName));
                killerPlayer.sendMessage(getMessage("elimination.kill-count")
                        .replace("{kills}", String.valueOf(gamePlayer.getKills())));
            }
        } else {
            game.broadcastMessage(getMessage("elimination.broadcast")
                    .replace("{player}", victimName + " (" + reason + ")")
                    .replace("{alive}", String.valueOf(game.getAlivePlayerCount())));
        }

        // 发送淘汰消息给被淘汰者
        if (victimPlayer != null && victimPlayer.isOnline()) {
            if (killer != null) {
                Player killerPlayer = Bukkit.getPlayer(killer);
                String killerName = killerPlayer != null ? killerPlayer.getName() : "未知玩家";
                victimPlayer.sendMessage(getMessage("elimination.eliminated-by")
                        .replace("{player}", killerName));
            } else {
                victimPlayer.sendMessage(getMessage("elimination.eliminated"));
            }

            // 设置为观战模式
            victimPlayer.setGameMode(org.bukkit.GameMode.SPECTATOR);
            victimPlayer.sendMessage(getMessage("elimination.spectator-mode"));
            victimPlayer.sendMessage(getMessage("elimination.spectator-tip"));

            // 启动返回大厅倒计时
            startReturnCountdown(victimPlayer, game);
        }
    }

    /**
     * 启动返回大厅倒计时
     */
    private void startReturnCountdown(Player player, Game game) {
        int countdown = plugin.getConfig().getInt("elimination.return-countdown", 10);

        // 创建倒计时任务
        final int[] remaining = {countdown};

        org.bukkit.scheduler.BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            // 检查玩家是否还在游戏中（可能已经手动离开）
            if (plugin.getGameManager().getPlayerGame(player) == null) {
                return;
            }

            if (remaining[0] > 0) {
                // 发送倒计时消息
                String action = getMessage("elimination.to-lobby");
                player.sendMessage(getMessage("elimination.countdown")
                        .replace("{time}", String.valueOf(remaining[0]))
                        .replace("{action}", action));

                remaining[0]--;
            } else {
                // 倒计时结束，传送回大厅
                returnPlayerToLobby(player);
            }
        }, 0L, 20L); // 每秒执行一次

        // countdown秒后自动取消任务
        Bukkit.getScheduler().runTaskLater(plugin, task::cancel, (countdown + 1) * 20L);
    }

    /**
     * 将玩家传送回大厅
     */
    private void returnPlayerToLobby(Player player) {
        String worldName = plugin.getConfig().getString("queue.lobby-world", "world");
        org.bukkit.World lobbyWorld = Bukkit.getWorld(worldName);

        if (lobbyWorld == null) {
            player.sendMessage(getMessage("error.world-not-found")
                    .replace("{world}", worldName));
            return;
        }

        double x = plugin.getConfig().getDouble("queue.lobby-location.x", 0.5);
        double y = plugin.getConfig().getDouble("queue.lobby-location.y", 100.0);
        double z = plugin.getConfig().getDouble("queue.lobby-location.z", 0.5);
        float yaw = (float) plugin.getConfig().getDouble("queue.lobby-location.yaw", 0.0);
        float pitch = (float) plugin.getConfig().getDouble("queue.lobby-location.pitch", 0.0);

        org.bukkit.Location lobby = new org.bukkit.Location(lobbyWorld, x, y, z, yaw, pitch);

        player.teleport(lobby);
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        player.sendMessage(getMessage("victory.returning-lobby").replace("{time}", "0"));

        // 检查是否启用自动重新匹配
        if (plugin.getConfig().getBoolean("queue.auto-rejoin-after-elimination", false)) {
            // 延迟1秒后自动加入队列
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getQueueManager().joinQueue(player);
                }
            }, 20L);
        }
    }

    /**
     * 治疗胜利者的宝可梦
     */
    private void healWinnerPokemon(UUID winnerUUID) {
        Player winner = Bukkit.getPlayer(winnerUUID);
        if (winner == null || !winner.isOnline()) {
            return;
        }

        // 执行治疗指令
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pokeheal " + winner.getName());

        winner.sendMessage(ChatColor.GREEN + "你的宝可梦已全部恢复！");
    }

    /**
     * 判断战斗失败是否应该淘汰
     */
    private boolean shouldEliminateOnDefeat(Game game) {
        if (game.getState() == GameState.FINAL_STAGE) {
            return plugin.getConfig().getBoolean("elimination.final-stage.battle-loss", true);
        } else {
            return plugin.getConfig().getBoolean("elimination.early-game.battle-loss", false);
        }
    }

    /**
     * 判断逃跑是否应该淘汰
     */
    private boolean shouldEliminateOnFlee(Game game) {
        if (game.getState() == GameState.FINAL_STAGE) {
            return plugin.getConfig().getBoolean("elimination.final-stage.flee", true);
        } else {
            return plugin.getConfig().getBoolean("elimination.early-game.flee", false);
        }
    }

    /**
     * 判断拒绝对战是否应该淘汰
     */
    private boolean shouldEliminateOnDecline(Game game) {
        if (game.getState() == GameState.FINAL_STAGE) {
            return plugin.getConfig().getBoolean("elimination.final-stage.decline-battle", true);
        } else {
            return plugin.getConfig().getBoolean("elimination.early-game.decline-battle", false);
        }
    }

    /**
     * 判断无宝可梦是否应该淘汰
     */
    private boolean shouldEliminateOnNoPokemon(Game game) {
        if (game.getState() == GameState.FINAL_STAGE) {
            return plugin.getConfig().getBoolean("elimination.final-stage.no-pokemon", true);
        } else {
            return plugin.getConfig().getBoolean("elimination.early-game.no-pokemon", true);
        }
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
