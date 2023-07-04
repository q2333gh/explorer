#!/bin/bash
nohup java -jar explorer-test01.jar &
sleep 1
# shellcheck disable=SC2009
if ps aux | grep -q "[e]xplorer-test01"; then
  echo "explorer-test01 running success!"
fi

