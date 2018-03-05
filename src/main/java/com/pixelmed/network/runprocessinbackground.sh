#!/bin/sh

if [ $# -le 3 ]
then
	echo "Usage: "`basename "${0}"`" pidfile stdoutfile stderrfile command [arguments]"
	exit 1
fi

processidfile="${1}"
shift
stdoutfile="${1}"
shift
stderrfile="${1}"
shift
#command="${1}"

rm -f "${processidfile}"

#echo $*
$* >"${stdoutfile}" 2>"${stderrfile}" &

# head -1 is because sometimes a spurious second process is listed
ps -o pid,ppid,command | grep " $$ " | egrep -v '(sh|grep)' | head -1 | awk '{print $1}' >"${processidfile}"
#ps -o pid,ppid,command | grep " $$ " | grep "${command}" | head -1 | awk '{print $1}' >"${processidfile}"

exit 0
