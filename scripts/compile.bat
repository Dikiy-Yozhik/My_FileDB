@echo off
echo Compilation of My_FileDB...

mkdir bin 2>nul

javac -d bin ^
      backend\src\core\*.java ^
      backend\src\api\*.java ^
      backend\src\api\controllers\*.java ^
      backend\src\api\dto\*.java ^
      backend\src\exceptions\*.java ^
      backend\src\model\*.java ^
      backend\src\storage\*.java ^
      backend\src\util\*.java ^
      backend\test\*.java

if %errorlevel% equ 0 (
    echo ✅ Success compilation!
) else (
    echo ❌ Compilation error!
    exit /b 1
)