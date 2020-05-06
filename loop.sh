#!/bin/sh  
while true  
do  
  docker run --name peapod-scraper --rm -v /Users/jwolinsky/code/peapod-scraper/log:/var/log/peapod peapod-scraper  
  sleep 90
done
