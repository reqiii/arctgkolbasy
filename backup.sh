#!/bin/bash
TOKEN=$1
CHAT=$2
backup_date=$(date +%Y-%m-%d_%H_%M_%S)

docker exec -t ArctgKolbasyDB pg_dumpall -c -U postgres  > /home/ubuntu/dump_$backup_date.sql

curl -F document=@/home/ubuntu/dump_$backup_date.sql https://api.telegram.org/bot$TOKEN/sendDocument?chat_id=$CHAT
