@echo off
echo 正在推送 PokemonBattleRoyale 插件到 GitHub...
echo.

cd /d "D:\project\PokemonBattleRoyale"

echo 当前Git状态:
git status
echo.

echo 正在推送到远程仓库...
git push -u origin main

if %ERRORLEVEL% == 0 (
    echo.
    echo ✅ 推送成功！
    echo 📁 项目已上传到: https://github.com/weiliangyan/pokemon
) else (
    echo.
    echo ❌ 推送失败，请检查权限或手动推送
    echo 💡 手动推送命令: git push -u origin main
)

echo.
pause