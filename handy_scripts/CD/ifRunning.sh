#!/bin/bash
# shellcheck disable=SC2009
if ps aux | grep -q "[e]xplorer-test01"; then
  echo "running"
  else
    echo "not running"
  fi