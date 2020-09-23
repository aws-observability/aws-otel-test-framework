#! /bin/bash

nohup bash -c "while true; do curl http://localhost:4567/span0; sleep 2; done &"
nohup bash -c "while true; do curl http://localhost:4567/span400; sleep 5; done &"
nohup bash -c "while true; do curl http://localhost:4567/span500; sleep 10; done &"
/app/bin/TraceEmitter