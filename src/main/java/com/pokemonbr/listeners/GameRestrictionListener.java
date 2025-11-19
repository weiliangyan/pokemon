package com.pokemonbr.listeners;

import com.pokemonbr.Main;
import com.pokemonbr.models.Game;
import com.pokemonbr.models.GameState;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.spigotmc.event.entity.EntityMountEvent;

/**
 * 游戏限制监听器
 * 处理游戏中的各种限制（PVP、飞行、骑乘、无敌等）
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class GameRestrictionListener implements Listener {

    private final Main plugin;

    public GameRestrictionListener(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * 处理玩家受到伤害
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        Game game = plugin.getGameManager().getPlayerGame(player);

        if (game == null) {
            return;
        }

        // 检查全局无敌设置
        if (plugin.getConfig().getBoolean("global.invincible-enabled", false)) {
            event.setCancelled(true);
            return;
        }

        // 检查无敌时间
        if (game.getState() == GameState.INVINCIBILITY) {
            event.setCancelled(true);
            return;
        }

        // 检查是否已被淘汰（观战模式）
        if (!game.isPlayerAlive(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // 检查是否在边界外受到伤害
        if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION ||
            event.getCause() == EntityDamageEvent.DamageCause.CUSTOM) {
            // 允许边界外伤害
            return;
        }
    }

    /**
     * 处理PVP伤害
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        Game game = plugin.getGameManager().getPlayerGame(victim);

        if (game == null) {
            return;
        }

        // 检查攻击者是否在同一游戏
        Game attackerGame = plugin.getGameManager().getPlayerGame(attacker);
        if (attackerGame == null || !attackerGame.getGameUuid().equals(game.getGameUuid())) {
            event.setCancelled(true);
            return;
        }

        // 检查全局PVP设置
        if (!plugin.getConfig().getBoolean("global.pvp-enabled", true)) {
            event.setCancelled(true);
            attacker.sendMessage(plugin.getConfigManager().getMessagesConfig()
                    .getString("restrictions.pvp-disabled", "§cPVP已被禁用"));
            return;
        }

        // 检查无敌时间
        if (game.getState() == GameState.INVINCIBILITY) {
            event.setCancelled(true);
            attacker.sendMessage(plugin.getConfigManager().getMessagesConfig()
                    .getString("restrictions.invincibility-active", "§c无敌时间内无法攻击"));
            return;
        }

        // 检查双方是否都存活
        if (!game.isPlayerAlive(victim.getUniqueId()) || !game.isPlayerAlive(attacker.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
    }

    /**
     * 处理飞行
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerFly(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        // 创造模式可以飞行
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        Game game = plugin.getGameManager().getPlayerGame(player);

        if (game == null) {
            return;
        }

        // 检查全局飞行设置
        if (!plugin.getConfig().getBoolean("global.fly-enabled", false)) {
            event.setCancelled(true);
            player.setAllowFlight(false);
            player.setFlying(false);

            if (event.isFlying()) {
                player.sendMessage(plugin.getConfigManager().getMessagesConfig()
                        .getString("restrictions.fly-disabled", "§c游戏中禁止飞行"));
            }
        }
    }

    /**
     * 处理骑乘实体（包括宝可梦）
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityMount(EntityMountEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        Game game = plugin.getGameManager().getPlayerGame(player);

        if (game == null) {
            return;
        }

        Entity mount = event.getMount();

        // 检查是否是宝可梦（通过类名判断）
        String mountClassName = mount.getClass().getName();
        boolean isPokemon = mountClassName.contains("pixelmon") ||
                           mountClassName.contains("Pixelmon") ||
                           mountClassName.contains("EntityPixelmon");

        if (isPokemon) {
            // 检查骑乘宝可梦设置
            if (!plugin.getConfig().getBoolean("global.ride-pokemon-enabled", true)) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getMessagesConfig()
                        .getString("restrictions.ride-disabled", "§c游戏中禁止骑乘宝可梦"));
            }
        }
    }

    /**
     * 处理边界外伤害
     */
    @EventHandler
    public void onBorderDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        Game game = plugin.getGameManager().getPlayerGame(player);

        if (game == null) {
            return;
        }

        // 只在PLAYING或FINAL_STAGE状态检查边界
        if (game.getState() != GameState.PLAYING && game.getState() != GameState.FINAL_STAGE) {
            return;
        }

        // 如果玩家在边界外，造成持续伤害
        if (plugin.getBorderShrinkManager().isPlayerOutsideBorder(player)) {
            // 这个伤害由定时任务处理，这里只是检测
        }
    }

    /**
     * 处理方块破坏
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);

        if (game == null) {
            return;
        }

        // 获取世界配置
        org.bukkit.configuration.file.FileConfiguration worldConfig =
            plugin.getWorldConfigManager().getWorldConfig(game.getWorldConfigName());

        // 检查是否允许破坏方块
        boolean allowBreaking;
        if (worldConfig != null && worldConfig.contains("special-rules.allow-block-breaking")) {
            allowBreaking = worldConfig.getBoolean("special-rules.allow-block-breaking", true);
        } else {
            // 使用全局配置
            allowBreaking = plugin.getConfig().getBoolean("global.allow-block-breaking", true);
        }

        if (!allowBreaking) {
            event.setCancelled(true);
            if (player.getGameMode() != GameMode.CREATIVE) {
                player.sendMessage("§c此地图禁止破坏方块！");
            }
        }
    }

    /**
     * 处理方块放置
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);

        if (game == null) {
            return;
        }

        // 获取世界配置
        org.bukkit.configuration.file.FileConfiguration worldConfig =
            plugin.getWorldConfigManager().getWorldConfig(game.getWorldConfigName());

        // 检查是否允许放置方块
        boolean allowPlacing;
        if (worldConfig != null && worldConfig.contains("special-rules.allow-block-placing")) {
            allowPlacing = worldConfig.getBoolean("special-rules.allow-block-placing", true);
        } else {
            // 使用全局配置
            allowPlacing = plugin.getConfig().getBoolean("global.allow-block-placing", true);
        }

        if (!allowPlacing) {
            event.setCancelled(true);
            if (player.getGameMode() != GameMode.CREATIVE) {
                player.sendMessage("§c此地图禁止放置方块！");
            }
        }
    }
}
