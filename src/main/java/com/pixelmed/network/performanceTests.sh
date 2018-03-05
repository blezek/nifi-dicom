#!/bin/sh

SCUTYPE="PIXELMED"	# OFFIS or PIXELMED
SCPTYPE="PIXELMED"	# OFFIS or PIXELMED

#OFFISSTORESCPOPTIONS="--ignore -pm -dhl +B"		# be careful there is a SCPRCVFOLDER if not --ignore
OFFISSTORESCPOPTIONS="-pm -dhl +B"

DEBUGLEVEL=0

SCPHOSTNAME=192.168.1.101
SCPAET=STORESCP
PORT=4006

SCUHOSTNAME=192.168.1.100
SCUAET=STORESCU

#scpMaxPDU=65536
#scpRcvBuf=0
scpSndBuf=0

scuMaxPDU=16384
scuRcvBuf=0
scuSndBuf=0

#repeatCount=10
repeatCount=3
assocnCount=3
syntaxCount=0
contextCount=0

#samples=10
samples=3

SCUTESTFOLDER=/tmp
SCUTESTFILE=$SCUTESTFOLDER/`basename $0`.$$.scu.dcm

SCPTESTFOLDER=/tmp
SCPTESTFILE=$SCPTESTFOLDER/`basename $0`.$$.scp.dcm
SCPRCVFOLDER=$SCUTESTFOLDER/`basename $0`.$$.received
#SCPRCVFOLDER=""		#ignore received images

SCPRCVLOGFILE=/tmp/`basename $0`.$$.log

echo "Log file to check for SCP errors is $SCPRCVLOGFILE"
rm -f "$SCPRCVLOGFILE"

echo "SCUTYPE=$SCUTYPE"
echo "SCPTYPE=$SCPTYPE"
echo "SCPRCVFOLDER=$SCPRCVFOLDER"
echo "OFFISSTORESCPOPTIONS=$OFFISSTORESCPOPTIONS"
echo "repeatCount=$repeatCount"
echo "assocnCount=$assocnCount"
echo "syntaxCount=$syntaxCount"
echo "contextCount=$contextCount"
echo "samples=$samples"


scp ./pixelmed.jar "${USER}@${SCUHOSTNAME}:$SCUTESTFOLDER/pixelmed.jar"
scp ./storescu "${USER}@${SCUHOSTNAME}:$SCUTESTFOLDER/storescu"
scp ./dicom.dic "${USER}@${SCUHOSTNAME}:$SCUTESTFOLDER/dicom.dic"

SCPWD=`pwd`

if [ $# = 0 ]
then
	echo "Making test file"
	#./dcsmpte -rows 4096 -columns 4096 -bits 12 "$SCPTESTFILE"
	./dcsmpte "$SCPTESTFILE"
	scp "$SCPTESTFILE" "${USER}@${SCUHOSTNAME}:$SCUTESTFILE"
	rm "$SCPTESTFILE"
	filelisttosend="$SCUTESTFILE"
else
	filelisttosend=""
	filecount=0
	for i in $*
	do
		#echo "Using file $i"
		filecount=`expr $filecount + 1`
		scp "$i" "${USER}@${SCUHOSTNAME}:$SCUTESTFILE.$filecount"
		filelisttosend="$filelisttosend $SCUTESTFILE.$filecount"
	done
fi
echo "Will send $filelisttosend"

if [ ! -z "$SCPRCVFOLDER" ]
then
	mkdir -p "$SCPRCVFOLDER"
fi

#for scpRcvBuf in 0 8192 16384 32768 65536 131072 262144 524288 1048576
for scpRcvBuf in 0
do
	#for scpMaxPDU in 4096 8192 16384 32768 65536 131072 262144 524288 1048576
	for scpMaxPDU in 16384 32768 65536
	do
		pids=`ps | grep "com.pixelmed.network.StorageSOPClassSCPDispatcher" | grep -v 'grep' | awk '{print $1}'`
		if [ ! -z "$pids" ]
		then
			kill $pids
			pids=`ps | grep "com.pixelmed.network.StorageSOPClassSCPDispatcher" | grep -v 'grep' | awk '{print $1}'`
			while [  ! -z "$pids" ]
			do
				sleep 1
				pids=`ps | grep "com.pixelmed.network.StorageSOPClassSCPDispatcher" | grep -v 'grep' | awk '{print $1}'`
			done
		fi
		pids=`ps | grep "storescp" | grep -v 'grep' | awk '{print $1}'`
		if [ ! -z "$pids" ]; then kill $pids; fi
		if [ "$SCPTYPE" = "PIXELMED" ]
		then
			java -Xms128m -Xmx512m -cp ./pixelmed.jar com.pixelmed.network.StorageSOPClassSCPDispatcher \
				"$PORT" "$SCPAET" "$scpMaxPDU" "$scpRcvBuf" "$scpSndBuf" \
				"$SCPRCVFOLDER" "$DEBUGLEVEL" >>"$SCPRCVLOGFILE" 2>&1 &
		elif [ "$SCPTYPE" = "OFFIS" ]
		then
			(cd "$SCPRCVFOLDER"; DCMDICTPATH="$SCPWD/dicom.dic" "$SCPWD/storescp" $OFFISSTORESCPOPTIONS -pdu "$scpMaxPDU" -aet "$SCPAET" "$PORT" >>"$SCPRCVLOGFILE" 2>&1 &)
		fi
		sample=0
		while [ $sample -lt $samples ]
		do
			echo "================== scpMaxPDU=$scpMaxPDU scpRcvBuf=$scpRcvBuf scpSndBuf=$scpSndBuf sample=$sample"
			if [ "$SCUTYPE" = "PIXELMED" ]
			then
				ssh -q "${USER}@${SCUHOSTNAME}" java -Xms128m -Xmx512m -cp "$SCUTESTFOLDER/pixelmed.jar" com.pixelmed.network.StorageSOPClassSCUPerformanceTest \
					"$SCPHOSTNAME" "$PORT" "$SCPAET" "$SCUAET" \
					"$scuMaxPDU" "$scuRcvBuf" "$scuSndBuf" \
					"$repeatCount" "$assocnCount" "$syntaxCount" "$contextCount" "$DEBUGLEVEL" \
					"$filelisttosend"
			elif [ "$SCUTYPE" = "OFFIS" ]
			then
				# assumes csh not sh at the other end wrt. environment variable
				# but do not use builtin csh time
				ssh -q "${USER}@${SCUHOSTNAME}" "setenv DCMDICTPATH $SCUTESTFOLDER/dicom.dic; /usr/bin/time -p $SCUTESTFOLDER/storescu" \
					-R -aec "$SCPAET" -aet "$SCUAET" -pdu "$scuMaxPDU" --repeat "$repeatCount" \
					"$SCPHOSTNAME" "$PORT" "$filelisttosend"
			fi
			sample=`expr $sample + 1`
		done
		pids=`ps | grep "com.pixelmed.network.StorageSOPClassSCPDispatcher" | grep -v 'grep' | awk '{print $1}'`
		if [ ! -z "$pids" ]; then kill $pids; fi
	done
done

ssh -q "${USER}@${SCUHOSTNAME}" rm "$filelisttosend $SCUTESTFOLDER/pixelmed.jar $SCUTESTFOLDER/storescu $SCUTESTFOLDER/dicom.dic"

if [ ! -z "$SCPRCVFOLDER" ]
then
	rm -rf "$SCPRCVFOLDER"
fi
