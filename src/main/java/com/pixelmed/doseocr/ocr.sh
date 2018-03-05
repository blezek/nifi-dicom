#!/bin/sh
#
# usage: ./ocr.sh dosescreenfile [outputdosesrfile [pathtoimagefiles]]
#

dosescreenfile="$1"
outputdosesrfile="$2"
pathtoimagefiles="$3"

java -Xmx512m -cp .:./pixelmed.jar -Djava.awt.headless=true OCR ${dosescreenfile} ./OCR_Glyphs_GEDoseScreen.xml - ${outputdosesrfile} ${pathtoimagefiles}

