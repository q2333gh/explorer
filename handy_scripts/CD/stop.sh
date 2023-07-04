#!/bin/bash
#if ps aux | grep -q "[e]xplorer-test01"; then
  pkill -f "java.*explorer-test01.jar"
  sleep 1
./ifRunning.sh

#<grep -q> explanation:
#  The -q option for the grep command stands for “quiet” or “silent” mode. 
# When this option is used, grep will not output any matching lines to the standard output.
#  Instead, it will simply return an exit status indicating whether or not it found any matches.


#<fi> stands for finish-if? no . its kind of funny:
#fi is not an abbreviation or acronym, it is simply the word if *spelled backwards*.
# In a shell script, fi is used to mark the end of an if statement.
#developed by Stephen Bourne at Bell Labs in the late 1970s
#acronym:首字母缩写

#<[e]>  explanation:
# in the grep command is used to prevent the grep command itself from appearing in the ps output.
 #When you run the ps aux | grep "explorer-test01" command, the grep process itself will appear in the ps output because its command line contains the search pattern "explorer-test01". This can cause the script to incorrectly report that the explorer-test01.jar Java program is running, even if it’s not.
# here is a example shell output to window:
#root@hecs-25426 ~/explorer# ps aux | grep  "[e]xplorer-test01"
 #root      169194  1.8 10.7 2518480 217344 pts/7  Sl   16:11   0:13 java -jar explorer-test01.jar
 #root      169817 61.6 10.9 2503064 222336 pts/8  Sl   16:24   0:12 java -jar explorer-test01.jar
 #root@hecs-25426 ~/explorer# ps aux | grep  "explorer-test01"
 #root      169194  1.7 10.7 2518480 217344 pts/7  Sl   16:11   0:13 java -jar explorer-test01.jar
 #root      169817 48.1 10.9 2503064 222488 pts/8  Sl   16:24   0:12 java -jar explorer-test01.jar
 #root      170013  0.0  0.0   9032   720 pts/8    S+   16:24   0:00 grep --color=auto explorer-test01

