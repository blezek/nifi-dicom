#!/bin/sh

PATHTOROOT="${HOME}/work/pixelmed/imgbook"

java  -Djava.awt.headless=true -Xms128m -Xmx512m -cp "${PATHTOROOT}/pixelmed.jar:${PATHTOROOT}/lib/additional/hsqldb.jar:${PATHTOROOT}/lib/additional/excalibur-bzip2-1.0.jar:${PATHTOROOT}/lib/additional/vecmath1.2-1.14.jar:${PATHTOROOT}/lib/additional/jmdns.jar:${PATHTOROOT}/lib/additional/commons-codec-1.3.jar" com.pixelmed.server.DicomAndWebStorageServer ${PATHTOROOT}/com/pixelmed/server/graymax.properties
