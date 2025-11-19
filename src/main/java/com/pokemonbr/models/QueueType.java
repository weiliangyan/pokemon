package com.pokemonbr.models;

/**
 * 队列类型枚举
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public enum QueueType {
    /**
     * 普通队列 (计分模式)
     * - 消耗游玩次数
     * - 影响排名积分
     * - 无需权限
     */
    NORMAL("normal", "普通队列", "ranked", ""),

    /**
     * VIP队列 (娱乐模式)
     * - 不消耗游玩次数
     * - 不影响排名积分
     * - 需要VIP权限
     */
    VIP("vip", "VIP队列", "casual", "pbr.vip.queue"),

    /**
     * 管理员队列 (娱乐模式)
     * - 不消耗游玩次数
     * - 不影响排名积分
     * - 需要管理员权限
     */
    ADMIN("admin", "管理员队列", "casual", "pbr.admin.queue");

    private final String configKey;
    private final String displayName;
    private final String gameMode;
    private final String permission;

    QueueType(String configKey, String displayName, String gameMode, String permission) {
        this.configKey = configKey;
        this.displayName = displayName;
        this.gameMode = gameMode;
        this.permission = permission;
    }

    /**
     * 获取配置文件中的键名
     * @return 配置键
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * 获取显示名称
     * @return 显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取游戏模式
     * @return ranked=计分模式, casual=娱乐模式
     */
    public String getGameMode() {
        return gameMode;
    }

    /**
     * 获取权限节点
     * @return 权限节点 (空字符串表示无需权限)
     */
    public String getPermission() {
        return permission;
    }

    /**
     * 是否为计分模式
     * @return 是否计分
     */
    public boolean isRanked() {
        return "ranked".equals(gameMode);
    }

    /**
     * 是否需要权限
     * @return 是否需要权限
     */
    public boolean requiresPermission() {
        return !permission.isEmpty();
    }

    /**
     * 根据配置键名获取队列类型
     * @param configKey 配置键
     * @return 队列类型 或 null
     */
    public static QueueType fromConfigKey(String configKey) {
        for (QueueType type : values()) {
            if (type.configKey.equalsIgnoreCase(configKey)) {
                return type;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return displayName + " [" + gameMode + "]";
    }
}
