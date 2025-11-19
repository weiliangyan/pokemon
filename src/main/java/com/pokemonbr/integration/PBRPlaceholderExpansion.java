package com.pokemonbr.integration;

import com.pokemonbr.Main;
import com.pokemonbr.models.Game;
import com.pokemonbr.models.GamePlayer;
import com.pokemonbr.models.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI 扩展
 * 注册插件变量
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class PBRPlaceholderExpansion extends PlaceholderExpansion {

    private final Main plugin;

    public PBRPlaceholderExpansion(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "pbr";
    }

    @Override
    public String getAuthor() {
        return "l1ang_Y5n";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // 插件重载时不注销
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);

        // ==================== 玩家统计数据 ====================

        // %pbr_games% - 总局数
        if (identifier.equals("games")) {
            return String.valueOf(data.getTotalGames());
        }

        // %pbr_points% - 总积分
        if (identifier.equals("points")) {
            return String.valueOf(data.getTotalPoints());
        }

        // %pbr_kills% - 总击败数
        if (identifier.equals("kills")) {
            return String.valueOf(data.getTotalKills());
        }

        // %pbr_wins% - 总胜利数
        if (identifier.equals("wins")) {
            return String.valueOf(data.getTotalWins());
        }

        // %pbr_winrate% - 胜率（百分比）
        if (identifier.equals("winrate")) {
            return String.format("%.2f", data.getWinRate());
        }

        // %pbr_avg_kills% - 场均击败
        if (identifier.equals("avg_kills")) {
            return String.format("%.2f", data.getAverageKills());
        }

        // %pbr_plays_today% - 今日已用次数
        if (identifier.equals("plays_today")) {
            return String.valueOf(data.getPlaysToday());
        }

        // %pbr_plays_remaining% - 今日剩余次数
        if (identifier.equals("plays_remaining")) {
            return String.valueOf(plugin.getPlayerDataManager().getRemainingPlays(player));
        }

        // %pbr_plays_max% - 今日最大次数
        if (identifier.equals("plays_max")) {
            return String.valueOf(plugin.getPlayerDataManager().getMaxPlays(player));
        }

        // ==================== 游戏中数据 ====================

        Game game = plugin.getGameManager().getPlayerGame(player);

        // %pbr_in_game% - 是否在游戏中 (true/false)
        if (identifier.equals("in_game")) {
            return String.valueOf(game != null);
        }

        // 如果不在游戏中，以下变量返回空或默认值
        if (game == null) {
            if (identifier.equals("game_alive")) return "0";
            if (identifier.equals("game_total")) return "0";
            if (identifier.equals("game_kills")) return "0";
            if (identifier.equals("game_rank")) return "0";
            if (identifier.equals("game_state")) return "无";
            if (identifier.equals("game_shrink_time")) return "0";
            if (identifier.equals("game_border_size")) return "0";
            return null;
        }

        GamePlayer gamePlayer = game.getGamePlayer(player.getUniqueId());

        // %pbr_game_alive% - 游戏中存活人数
        if (identifier.equals("game_alive")) {
            return String.valueOf(game.getAlivePlayerCount());
        }

        // %pbr_game_total% - 游戏中总人数
        if (identifier.equals("game_total")) {
            return String.valueOf(game.getTotalPlayerCount());
        }

        // %pbr_game_kills% - 游戏中击败数
        if (identifier.equals("game_kills")) {
            return gamePlayer != null ? String.valueOf(gamePlayer.getKills()) : "0";
        }

        // %pbr_game_rank% - 游戏中当前排名
        if (identifier.equals("game_rank")) {
            if (gamePlayer != null && gamePlayer.isAlive()) {
                int rank = game.getTotalPlayerCount() - game.getAlivePlayerCount() + 1;
                return String.valueOf(rank);
            } else if (gamePlayer != null) {
                return String.valueOf(gamePlayer.getRank());
            }
            return "0";
        }

        // %pbr_game_state% - 游戏状态
        if (identifier.equals("game_state")) {
            return getGameStateDisplay(game.getState().name());
        }

        // %pbr_game_shrink_time% - 下次缩圈倒计时
        if (identifier.equals("game_shrink_time")) {
            return String.valueOf(plugin.getBorderShrinkManager().getNextShrinkCountdown(game));
        }

        // %pbr_game_border_size% - 当前边界大小
        if (identifier.equals("game_border_size")) {
            return String.valueOf(plugin.getBorderShrinkManager().getCurrentBorderSize(game));
        }

        // %pbr_game_is_alive% - 玩家是否存活 (true/false)
        if (identifier.equals("game_is_alive")) {
            return gamePlayer != null ? String.valueOf(gamePlayer.isAlive()) : "false";
        }

        return null; // 未知变量返回null
    }

    /**
     * 获取游戏状态显示名称
     */
    private String getGameStateDisplay(String state) {
        switch (state) {
            case "PREPARING":
                return "准备中";
            case "INVINCIBILITY":
                return "无敌阶段";
            case "PLAYING":
                return "游戏中";
            case "FINAL_STAGE":
                return "最终阶段";
            case "ENDING":
                return "结束中";
            case "FINISHED":
                return "已结束";
            default:
                return "未知";
        }
    }
}
