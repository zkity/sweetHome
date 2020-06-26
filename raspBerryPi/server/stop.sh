#!/bin/bash

ps -aux|grep python3| grep -v grep | awk '{print $2}'| xargs kill -9
