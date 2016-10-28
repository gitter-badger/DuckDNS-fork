#!/bin/sh
echo "*************************************"
echo "JETTY-DUCK-DNS" 
echo "*************************************"
USERNAME_REQUIRED="jetty"
if [ `whoami` != $USERNAME_REQUIRED ]; then
	echo "Not running as $USERNAME_REQUIRED"
	exit 1
fi
#echo "*** UPDATING HOST PROPERTIES"
#/apps/scripts/update_host_info.sh
#cat /apps/jetty/jetty_current/resources/environment.properties
#echo "*** DONE"
export JAVA_HOME=/apps/java/jdk_current
export JETTY_HOME=/apps/jetty/jetty_current
export JAVA_OPTIONS="-d64 -server -Xms200m -Xmx400m"
export JETTY_PORT=8080
export JETTY_RUN=${JETTY_HOME}/run
export LANG=en_GB.ISO8859-1
${JETTY_HOME}/bin/jetty.sh $@