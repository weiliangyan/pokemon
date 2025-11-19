package com.pokemonbr.managers;

import com.pokemonbr.Main;
import com.pokemonbr.models.Game;
import com.pokemonbr.models.GamePlayer;
import com.pokemonbr.models.MatchQueue;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 计分板管理器
 * 管理玩家计分板显示
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class ScoreboardManager {

    private final Main plugin;
    private final Map<UUID, Scoreboard> playerScoreboards;
    private BukkitTask updateTask;

    public ScoreboardManager(Main plugin) {
        this.plugin = plugin;
        this.playerScoreboards = new HashMap<>();

        // 启动计分板更新任务
        startUpdateTask();
    }

    /**
     * 启动计分板更新任务
     */
    private void startUpdateTask() {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updatePlayerScoreboard(player);
            }
        }, 20L, 20L); // 每秒更新一次
    }

    /**
     * 更新玩家计分板
     */
    public void updatePlayerScoreboard(Player player) {
        // 检查玩家是否在队列中
        MatchQueue queue = plugin.getQueueManager().getPlayerQueue(player);
        if (queue != null) {
            showQueueScoreboard(player, queue);
            return;
        }

        // 检查玩家是否在游戏中
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game != null) {
            GamePlayer gamePlayer = game.getGamePlayer(player.getUniqueId());
            if (gamePlayer != null) {
                if (gamePlayer.isAlive()) {
                    showGameScoreboard(player, game, gamePlayer);
                } else {
                    showSpectatorScoreboard(player, game, gamePlayer);
                }
                return;
            }
        }

        // 移除计分板
        removeScoreboard(player);
    }

    /**
     * 显示队列计分板
     */
    private void showQueueScoreboard(Player player, MatchQueue queue) {
        Scoreboard scoreboard = getOrCreateScoreboard(player);
        Objective objective = getOrCreateObjective(scoreboard, "queue");

        // 设置标题
        String title = getMessage("scoreboard.queue-title");
        objective.setDisplayName(title);

        // 获取剩余游玩次数
        int remaining = plugin.getPlayerDataManager().getRemainingPlays(player);

        // 设置内容
        clearScores(objective);
        setScore(objective, "§7§m------------------", 8);
        setScore(objective, "§e状态: §a匹配中", 7);
        setScore(objective, "§e人数: §a" + queue.getPlayerCount() + "/" + queue.getMaxPlayers(), 6);
        setScore(objective, "§e倒计时: §a" + queue.getCountdown() + "秒", 5);
        setScore(objective, "§7", 4);
        setScore(objective, "§e今日剩余: §a" + remaining + "次", 3);
        setScore(objective, "§7§m------------------", 2);

        player.setScoreboard(scoreboard);
    }

    /**
     * 显示游戏计分板
     */
    private void showGameScoreboard(Player player, Game game, GamePlayer gamePlayer) {
        Scoreboard scoreboard = getOrCreateScoreboard(player);
        Objective objective = getOrCreateObjective(scoreboard, "game");

        // 设置标题
        String title = getMessage("scoreboard.game-title");
        objective.setDisplayName(title);

        // 获取缩圈倒计时
        int shrinkTime = plugin.getBorderShrinkManager().getNextShrinkCountdown(game);
        int borderSize = plugin.getBorderShrinkManager().getCurrentBorderSize(game);

        // 设置内容
        clearScores(objective);
        setScore(objective, "§7§m------------------", 9);
        setScore(objective, "§e存活人数: §a" + game.getAlivePlayerCount(), 8);
        setScore(objective, "§e你的击败: §a" + gamePlayer.getKills(), 7);
        setScore(objective, "§e当前排名: §a#" + (game.getTotalPlayerCount() - game.getAlivePlayerCount() + 1), 6);
        setScore(objective, "§7", 5);
        setScore(objective, "§e下次缩圈: §c" + formatTime(shrinkTime), 4);
        setScore(objective, "§e边界大小: §c" + borderSize, 3);
        setScore(objective, "§7§m------------------", 2);

        player.setScoreboard(scoreboard);
    }

    /**
     * 显示观战计分板
     */
    private void showSpectatorScoreboard(Player player, Game game, GamePlayer gamePlayer) {
        Scoreboard scoreboard = getOrCreateScoreboard(player);
        Objective objective = getOrCreateObjective(scoreboard, "spectator");

        // 设置标题
        String title = getMessage("scoreboard.spectator-title");
        objective.setDisplayName(title);

        // 设置内容
        clearScores(objective);
        setScore(objective, "§7§m------------------", 9);
        setScore(objective, "§e存活人数: §a" + game.getAlivePlayerCount(), 8);
        setScore(objective, "§e你的排名: §7#" + gamePlayer.getRank(), 7);
        setScore(objective, "§e你的击败: §7" + gamePlayer.getKills(), 6);
        setScore(objective, "§7", 5);
        setScore(objective, "§7输入 /pbr lobby", 4);
        setScore(objective, "§7返回大厅", 3);
        setScore(objective, "§7§m------------------", 2);

        player.setScoreboard(scoreboard);
    }

    /**
     * 移除计分板
     */
    public void removeScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        playerScoreboards.remove(player.getUniqueId());
    }

    /**
     * 获取或创建计分板
     */
    private Scoreboard getOrCreateScoreboard(Player player) {
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            playerScoreboards.put(player.getUniqueId(), scoreboard);
        }
        return scoreboard;
    }

    /**
     * 获取或创建目标
     */
    private Objective getOrCreateObjective(Scoreboard scoreboard, String name) {
        Objective objective = scoreboard.getObjective(name);
        if (objective == null) {
            objective = scoreboard.registerNewObjective(name, "dummy", "PBR");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        return objective;
    }

    /**
     * 清空计分
     */
    private void clearScores(Objective objective) {
        for (String entry : objective.getScoreboard().getEntries()) {
            objective.getScoreboard().resetScores(entry);
        }
    }

    /**
     * 设置计分
     */
    private void setScore(Objective objective, String text, int score) {
        objective.getScore(text).setScore(score);
    }

    /**
     * 格式化时间
     */
    private String formatTime(int seconds) {
        if (seconds >= 60) {
            int minutes = seconds / 60;
            int remainingSeconds = seconds % 60;
            return minutes + ":" + String.format("%02d", remainingSeconds);
        }
        return seconds + "秒";
    }

    /**
     * 获取消息
     */
    private String getMessage(String key) {
        String message = plugin.getConfigManager().getMessagesConfig().getString(key, key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * 关闭计分板管理器
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        // 清除所有玩家的计分板
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeScoreboard(player);
        }

        playerScoreboards.clear();
    }
}
