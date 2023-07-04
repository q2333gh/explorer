#!/bin/bash
##########mvn pkg before deploy.
#the symbol #! combination at the beginning of the first line of a script is called a shebang
#  mechanism: in bash we run xx.sh , the shell interpret the xx.sh file and exec them.
#  how to impl the shell function?, well , goto the OS course~

#scp:secure copy, copy file across network between linux machines.
#   base on SSH protocol
#   u can use <#tldr scp >to check it out

scp \
    ./run/* \
    ./readme* \
    ./target/explorer-test01.jar \
    root@<your-ip>:/root/explorer
#also  this cmd need setup ssh . i didnt check it for you .

#in sh , we must use \ to start break a line for ez for human read.
#   *it will auto cat together into 1 line* when exec
#   also can`t have comments in it . at least it didnt work for me

