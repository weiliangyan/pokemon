package com.pokemonbr.managers;

import com.pokemonbr.Main;
import com.pokemonbr.models.Game;
import com.pokemonbr.models.GamePlayer;
import com.pokemonbr.models.PlayerData;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.List;
import java.util.UUID;

/**
 * 奖励管理器
 * 管理玩家奖励和积分发放
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class RewardManager {

    private final Main plugin;
    private Economy economy;

    public RewardManager(Main plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    /**
     * 设置经济系统
     */
    private void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager()
                .getRegistration(Economy.class);

        if (rsp != null) {
            economy = rsp.getProvider();
            plugin.getLogger().info("§aVault经济系统已连接");
        }
    }

    /**
     * 处理游戏奖励
     * @param game 游戏实例
     */
    public void processGameRewards(Game game) {
        plugin.getLogger().info("§a正在处理游戏 " + game.getGameUuid() + " 的奖励...");

        // 获取前三名玩家
        List<GamePlayer> topThree = game.getTopThreePlayers();

        // 处理每个玩家的奖励
        for (GamePlayer gamePlayer : game.getPlayers().values()) {
            processPlayerReward(game, gamePlayer);
        }
    }

    /**
     * 处理单个玩家的奖励
     */
    private void processPlayerReward(Game game, GamePlayer gamePlayer) {
        Player player = Bukkit.getPlayer(gamePlayer.getUuid());
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(gamePlayer.getUuid());

        int rank = gamePlayer.getRank();
        int kills = gamePlayer.getKills();

        // 计算积分
        int rankPoints = calculateRankPoints(rank);
        int killPoints = calculateKillPoints(rank, kills);
        int totalPoints = rankPoints + killPoints;

        // 应用VIP加成
        if (player != null && player.isOnline()) {
            double multiplier = getVIPMultiplier(player);
            totalPoints = (int) (totalPoints * multiplier);
        }

        // 更新玩家数据
        data.addTotalPoints(totalPoints);
        data.addTotalGames(1);
        data.addTotalKills(kills);

        if (rank == 1) {
            data.addTotalWins(1);
        }

        // 保存数据
        plugin.getPlayerDataManager().savePlayerData(data);

        // 发放货币奖励
        double money = calculateMoneyReward(rank);
        if (economy != null && player != null && player.isOnline()) {
            economy.depositPlayer(player, money);
        }

        // 执行奖励指令
        executeRewardCommands(gamePlayer, rank);

        // 发送奖励消息
        if (player != null && player.isOnline()) {
            sendRewardMessage(player, rank, rankPoints, killPoints, totalPoints, money);
        }
    }

    /**
     * 计算排名积分
     */
    private int calculateRankPoints(int rank) {
        FileConfiguration config = plugin.getConfigManager().getRewardsConfig();

        // 前三名特殊奖励
        if (rank == 1) {
            return config.getInt("top-rewards.rank-1.rank-points", 100);
        } else if (rank == 2) {
            return config.getInt("top-rewards.rank-2.rank-points", 75);
        } else if (rank == 3) {
            return config.getInt("top-rewards.rank-3.rank-points", 50);
        }

        // 其他排名
        if (rank >= 4 && rank <= 10) {
            return config.getInt("other-ranks.ranges.4-10.rank-points", 30);
        } else if (rank >= 11 && rank <= 20) {
            return config.getInt("other-ranks.ranges.11-20.rank-points", 20);
        } else {
            return config.getInt("other-ranks.ranges.21+.rank-points", 10);
        }
    }

    /**
     * 计算击败积分
     */
    private int calculateKillPoints(int rank, int kills) {
        FileConfiguration config = plugin.getConfigManager().getRewardsConfig();

        int pointsPerKill;

        if (rank == 1) {
            pointsPerKill = config.getInt("top-rewards.rank-1.kill-points", 10);
        } else if (rank == 2) {
            pointsPerKill = config.getInt("top-rewards.rank-2.kill-points", 8);
        } else if (rank == 3) {
            pointsPerKill = config.getInt("top-rewards.rank-3.kill-points", 6);
        } else if (rank >= 4 && rank <= 10) {
            pointsPerKill = config.getInt("other-ranks.ranges.4-10.kill-points", 5);
        } else if (rank >= 11 && rank <= 20) {
            pointsPerKill = config.getInt("other-ranks.ranges.11-20.kill-points", 4);
        } else {
            pointsPerKill = config.getInt("other-ranks.ranges.21+.kill-points", 3);
        }

        return kills * pointsPerKill;
    }

    /**
     * 计算货币奖励
     */
    private double calculateMoneyReward(int rank) {
        FileConfiguration config = plugin.getConfigManager().getRewardsConfig();

        if (rank == 1) {
            return config.getDouble("top-rewards.rank-1.money", 1000);
        } else if (rank == 2) {
            return config.getDouble("top-rewards.rank-2.money", 500);
        } else if (rank == 3) {
            return config.getDouble("top-rewards.rank-3.money", 300);
        } else if (rank >= 4 && rank <= 10) {
            return config.getDouble("other-ranks.ranges.4-10.money", 100);
        } else if (rank >= 11 && rank <= 20) {
            return config.getDouble("other-ranks.ranges.11-20.money", 50);
        } else {
            return config.getDouble("other-ranks.ranges.21+.money", 20);
        }
    }

    /**
     * 执行奖励指令
     */
    private void executeRewardCommands(GamePlayer gamePlayer, int rank) {
        FileConfiguration config = plugin.getConfigManager().getRewardsConfig();
        String configPath;

        if (rank == 1) {
            configPath = "top-rewards.rank-1.commands";
        } else if (rank == 2) {
            configPath = "top-rewards.rank-2.commands";
        } else if (rank == 3) {
            configPath = "top-rewards.rank-3.commands";
        } else {
            return; // 其他排名默认无指令奖励
        }

        List<String> commands = config.getStringList(configPath);

        for (String command : commands) {
            String cmd = command.replace("%player%", gamePlayer.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    /**
     * 获取VIP积分加成
     */
    private double getVIPMultiplier(Player player) {
        FileConfiguration config = plugin.getConfigManager().getRewardsConfig();
        ConfigurationSection vipSection = config.getConfigurationSection("multipliers.vip-bonus");

        if (vipSection == null) {
            return 1.0;
        }

        // 从高到低检查VIP权限
        if (player.hasPermission("pbr.vip.plays.30")) {
            return vipSection.getDouble("pbr.vip.plays.30", 2.0);
        } else if (player.hasPermission("pbr.vip.plays.20")) {
            return vipSection.getDouble("pbr.vip.plays.20", 1.5);
        } else if (player.hasPermission("pbr.vip.plays.10")) {
            return vipSection.getDouble("pbr.vip.plays.10", 1.2);
        }

        return 1.0;
    }

    /**
     * 发送奖励消息
     */
    private void sendRewardMessage(Player player, int rank, int rankPoints, int killPoints, int totalPoints, double money) {
        // 发送排名消息
        String rankMessage;
        if (rank == 1) {
            rankMessage = getMessage("victory.rank-1");
        } else if (rank == 2) {
            rankMessage = getMessage("victory.rank-2");
        } else if (rank == 3) {
            rankMessage = getMessage("victory.rank-3");
        } else {
            rankMessage = getMessage("victory.rank-other").replace("{rank}", String.valueOf(rank));
        }

        player.sendMessage(rankMessage);

        // 发送奖励详情
        player.sendMessage(getMessage("victory.reward-received")
                .replace("{points}", String.valueOf(totalPoints)));

        List<String> details = plugin.getConfigManager().getMessagesConfig()
                .getStringList("victory.reward-details");

        for (String line : details) {
            String message = ChatColor.translateAlternateColorCodes('&', line)
                    .replace("{rank_points}", String.valueOf(rankPoints))
                    .replace("{kill_points}", String.valueOf(killPoints))
                    .replace("{total_points}", String.valueOf(totalPoints));

            player.sendMessage(message);
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

