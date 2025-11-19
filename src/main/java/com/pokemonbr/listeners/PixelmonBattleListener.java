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

import java.lang.reflect.Method;
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
    private final boolean pixelmonAvailable;
    private static final String[] REQUIRED_METHODS = {
        "getLoser", "getWinner", "getFleer", "getPlayer", "getUUID",
        "getParty", "countPokemon", "getTeam", "isFainted",
        "getDisplayName", "getSpecies", "getLocalizedName", "set"
    };

    public PixelmonBattleListener(Main plugin) {
        this.plugin = plugin;
        this.pixelmonAvailable = checkPixelmonAPI();
    }

    /**
     * 检查Pixelmon API可用性
     */
    private boolean checkPixelmonAPI() {
        try {
            plugin.getLogger().info("§a正在检查Pixelmon API兼容性...");

            // 检查Pixelmon主类
            Class<?> pixelmonClass = Class.forName("com.pixelmonmod.pixelmon.Pixelmon");
            plugin.getLogger().info("§a✓ Pixelmon主类已找到");

            // 检查存储管理器
            Object storageManager = pixelmonClass.getField("storageManager").get(null);
            if (storageManager != null) {
                plugin.getLogger().info("§a✓ 存储管理器已找到");

                // 检查关键方法
                Method getPartyMethod = storageManager.getClass().getMethod("getParty", java.util.UUID.class);
                if (getPartyMethod != null) {
                    plugin.getLogger().info("§a✓ getParty方法已找到");
                }
            }

            // 检查战斗事件类（尝试多个可能的类名）
            String[] possibleEventClasses = {
                "com.pixelmonmod.pixelmon.api.events.battles.BattleEndEvent",
                "com.pixelmonmod.pixelmon.battles.api.event.BattleEndedEvent",
                "com.pixelmonmod.pixelmon.api.events.BattleEndEvent"
            };

            boolean battleEventFound = false;
            for (String eventClass : possibleEventClasses) {
                try {
                    Class.forName(eventClass);
                    plugin.getLogger().info("§a✓ 战斗事件类已找到: " + eventClass);
                    battleEventFound = true;
                    break;
                } catch (ClassNotFoundException e) {
                    // 继续尝试下一个类名
                }
            }

            if (!battleEventFound) {
                plugin.getLogger().warning("§c未找到任何已知的战斗事件类，战斗监听可能无法正常工作");
            }

            plugin.getLogger().info("§aPixelmon API检查完成，战斗监听器已启用");
            return true;

        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("§cPixelmon主类未找到！请确保安装了正确版本的Pixelmon插件");
            plugin.getLogger().severe("§cPokemonBattleRoyale插件的战斗功能将被禁用");
            return false;
        } catch (NoSuchFieldException e) {
            plugin.getLogger().severe("§cPixelmon API结构已更改！找不到storageManager字段");
            plugin.getLogger().severe("§c请检查Pixelmon版本兼容性，战斗功能将被禁用");
            return false;
        } catch (NoSuchMethodException e) {
            plugin.getLogger().severe("§cPixelmon API方法已更改！找不到必需的方法: " + e.getMessage());
            plugin.getLogger().severe("§c请检查Pixelmon版本兼容性，战斗功能将被禁用");
            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("§c检查Pixelmon API时发生未知错误: " + e.getMessage());
            plugin.getLogger().severe("§c战斗功能将被禁用以防止错误");
            e.printStackTrace();
            return false;
        }
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
        // 检查Pixelmon API是否可用
        if (!pixelmonAvailable) {
            return;
        }

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
                // 开局阶段：删除一只宝可梦
                boolean pokemonRemoved = removeRandomPokemon(loserPlayer);

                if (pokemonRemoved) {
                    loserPlayer.sendMessage(getMessage("battle.defeat-pokemon-removed"));
                    plugin.getLogger().info("玩家 " + loserPlayer.getName() + " 战败，已删除一只宝可梦");

                    // 立即检查宝可梦数量，可能需要淘汰
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        checkPlayerPokemonCount(loserPlayer);
                    }, 10L); // 延迟半秒执行，确保删除完成
                } else {
                    loserPlayer.sendMessage(getMessage("battle.defeat"));
                }
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
        if (!pixelmonAvailable) {
            return;
        }

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
        if (!pixelmonAvailable) {
            return;
        }

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
        if (!pixelmonAvailable) {
            return;
        }

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
        if (!pixelmonAvailable) {
            return;
        }

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
     * 删除玩家的一只随机宝可梦（开局阶段惩罚）
     */
    private boolean removeRandomPokemon(Player player) {
        if (!pixelmonAvailable) {
            return false;
        }

        try {
            // 获取玩家的宝可梦存储
            Class<?> pixelmonClass = Class.forName("com.pixelmonmod.pixelmon.Pixelmon");
            Object storageManager = pixelmonClass.getField("storageManager").get(null);

            Object partyStorage = storageManager.getClass()
                    .getMethod("getParty", java.util.UUID.class)
                    .invoke(storageManager, player.getUniqueId());

            if (partyStorage != null) {
                // 获取宝可梦数量
                Integer count = (Integer) partyStorage.getClass()
                        .getMethod("countPokemon")
                        .invoke(partyStorage);

                if (count != null && count > 0) {
                    // 获取所有宝可梦
                    java.util.List<Object> pokemonList = (java.util.List<Object>) partyStorage.getClass()
                            .getMethod("getTeam")
                            .invoke(partyStorage);

                    // 过滤出非空的宝可梦
                    java.util.List<Object> validPokemon = new java.util.ArrayList<>();
                    for (Object pokemon : pokemonList) {
                        if (pokemon != null) {
                            // 检查宝可梦是否存活或存在
                            try {
                                Boolean isFainted = (Boolean) pokemon.getClass()
                                        .getMethod("isFainted")
                                        .invoke(pokemon);
                                if (isFainted != null && !isFainted) {
                                    validPokemon.add(pokemon);
                                }
                            } catch (Exception e) {
                                // 如果无法检查是否昏厥，只要不是null就认为是有效宝可梦
                                validPokemon.add(pokemon);
                            }
                        }
                    }

                    if (!validPokemon.isEmpty()) {
                        // 随机选择一只宝可梦删除
                        Object targetPokemon = validPokemon.get(
                            java.util.concurrent.ThreadLocalRandom.current().nextInt(validPokemon.size())
                        );

                        // 获取宝可梦的显示名称用于日志
                        String pokemonName = "未知宝可梦";
                        try {
                            pokemonName = (String) targetPokemon.getClass()
                                    .getMethod("getDisplayName")
                                    .invoke(targetPokemon);
                        } catch (Exception e) {
                            try {
                                // 尝试获取Species名称
                                Object species = targetPokemon.getClass()
                                        .getMethod("getSpecies")
                                        .invoke(targetPokemon);
                                if (species != null) {
                                    pokemonName = (String) species.getClass()
                                            .getMethod("getLocalizedName")
                                            .invoke(species);
                                }
                            } catch (Exception ex) {
                                // 使用默认名称
                            }
                        }

                        // 从队伍中移除宝可梦
                        partyStorage.getClass()
                                .getMethod("set", int.class, Object.class)
                                .invoke(partyStorage, pokemonList.indexOf(targetPokemon), null);

                        player.sendMessage(ChatColor.YELLOW + "你的宝可梦 " + ChatColor.RED + pokemonName +
                                         ChatColor.YELLOW + " 因战斗失败被删除了！");

                        plugin.getLogger().info("已删除玩家 " + player.getName() + " 的宝可梦: " + pokemonName);
                        return true;
                    }
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("§c删除玩家宝可梦时出错: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
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
