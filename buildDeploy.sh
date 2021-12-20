#!/usr/bin/env bash
./build.sh

docker tag metis-server:0.3 qopbot/metis-server:0.3
docker push qopbot/metis-server:0.3
