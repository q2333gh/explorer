todo:
typography problem of .md file
I cant display space after .md rendered.
I NEED space before some newline!

## steps to boot up the project :

### on ubuntu:(need sequential)

1.mysql installed and running  
2.redis installed and running  
1.make sure stream is exist ,means the MQ is on.  
redis-cli: XGROUP CREATE stream.orders g1 0 MKSTREAM  
3.tomcat:  
1.springboot  
2.check .yaml rename the application-public.yml to :   src/main/resources/application.yaml  
check contents: mysql,redis ip,port,passwd...  
3.you can package and run proj.jar  
use mvn cmd.

u can use maven package ,and get jar file

#### run the jar in background mode:

nohup java -jar your-app.jar &

##### command explain:

& : In your case, it means to run the command in the background and
return the shell prompt immediately without waiting for the command to finish.  
nohup: used to tell a process to ignore any SIGHUP (hang up) signals that it receives.

#### study-use::joy:

if you want to read all the dependency source code and its doc.
use this cmd at ./ :  
mvn dependency:resolve dependency:resolve -Dclassifier=javadoc dependency:sources

#### windows nginx start:

at nginx.exe folder:  
start ./nginx.exe  
check if nginx is on :  
Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess  
kill nginx:  
Stop-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess -Force  
prefer: ./nginx.exe -s stop   
because somehow if force kill nginx by windows. it will automatically reboot itself.

### upgrade dependencies(libs)

simply click the refresh bottom in IDEA-pom.xml-editorArea-topRight

## Bugs remaining now:🕵️💡

( a challenge to overcome and transcend,)

the project use StringRedisTemplate base on lettuce(生菜)    
and the lettuce offer a pool to call redis-server connection resource,  
**user can require connection, but must return after use**:  
if the user don't ret , may cause mem-leak  
(probably not , the lib itself should handle it ?)  
**but more important: new users need connections from the pool! **  
so far,this msg just show up when program stop.  
but may cause problem! take your proj as 一期一会

58:03.607 WARN 22108 --- [extShutdownHook] d.r.c.l. LettucePoolingConnectionProvider :
LettucePoolingConnectionProvider contains unreleased connections
Unable to connect to Redis; nested exception is org.
springframework.data.redis.connection.PoolException: Could not get a resource from the pool;  




  
