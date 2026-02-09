#!/bin/bash
cd "$(dirname "$0")"

# Ensure a writable/executable temp dir for JNA
TMPDIR="$(pwd)/tmp"
mkdir -p "$TMPDIR"

java -Xms2G -Xmx4G -Djava.io.tmpdir="$TMPDIR" -Djna.tmpdir="$TMPDIR" -Dio.netty.transport.noNative=true -jar mohist.jar nogui
