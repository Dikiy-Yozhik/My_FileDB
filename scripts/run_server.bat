@echo off
echo Starting My_FileDB HTTP Server...
java -cp "bin;backend\lib\*" api.HttpServer
pause