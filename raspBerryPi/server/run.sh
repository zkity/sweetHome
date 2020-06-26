#!/bin/bash

nohup uvicorn main:app --host 0.0.0.0 --reload > ./log/run.log 2>&1 &
