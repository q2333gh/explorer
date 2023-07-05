#!/bin/bash
echo ----------staus--------------
git status
echo ----------add .--------------
git add .   # add all files in the current directory and its subdirectories
echo ----------commit-------------
git commit -m "$1" # use the first argument as the commit message
echo ----------push---------------
  git push -u origin master #the push-operation need to config remote address.
  # i didn`t check it for you in the script
echo ----------status-------------
git status
echo ----------done-------------------

#todo:
#git pull origin master
#should i pull the newest from remote firstly ?


# todo:
# display push status instantly , not wait for buffer in cmd output
#which means , if push need a bit more network. i must after push finish to know the procedure
#if procedure stucked due to network , i cant see it .
#if im not using .sh file . but directly git push cmd in terminal . i can see the procedure in real-time
