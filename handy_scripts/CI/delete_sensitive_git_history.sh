#todo:
# i have a idea : someone accidentally upload password in git history.

#input: a filename , contains passwd
#function: delete all this file git history.
#output:  tell true or false if deleted,

#impl: cmd may need:
#delete history:
  #git filter-branch --index-filter "git rm -rf --cached --ignore-unmatch *.yaml" HEAD
  #git filter-branch --index-filter 'git ls-files -z | grep -z -E "RedissonConfig.java$" | xargs -0 -r git rm -rf --cached --ignore-unmatch' HEAD
#check new history
  #git log -p --follow -- src/main/java/com/explorer/config/RedissonConfig.java > ./rc.log
#bash cmd to check contains certain "123456" like passwd.


#usefull cmds:
# git reflog expire --expire=now --all && git gc --prune=now --aggressive
# java -jar /mnt/d/T/1_code/Explorer/tools/bfg-1.14.0.jar --delete-files RedissonConfig.java /mnt/d/T/1_code/Explorer

