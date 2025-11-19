package com.pokemonbr.managers;

import com.pokemonbr.Main;
import com.pokemonbr.models.Game;
import com.pokemonbr.models.GamePlayer;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 胜利特效管理器
 * 负责游戏结束时的特效展示
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class VictoryEffectManager {

    private final Main plugin;

    public VictoryEffectManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * 播放胜利特效（给所有玩家）
     * @param game 游戏实例
     */
    public void playVictoryEffects(Game game) {
        List<GamePlayer> topThree = game.getTopThreePlayers();

        if (topThree.isEmpty()) {
            return;
        }

        // 为前三名播放特效
        for (int i = 0; i < topThree.size() && i < 3; i++) {
            GamePlayer gamePlayer = topThree.get(i);
            Player player = Bukkit.getPlayer(gamePlayer.getUuid());

            if (player == null || !player.isOnline()) {
                continue;
            }

            int rank = i + 1;
            playPlayerVictoryEffect(player, rank);
        }

        // 为其他玩家播放淘汰效果
        for (GamePlayer gamePlayer : game.getPlayers().values()) {
            if (gamePlayer.getRank() > 3) {
                Player player = Bukkit.getPlayer(gamePlayer.getUuid());
                if (player != null && player.isOnline()) {
                    playPlayerDefeatEffect(player, gamePlayer.getRank());
                }
            }
        }
    }

    /**
     * 为玩家播放胜利特效
     * @param player 玩家
     * @param rank 排名
     */
    private void playPlayerVictoryEffect(Player player, int rank) {
        // 播放Title
        sendVictoryTitle(player, rank);

        // 播放音效
        playVictorySound(player, rank);

        // 播放烟花
        if (plugin.getConfig().getBoolean("victory.effects.fireworks.enabled", true)) {
            spawnFireworks(player, rank);
        }
    }

    /**
     * 为玩家播放失败效果
     * @param player 玩家
     * @param rank 排名
     */
    private void playPlayerDefeatEffect(Player player, int rank) {
        // 发送Title
        String title = getMessage("victory.title");
        String subtitle = getMessage("victory.subtitle").replace("{rank}", String.valueOf(rank));

        player.sendTitle(
                ChatColor.translateAlternateColorCodes('&', title),
                ChatColor.translateAlternateColorCodes('&', subtitle),
                10, 60, 20
        );

        // 播放音效
        if (plugin.getConfig().getBoolean("victory.effects.sound.enabled", true)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    /**
     * 发送胜利Title
     * @param player 玩家
     * @param rank 排名
     */
    private void sendVictoryTitle(Player player, int rank) {
        if (!plugin.getConfig().getBoolean("victory.effects.title.enabled", true)) {
            return;
        }

        String title;
        String subtitle;

        switch (rank) {
            case 1:
                title = getMessage("victory.rank-1");
                subtitle = "§6★ 第一名 ★";
                break;
            case 2:
                title = getMessage("victory.rank-2");
                subtitle = "§7★ 第二名 ★";
                break;
            case 3:
                title = getMessage("victory.rank-3");
                subtitle = "§c★ 第三名 ★";
                break;
            default:
                title = getMessage("victory.title");
                subtitle = getMessage("victory.subtitle").replace("{rank}", String.valueOf(rank));
                break;
        }

        int fadeIn = plugin.getConfig().getInt("victory.effects.title.fade-in", 10);
        int stay = plugin.getConfig().getInt("victory.effects.title.stay", 70);
        int fadeOut = plugin.getConfig().getInt("victory.effects.title.fade-out", 20);

        player.sendTitle(
                ChatColor.translateAlternateColorCodes('&', title),
                ChatColor.translateAlternateColorCodes('&', subtitle),
                fadeIn, stay, fadeOut
        );
    }

    /**
     * 播放胜利音效
     * @param player 玩家
     * @param rank 排名
     */
    private void playVictorySound(Player player, int rank) {
        if (!plugin.getConfig().getBoolean("victory.effects.sound.enabled", true)) {
            return;
        }

        String soundName;
        float pitch;

        switch (rank) {
            case 1:
                soundName = plugin.getConfig().getString("victory.effects.sound.rank-1", "ENTITY_PLAYER_LEVELUP");
                pitch = 1.5f;
                break;
            case 2:
                soundName = plugin.getConfig().getString("victory.effects.sound.rank-2", "ENTITY_PLAYER_LEVELUP");
                pitch = 1.2f;
                break;
            case 3:
                soundName = plugin.getConfig().getString("victory.effects.sound.rank-3", "ENTITY_PLAYER_LEVELUP");
                pitch = 1.0f;
                break;
            default:
                soundName = "ENTITY_EXPERIENCE_ORB_PICKUP";
                pitch = 1.0f;
                break;
        }

        try {
            Sound sound = Sound.valueOf(soundName);
            float volume = (float) plugin.getConfig().getDouble("victory.effects.sound.volume", 1.0);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("§c无效的音效配置: " + soundName);
        }
    }

    /**
     * 生成烟花特效
     * @param player 玩家
     * @param rank 排名
     */
    private void spawnFireworks(Player player, int rank) {
        int amount = plugin.getConfig().getInt("victory.effects.fireworks.amount", 5);
        int duration = plugin.getConfig().getInt("victory.effects.fireworks.duration", 3);

        // 根据排名选择颜色
        List<Color> colors = getFireworkColors(rank);

        // 定时生成烟花
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int count = 0;

            @Override
            public void run() {
                if (count >= amount || !player.isOnline()) {
                    return;
                }

                Location loc = player.getLocation().clone().add(
                        (Math.random() - 0.5) * 4,
                        1,
                        (Math.random() - 0.5) * 4
                );

                Firework firework = (Firework) player.getWorld().spawnEntity(loc, EntityType.FIREWORK);
                FireworkMeta meta = firework.getFireworkMeta();

                // 设置烟花效果
                FireworkEffect.Builder builder = FireworkEffect.builder()
                        .with(getRandomFireworkType())
                        .withColor(colors)
                        .withFlicker()
                        .withTrail();

                meta.addEffect(builder.build());
                meta.setPower(1);
                firework.setFireworkMeta(meta);

                count++;
            }
        }, 0L, 10L); // 每0.5秒生成一个

        // duration秒后停止
        Bukkit.getScheduler().runTaskLater(plugin, task::cancel, duration * 20L);
    }

    /**
     * 获取烟花颜色（根据排名）
     * @param rank 排名
     * @return 颜色列表
     */
    private List<Color> getFireworkColors(int rank) {
        List<Color> colors = new ArrayList<>();

        switch (rank) {
            case 1:
                // 金色 + 黄色
                colors.add(Color.YELLOW);
                colors.add(Color.ORANGE);
                colors.add(Color.WHITE);
                break;
            case 2:
                // 银色 + 白色
                colors.add(Color.WHITE);
                colors.add(Color.SILVER);
                colors.add(Color.GRAY);
                break;
            case 3:
                // 铜色 + 红色
                colors.add(Color.RED);
                colors.add(Color.ORANGE);
                colors.add(Color.MAROON);
                break;
            default:
                colors.add(Color.AQUA);
                break;
        }

        return colors;
    }

    /**
     * 获取随机烟花类型
     * @return 烟花类型
     */
    private FireworkEffect.Type getRandomFireworkType() {
        FireworkEffect.Type[] types = FireworkEffect.Type.values();
        return types[(int) (Math.random() * types.length)];
    }

    /**
     * 获取消息
     * @param key 消息键
     * @return 消息
     */
    private String getMessage(String key) {
        String prefix = plugin.getConfigManager().getMessagesConfig().getString("prefix", "");
        String message = plugin.getConfigManager().getMessagesConfig().getString(key, key);
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }
}
