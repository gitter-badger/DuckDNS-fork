#!/bin/bash 
echo Starting the updater
while true; do
	echo Updating
	curl -k -o /home/douglas/duckdns/duck.log "https://www.duckdns.org/update?domains=exampledomain&token=a7c4d0ad-114e-40ef-ba1d-d217904a50f2&ip="
	sleep 5m
done