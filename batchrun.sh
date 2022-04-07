#!/bin/bash

if [ ! -d log ]; then
    mkdir log
fi

rm result.txt

for ((i = 1; i <= 12; i++)); do
    ./smartrun.sh bftsmart.benchmark.ThroughputLatencyClient 2022 ${i} 100 100000 true true 10 200 >log/${i}.log 2>&1
    cat log/${i}.log | tail -n 1 >>result.txt
done
