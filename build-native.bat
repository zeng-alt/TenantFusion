@echo off
echo ========================================================
echo Setting up Visual Studio C++ Environment for GraalVM...
echo ========================================================
call "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat"

echo.
echo ========================================================
echo Stopping old Gradle Daemons to ensure clean environment...
echo ========================================================
call gradlew --stop

echo.
echo ========================================================
echo Starting Native Compilation...
echo ========================================================
call gradlew :backend:admin:nativeCompile %*

if %ERRORLEVEL% equ 0 (
    echo.
    echo ========================================================
    echo SUCCESS: Native image compiled successfully!
    echo You can find the .exe file in: backend\admin\build\native\nativeCompile\
    echo ========================================================
) else (
    echo.
    echo ========================================================
    echo FAILED: Native compilation failed. Please check the logs above.
    echo ========================================================
)
