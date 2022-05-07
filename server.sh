#!/bin/bash

ips=(`cat config/hosts.config | grep -v "#" | grep "219" | awk '{print $2}'`)
smartbftid=0

for ip in ${ips[@]}; do
    if [ "$ip" == "$myhost" ]; then 
        break
    fi
    ((smartbftid++))
done

rm -rf config/currentView

sleepTime=0
if [ $# -gt 0 ]; then
    sleepTime=$1
fi

echo "== myhost=$myhost"
echo "== smartbftid=$smartbftid"
echo "== sleepTime=${sleepTime}ms"
echo ""
echo ">>> ./smartrun.sh bftsmart.benchmark.ThroughputLatencyServer $smartbftid $sleepTime"
echo ""
./smartrun.sh bftsmart.benchmark.ThroughputLatencyServer $smartbftid $sleepTime
