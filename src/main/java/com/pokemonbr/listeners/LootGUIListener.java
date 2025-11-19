package com.pokemonbr.listeners;

import com.pokemonbr.managers.LootGUIManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * 物品管理GUI监听器
 * 监听GUI关闭事件并自动保存物品
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class LootGUIListener implements Listener {

    private final LootGUIManager guiManager;

    public LootGUIListener(LootGUIManager guiManager) {
        this.guiManager = guiManager;
    }

    /**
     * 监听GUI关闭事件
     * 当玩家关闭物品管理GUI时自动保存物品到配置文件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        // 检查玩家是否正在编辑物品管理GUI
        if (!guiManager.isEditingGUI(player)) {
            return;
        }

        // 自动保存GUI中的物品到配置文件
        guiManager.saveGUI(player, event.getInventory());
    }
}
