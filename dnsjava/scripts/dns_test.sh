echo "*************************************"
echo "JAVA DNS SERVER START SCRIPT" 
echo "*************************************"
echo `date`
USERNAME_REQUIRED="root"
if [ `whoami` != $USERNAME_REQUIRED ]; then
	echo "Not running as $USERNAME_REQUIRED"
	exit 1
fi
export DNSJAVA_CONTROL=/apps/dnsjava
nohup sh ${DNSJAVA_CONTROL}/dnsjava_run.sh > ${DNSJAVA_CONTROL}/dnsjava.log