package com.pokemonbr.listeners;

import com.pokemonbr.Main;
import com.pokemonbr.models.Game;
import com.pokemonbr.models.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 玩家事件监听器
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class PlayerListener implements Listener {

    private final Main plugin;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * 监听玩家退出服务器
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // 检查是否在队列中
        if (plugin.getQueueManager().isInQueue(player)) {
            plugin.getQueueManager().leaveQueue(player);
        }

        // 检查是否在游戏中
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game != null) {
            // 检查玩家是否存活
            if (game.isPlayerAlive(player.getUniqueId())) {
                // 根据配置判断是否淘汰
                boolean shouldEliminate = shouldEliminateOnDisconnect(game);

                if (shouldEliminate) {
                    // 淘汰玩家
                    game.eliminatePlayer(player.getUniqueId(), null);

                    // 广播消息
                    String message = plugin.getConfigManager().getMessagesConfig()
                            .getString("elimination.eliminated-disconnect", "{player} 因掉线而被淘汰");
                    game.broadcastMessage(message.replace("{player}", player.getName()));
                }
            }
        }

        // 清除邀请记录
        plugin.getInviteManager().clearInvites(player);

        // 保存玩家数据
        plugin.getPlayerDataManager().removePlayerCache(player.getUniqueId());
    }

    /**
     * 判断掉线是否应该淘汰
     */
    private boolean shouldEliminateOnDisconnect(Game game) {
        if (game.getState() == GameState.FINAL_STAGE) {
            return plugin.getConfig().getBoolean("elimination.final-stage.disconnect", true);
        } else {
            return plugin.getConfig().getBoolean("elimination.early-game.disconnect", false);
        }
    }
}
