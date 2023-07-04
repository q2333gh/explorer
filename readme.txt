init the  project locally:
on ubuntu:(need sequential)
  1.mysql
  2.redis
    1.make sure stream is exist ,means the MQ is on.
      >>> XGROUP CREATE stream.orders g1 0 MKSTREAM
      preheat: use unit-test to add seckill voucher to redis
  3.tomcat:
    1.springboot
      2.check .yml  rename the sample.yml to : src/main/resources/application.yaml
          mysql,redis ip,port,passwd...
      3.you can package and run proj.jar

u can use maven package ,and get jar file
run the jar in background mode:
  nohup java -jar your-app.jar &
command explain:
  & : In your case, it means to run the command in the background and
    return the shell prompt immediately without waiting for the command to finish.
  nohup: used to tell a process to ignore any SIGHUP (hang up) signals that it receives.

if you want to read all the dependency source code and its doc.
use this cmd at ./ :
  mvn dependency:resolve dependency:resolve -Dclassifier=javadoc dependency:sources


windows nginx start:
at nginx.exe folder:
  start ./nginx.exe
check if nginx is on :
  Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess
kill nginx:
  Stop-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess -Force

