#!/bin/bash

client="219.228.148.154"

servers=(
    "219.228.148.45"
    "219.228.148.80"
    "219.228.148.89"
    "219.228.148.129"
    "219.228.148.178"
    "219.228.148.181"
    "219.228.148.222"
    "219.228.148.231"
)

printf "\n[compile]\n"
gradle remoteDeploy

printf "\n[deploy]\n"

printf "deploy in client  %-16s ..." ${client}
start=$(date +%s)
sshpass -p z scp -r ./build/install/library/* z@${client}:~/smartbft
end=$(date +%s)
take=$((end - start))
printf "\rdeploy in client  %-16s ok, take %ds\n" ${client} ${take}

id=0
for srv in ${servers[@]}; do
    printf "deploy in server%d %-16s ..." ${id} ${srv}
    start=$(date +%s)
    # scp -r z@${client}:~/smartbft z@${srv}:~/smartbft >/dev/null
    # ssh z@${client} "scp -r ~/smartbft z@${srv}:~/smartbft"
    sshpass -p z scp -r ./build/install/library/* z@${srv}:~/smartbft
    end=$(date +%s)
    take=$((end - start))
    printf "\rdeploy in server%d %-16s ok, take %ds\n" ${id} ${srv} ${take}
    ((++id))
done

# ./smartrun.sh bftsmart.benchmark.ThroughputLatencyClient 1 1 100 100000 true true

# ./smartrun.sh bftsmart.benchmark.ThroughputLatencyServer $smartbftid
