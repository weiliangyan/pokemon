package com.pokemonbr.listeners;

import com.pokemonbr.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * 幸运方块兼容性监听器
 * 确保幸运方块插件在游戏中正常工作
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class LuckyBlockListener implements Listener {

    private final Main plugin;

    public LuckyBlockListener(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * 监听方块破坏事件（低优先级，不干扰幸运方块插件）
     *
     * 注意：
     * 1. 使用 EventPriority.LOWEST 确保最早处理
     * 2. 不取消事件，只记录日志
     * 3. 让幸运方块插件正常处理方块破坏
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        // 检查玩家是否在游戏中
        if (plugin.getGameManager().isInGame(event.getPlayer())) {
            // 幸运方块插件会自动处理，这里只记录调试信息
            if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                plugin.getLogger().info("§7玩家 " + event.getPlayer().getName() +
                        " 在游戏中破坏方块: " + event.getBlock().getType());
            }
        }
    }
}
