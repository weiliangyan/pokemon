package com.pokemonbr.managers;

import com.pokemonbr.Main;
import com.pokemonbr.models.Game;
import com.pokemonbr.models.GameState;
import com.pokemonbr.models.ShrinkStage;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * 边界缩圈管理器
 * 管理游戏世界边界的缩小
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class BorderShrinkManager {

    private final Main plugin;

    // 游戏UUID -> 缩圈数据
    private final Map<String, ShrinkData> activeShrinks;

    // 缩圈任务
    private BukkitTask shrinkTask;

    public BorderShrinkManager(Main plugin) {
        this.plugin = plugin;
        this.activeShrinks = new HashMap<>();

        // 启动缩圈任务
        startShrinkTask();
    }

    /**
     * 缩圈数据类
     */
    private static class ShrinkData {
        Game game;
        List<ShrinkStage> stages;
        int currentStage;
        int countdown;
        boolean shrinking;

        ShrinkData(Game game, List<ShrinkStage> stages) {
            this.game = game;
            this.stages = stages;
            this.currentStage = 0;
            this.countdown = 0;
            this.shrinking = false;
        }
    }

    /**
     * 启动缩圈任务
     */
    private void startShrinkTask() {
        shrinkTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (ShrinkData data : new ArrayList<>(activeShrinks.values())) {
                updateShrink(data);
            }
        }, 20L, 20L); // 每秒执行一次
    }

    /**
     * 更新缩圈状态
     * @param data 缩圈数据
     */
    private void updateShrink(ShrinkData data) {
        // 检查游戏是否还在进行
        if (data.game.getState() != GameState.PLAYING && data.game.getState() != GameState.FINAL_STAGE) {
            return;
        }

        // 如果还有阶段未执行
        if (data.currentStage < data.stages.size()) {
            if (!data.shrinking) {
                // 倒计时阶段
                if (data.countdown > 0) {
                    data.countdown--;

                    // 广播倒计时
                    if (data.countdown == 60 || data.countdown == 30 || data.countdown == 10 || data.countdown <= 5) {
                        broadcastCountdown(data);
                    }
                } else {
                    // 开始缩圈
                    startStageShrink(data);
                }
            }
        }
    }

    /**
     * 为游戏初始化缩圈系统
     * @param game 游戏实例
     */
    public void initializeShrink(Game game) {
        // 加载缩圈阶段配置（优先使用世界配置）
        List<ShrinkStage> stages = loadShrinkStages(game);

        if (stages.isEmpty()) {
            plugin.getLogger().warning("§c未找到缩圈阶段配置");
            return;
        }

        // 创建缩圈数据
        ShrinkData data = new ShrinkData(game, stages);

        // 设置初始世界边界
        World world = game.getGameWorld();

        // 获取初始大小和中心点（优先使用世界配置）
        int initialSize;
        double centerX, centerZ;

        org.bukkit.configuration.file.FileConfiguration worldConfig =
            plugin.getWorldConfigManager().getWorldConfig(game.getWorldConfigName());

        if (worldConfig != null && worldConfig.getBoolean("shrink-stages.override-global", false)) {
            initialSize = worldConfig.getInt("shrink-stages.initial-size", 500);
            plugin.getLogger().info("§7使用世界专属初始边界大小: " + initialSize);
        } else {
            initialSize = plugin.getConfig().getInt("shrink.initial-size", 500);
        }

        // 读取缩圈中心点（从世界配置）
        if (worldConfig != null) {
            String mode = worldConfig.getString("shrink-center.mode", "random");
            if ("random".equals(mode)) {
                // 随机中心点
                int minX = worldConfig.getInt("shrink-center.min-x", -150);
                int maxX = worldConfig.getInt("shrink-center.max-x", 150);
                int minZ = worldConfig.getInt("shrink-center.min-z", -150);
                int maxZ = worldConfig.getInt("shrink-center.max-z", 150);

                Random random = new Random();
                centerX = minX + random.nextInt(maxX - minX + 1);
                centerZ = minZ + random.nextInt(maxZ - minZ + 1);
            } else {
                // 固定中心点
                centerX = worldConfig.getDouble("shrink-center.fixed.x", 0.0);
                centerZ = worldConfig.getDouble("shrink-center.fixed.z", 0.0);
            }
        } else {
            centerX = plugin.getConfig().getDouble("shrink.center.x", 0.0);
            centerZ = plugin.getConfig().getDouble("shrink.center.z", 0.0);
        }

        WorldBorder border = world.getWorldBorder();
        border.setCenter(centerX, centerZ);
        border.setSize(initialSize);
        border.setWarningDistance(20);

        // 设置第一阶段的倒计时
        if (!stages.isEmpty()) {
            data.countdown = stages.get(0).getDelay();
        }

        activeShrinks.put(game.getGameUuid(), data);

        plugin.getLogger().info("§a游戏 " + game.getGameUuid() + " 缩圈系统已初始化");
        plugin.getLogger().info("§a缩圈中心: (" + centerX + ", " + centerZ + "), 初始大小: " + initialSize);
        plugin.getLogger().info("§a共 " + stages.size() + " 个缩圈阶段");
    }

    /**
     * 加载缩圈阶段配置（支持世界专属配置）
     * @param game 游戏实例
     * @return 缩圈阶段列表
     */
    private List<ShrinkStage> loadShrinkStages(Game game) {
        List<ShrinkStage> stages = new ArrayList<>();

        // 获取世界配置
        org.bukkit.configuration.file.FileConfiguration worldConfig =
            plugin.getWorldConfigManager().getWorldConfig(game.getWorldConfigName());

        // 检查是否使用世界专属配置
        if (worldConfig != null && worldConfig.getBoolean("shrink-stages.override-global", false)) {
            plugin.getLogger().info("§7使用世界专属缩圈配置: " + game.getWorldConfigName());
            List<Map<?, ?>> stagesList = worldConfig.getMapList("shrink-stages.stages");
            stages = parseShrinkStages(stagesList);
        } else {
            // 使用全局配置
            List<Map<?, ?>> stagesList = plugin.getConfig().getMapList("shrink.stages");
            stages = parseShrinkStages(stagesList);
        }

        return stages;
    }

    /**
     * 解析缩圈阶段列表
     * @param stagesList 配置中的阶段列表
     * @return 缩圈阶段对象列表
     */
    private List<ShrinkStage> parseShrinkStages(List<Map<?, ?>> stagesList) {
        List<ShrinkStage> stages = new ArrayList<>();
        int stageNumber = 1;

        for (Map<?, ?> stageMap : stagesList) {
            int size = (Integer) stageMap.get("size");
            int duration = (Integer) stageMap.get("duration");
            int delay = (Integer) stageMap.get("delay");

            stages.add(new ShrinkStage(stageNumber, size, duration, delay));
            stageNumber++;
        }

        return stages;
    }

    /**
     * 开始某个阶段的缩圈
     * @param data 缩圈数据
     */
    private void startStageShrink(ShrinkData data) {
        ShrinkStage stage = data.stages.get(data.currentStage);
        data.shrinking = true;

        // 广播缩圈开始
        String message = getMessage("shrink.started")
                .replace("{size}", String.valueOf(stage.getTargetSize()));
        data.game.broadcastMessage(message);

        // 设置世界边界缩小
        WorldBorder border = data.game.getGameWorld().getWorldBorder();
        border.setSize(stage.getTargetSize(), stage.getDuration());

        // 检查是否是最后一个阶段
        boolean isFinalStage = data.currentStage == data.stages.size() - 1;
        if (isFinalStage && plugin.getConfig().getBoolean("shrink.final-stage-special", true)) {
            data.game.setState(GameState.FINAL_STAGE);
            data.game.broadcastMessage(getMessage("shrink.final-warning"));
        }

        // 延迟执行下一阶段
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            onStageShrinkComplete(data, stage);
        }, stage.getDuration() * 20L);
    }

    /**
     * 缩圈阶段完成
     * @param data 缩圈数据
     * @param stage 完成的阶段
     */
    private void onStageShrinkComplete(ShrinkData data, ShrinkStage stage) {
        // 广播缩圈完成
        String message = getMessage("shrink.completed")
                .replace("{size}", String.valueOf(stage.getTargetSize()));
        data.game.broadcastMessage(message);

        // 准备下一阶段
        data.currentStage++;
        data.shrinking = false;

        if (data.currentStage < data.stages.size()) {
            // 设置下一阶段的倒计时
            ShrinkStage nextStage = data.stages.get(data.currentStage);
            data.countdown = nextStage.getDelay();

            // 广播下一阶段信息
            String info = getMessage("shrink.stage-info")
                    .replace("{stage}", String.valueOf(nextStage.getStageNumber()))
                    .replace("{size}", String.valueOf(nextStage.getTargetSize()));
            data.game.broadcastMessage(info);
        }
    }

    /**
     * 广播倒计时
     * @param data 缩圈数据
     */
    private void broadcastCountdown(ShrinkData data) {
        String message = getMessage("shrink.countdown")
                .replace("{time}", formatTime(data.countdown));
        data.game.broadcastMessage(message);
    }

    /**
     * 检查玩家是否在边界外
     * @param player 玩家
     * @return 是否在边界外
     */
    public boolean isPlayerOutsideBorder(Player player) {
        World world = player.getWorld();
        WorldBorder border = world.getWorldBorder();

        Location loc = player.getLocation();
        double centerX = border.getCenter().getX();
        double centerZ = border.getCenter().getZ();
        double radius = border.getSize() / 2.0;

        double distanceX = Math.abs(loc.getX() - centerX);
        double distanceZ = Math.abs(loc.getZ() - centerZ);

        return distanceX > radius || distanceZ > radius;
    }

    /**
     * 对边界外的玩家造成伤害
     * @param game 游戏实例
     */
    public void damagePlayersOutsideBorder(Game game) {
        double damage = plugin.getConfig().getDouble("shrink.damage-per-second", 2.0);

        for (UUID uuid : game.getAlivePlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue;
            }

            if (isPlayerOutsideBorder(player)) {
                player.damage(damage);

                // 发送警告消息
                player.sendMessage(getMessage("shrink.damage-warning"));
            }
        }
    }

    /**
     * 获取游戏的当前缩圈阶段
     * @param game 游戏实例
     * @return 当前阶段 或 null
     */
    public ShrinkStage getCurrentStage(Game game) {
        ShrinkData data = activeShrinks.get(game.getGameUuid());
        if (data == null || data.currentStage >= data.stages.size()) {
            return null;
        }
        return data.stages.get(data.currentStage);
    }

    /**
     * 获取下次缩圈倒计时
     * @param game 游戏实例
     * @return 倒计时秒数
     */
    public int getNextShrinkCountdown(Game game) {
        ShrinkData data = activeShrinks.get(game.getGameUuid());
        return data != null ? data.countdown : 0;
    }

    /**
     * 获取当前边界大小
     * @param game 游戏实例
     * @return 边界大小
     */
    public int getCurrentBorderSize(Game game) {
        WorldBorder border = game.getGameWorld().getWorldBorder();
        return (int) border.getSize();
    }

    /**
     * 停止游戏的缩圈
     * @param game 游戏实例
     */
    public void stopShrink(Game game) {
        activeShrinks.remove(game.getGameUuid());
    }

    /**
     * 格式化时间
     * @param seconds 秒数
     * @return 格式化后的时间
     */
    private String formatTime(int seconds) {
        if (seconds >= 60) {
            int minutes = seconds / 60;
            int remainingSeconds = seconds % 60;
            return minutes + ":" + String.format("%02d", remainingSeconds);
        }
        return String.valueOf(seconds) + "秒";
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

    /**
     * 关闭缩圈管理器
     */
    public void shutdown() {
        if (shrinkTask != null) {
            shrinkTask.cancel();
        }
        activeShrinks.clear();
    }
}
