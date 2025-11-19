# ==========================================
#   PokemonBattleRoyale - LuckPerms 权限组配置
#   完整的 LuckPerms 权限组配置指南
# ==========================================

## 权限组配置

### 基础组配置

#### default 组（默认玩家）
```
/lp group default set displayname "&7[玩家]"
/lp group default set meta.prefix "&7[玩家]&r"
/lp group default permission set pbr.player.join true
/lp group default permission set pbr.player.leave true
/lp group default permission set pbr.player.lobby true
/lp group default permission set pbr.player.spectate true
/lp group default permission set pbr.player.stats true
/lp group default permission set pbr.player.playlimit.default true
```

#### vip 组（VIP玩家）
```
/lp group vip set displayname "&6[VIP]"
/lp group vip set meta.prefix "&6[VIP]&r"
/lp group vip permission set pbr.player.join true
/lp group vip permission set pbr.player.leave true
/lp group vip permission set pbr.player.lobby true
/lp group vip permission set pbr.player.spectate true
/lp group vip permission set pbr.player.stats true
/lp group vip permission set pbr.player.invite true
/lp group vip permission set pbr.player.queue.custom true
/lp group vip permission set pbr.vip.plays.10 true
/lp group vip parent default
```

#### vip_plus 组（高级VIP）
```
/lp group vip_plus set displayname "&e[VIP+]"
/lp group vip_plus set meta.prefix "&e[VIP+]&r"
/lp group vip_plus permission set pbr.player.join true
/lp group vip_plus permission set pbr.player.leave true
/lp group vip_plus permission set pbr.player.lobby true
/lp group vip_plus permission set pbr.player.spectate true
/lp group vip_plus permission set pbr.player.stats true
/lp group vip_plus permission set pbr.player.invite true
/lp group vip_plus permission set pbr.player.queue.custom true
/lp group vip_plus permission set pbr.vip.plays.20 true
/lp group vip_plus parent vip
```

#### mvp 组（高级玩家）
```
/lp group mvp set displayname "&b[MVP]"
/lp group mvp set meta.prefix "&b[MVP]&r"
/lp group mvp permission set pbr.player.join true
/lp group mvp permission set pbr.player.leave true
/lp group mvp permission set pbr.player.lobby true
/lp group mvp permission set pbr.player.spectate true
/lp group mvp permission set pbr.player.stats true
/lp group mvp permission set pbr.player.invite true
/lp group mvp permission set pbr.player.queue.custom true
/lp group mvp permission set pbr.vip.plays.30 true
/lp group mvp parent vip_plus
```

#### staff 组（工作人员）
```
/lp group staff set displayname "&2[Staff]"
/lp group staff set meta.prefix "&2[Staff]&r"
/lp group staff permission set pbr.player.join true
/lp group staff permission set pbr.player.leave true
/lp group staff permission set pbr.player.lobby true
/lp group staff permission set pbr.player.spectate true
/lp group staff permission set pbr.player.stats true
/lp group staff permission set pbr.player.invite true
/lp group staff permission set pbr.player.queue.custom true
/lp group staff permission set pbr.vip.plays.unlimited true
/lp group staff parent mvp
```

#### moderator 组（管理员）
```
/lp group moderator set displayname "&3[MOD]"
/lp group moderator set meta.prefix "&3[MOD]&r"
/lp group moderator permission set pbr.admin true
/lp group moderator permission set pbr.admin.lootgui true
/lp group moderator permission set pbr.admin.lootgui.youpin true
/lp group moderator permission set pbr.admin.lootgui.jipin true
/lp group moderator permission set pbr.admin.lootgui.putong true
/lp group moderator parent staff
```

#### admin 组（管理员）
```
/lp group admin set displayname "&4[Admin]"
/lp group admin set meta.prefix "&4[Admin]&r"
/lp group admin permission set "*"
/lp group admin set weight 100
/lp group admin parent moderator
```

### 特殊权限组

#### freeplays 组（免费游玩权限）
```
/lp group freeplays permission set pbr.freeplays true
/lp group freeplays parent default
```

#### queue_vip 组（VIP队列权限）
```
/lp group queue_vip permission set pbr.player.queue.custom true
/lp group queue_vip permission set pbr.vip.queue true
/lp group queue_vip parent default
```

### 权限设置命令参考

#### 基础命令
```
/lp group <组名> permission set <权限> <true/false>    # 设置权限
/lp group <组名> parent set <父组>                   # 设置父组
/lp user <玩家> parent set <组名>                       # 设置玩家组
/lp user <玩家> permission set <权限> <true/false>   # 设置玩家权限
/lp group <组名> set meta.prefix "<前缀>"            # 设置前缀
/lp group <组名> set displayname "<显示名>"          # 设置显示名
```

#### 示例
```
# 给玩家添加VIP权限
/lp user Steve parent set vip

# 给玩家添加管理员权限
/lp user Alex parent set admin

# 设置VIP组继承权限
/lp group vip parent default

# 设置自定义前缀
/lp group vip set meta.prefix "&6[VIP]&r"

# 设置权重（数字越大，显示越靠前）
/lp group admin set weight 100
/lp group vip set weight 50
```

## 游戏内权限测试

### 测试命令
```
/pbr test-permission <权限>        # 测试权限
/pbr test-group <组名>             # 测试组权限
/pbr test-vip                    # 测试VIP状态
/pbr test-admin                  # 测试管理员状态
```

### 检查玩家权限
```
/lp user <玩家>               # 查看玩家信息
/lp user <玩家> permission list # 查看玩家权限列表
/lp user <玩家> parent list    # 查看玩家组继承
```

## 配置文件

### luckperms.yml 配置示例
```yaml
# LuckPerms 基础配置
servers:
  minecraft:
    # 默认组
    default-group: default
    # 权限组优先级
    group-weight:
      default: 1
      vip: 10
      vip_plus: 20
      mvp: 30
      staff: 40
      moderator: 50
      admin: 60

# 权限组继承设置
groups:
  default:
    permissions:
    - "pbr.player.join"
    - "pbr.player.leave"
    - "pbr.player.lobby"

  vip:
    inherits:
      - default
    permissions:
    - "pbr.player.invite"
    - "pbr.player.queue.custom"
    - "pbr.vip.plays.10"

# 上下文设置（可选）
contexts:
  world:
    - "world=world_pbr_*"
```

## 实际使用建议

### 1. 设置默认组
```
/lp group default permission set pbr.player.join true
/lp group default permission set pbr.player.leave true
```

### 2. 创建VIP权限组
```
/lp group create vip
/lp group vip set displayname "&6[VIP]"
/lp group vip permission set pbr.player.join true
/lp group vip permission set pbr.vip.plays.10 true
/lp group vip parent default
```

### 3. 创建管理员组
```
/lp group create admin
/lp group admin set displayname "&4[管理员]"
/lp group admin permission set "*"
/lp group admin set weight 100
```

### 4. 批量设置权限
```
# 为组设置所有基础权限
/lp group vip permission set pbr.player.join true
/lp group vip permission set pbr.player.leave true
/lp group vip permission set pbr.player.lobby true
/lp group vip permission set pbr.player.spectate true
/lp group vip permission set pbr.player.stats true
```

### 5. 权限继承示例
```
# 设置权限组继承关系
/lp group vip_plus parent vip        # vip_plus 继承 vip
/lp group mvp parent vip_plus      # mvp 继承 vip_plus
/lp group staff parent mvp         # staff 继承 mvp
/lp group moderator parent staff   # moderator 继承 staff
/lp group admin parent moderator   # admin 继承 moderator
```

## 注意事项

1. **权重设置**: 使用 `/lp group <组名> set weight <数字>` 设置组权重
2. **前缀颜色**: 使用 Minecraft 颜色代码 (&a, &b, &c, &d, &e, &f, &1-9)
3. **权限继承**: 子组会自动继承父组的所有权限
4. **实时生效**: 大部分权限修改会实时生效
5. **备份数据**: 修改前建议备份 LuckPerms 数据

## 常见问题

### Q: 如何检查 LuckPerms 是否正常工作？
A: 使用命令 `/lp version` 查看版本信息

### Q: 权限不生效怎么办？
A: 检查玩家组归属: `/lp user <玩家> parent list`
   检查权限列表: `/lp user <玩家> permission list`

### Q: 如何设置临时权限？
A: 使用命令 `/lp user <玩家> permission settemp <权限> <时间>`

### Q: 如何设置权重？
A: 使用命令 `/lp group <组名> set weight <数字>`

## 更新时间
- 2025年01月19日: 创建初始配置
- 支持 PokemonBattleRoyale v1.0.0