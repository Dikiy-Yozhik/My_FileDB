@echo off
echo Starting tests...

echo ===== EmployeeTest =====
java -cp bin backend.test.EmployeeTest

echo.
echo ===== SerializationUtilTest =====  
java -cp bin backend.test.SerializationUtilTest

echo.
echo ===== FileManagerTest =====
java -cp bin backend.test.FileManagerTest

echo.
echo Testing is complete!
pause