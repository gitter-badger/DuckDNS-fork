echo "*************************************"
echo "JETTY-DUCK-DNS" 
echo "*************************************"
USERNAME_REQUIRED="jetty"
if [ `whoami` != $USERNAME_REQUIRED ]; then
	echo "Not running as $USERNAME_REQUIRED"
	exit 1
fi
echo "*** UPDATING HOST PROPERTIES"
/apps/scripts/update_host_info.sh
cat /apps/jetty/jetty_dev/resources/environment.properties
echo "*** DONE"
export JAVA_HOME=/apps/java/jdk_current
export JETTY_HOME=/apps/jetty/jetty_dev
export JAVA_OPTIONS="-d64 -server -Xms100m -Xmx200m -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5555"
export JETTY_PORT=8090
export JETTY_RUN=${JETTY_HOME}/run
export LANG=en_GB.ISO8859-1
${JETTY_HOME}/bin/jetty.sh $@
