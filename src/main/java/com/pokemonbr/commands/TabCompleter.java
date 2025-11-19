package com.pokemonbr.commands;

import com.pokemonbr.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab补全处理器
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class TabCompleter implements org.bukkit.command.TabCompleter {

    private final Main plugin;

    public TabCompleter(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmdName = command.getName().toLowerCase();

        switch (cmdName) {
            case "pbr":
                return handlePbrTab(sender, args);

            case "pbradmin":
                return handleAdminTab(sender, args);

            case "pbrstats":
            case "pbrspectate":
                return handlePlayerNameTab(args);

            case "pbrinvite":
                return handlePlayerNameTab(args);

            default:
                return new ArrayList<>();
        }
    }

    /**
     * 处理 /pbr 的Tab补全
     */
    private List<String> handlePbrTab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // 第一级子指令
            List<String> subCommands = Arrays.asList(
                    "join", "leave", "lobby", "stats", "spectate", "help"
            );
            return filterStartsWith(subCommands, args[0]);
        } else if (args.length == 2) {
            // 第二级参数
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("stats") || subCmd.equals("spectate")) {
                return getOnlinePlayerNames(args[1]);
            }
        }

        return new ArrayList<>();
    }

    /**
     * 处理 /pbradmin 的Tab补全
     */
    private List<String> handleAdminTab(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pbr.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            // 第一级子指令
            List<String> subCommands = Arrays.asList(
                    "start", "stop", "reload", "setlobby", "backup", "restore"
            );
            return filterStartsWith(subCommands, args[0]);
        } else if (args.length == 2) {
            // 第二级参数
            String subCmd = args[0].toLowerCase();

            if (subCmd.equals("start")) {
                // 队列ID补全
                return Arrays.asList("1", "2", "3");
            } else if (subCmd.equals("backup") || subCmd.equals("restore")) {
                // 世界名补全
                return Bukkit.getWorlds().stream()
                        .map(world -> world.getName())
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }

    /**
     * 处理玩家名称补全
     */
    private List<String> handlePlayerNameTab(String[] args) {
        if (args.length == 1) {
            return getOnlinePlayerNames(args[0]);
        }
        return new ArrayList<>();
    }

    /**
     * 获取在线玩家名称列表
     */
    private List<String> getOnlinePlayerNames(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * 过滤以指定前缀开头的字符串
     */
    private List<String> filterStartsWith(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
