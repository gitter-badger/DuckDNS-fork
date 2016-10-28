echo "*************************************"
echo "JAVA DNS SERVER" 
echo "*************************************"
USERNAME_REQUIRED="root"
if [ `whoami` != $USERNAME_REQUIRED ]; then
	echo "Not running as $USERNAME_REQUIRED"
	exit 1
fi
export JAVA_HOME=/apps/java/jdk_current
export DNSJAVA_HOME=/apps/dnsjava/dnsjava_current
export JAVA_OPTIONS="-d64 -server -Xms300m -Xmx300m"
export LANG=en_GB.ISO8859-1
cd ${DNSJAVA_HOME}
${JAVA_HOME}/bin/java -cp DuckDnsServer.jar:dnsjava-2.1.7.jar:aws-java-sdk-1.7.5.jar:org.apache.httpcomponents.httpcore_4.3.2.jar:joda-time-2.3.jar:jackson-core-2.3.2.jar:jackson-annotations-2.3.2.jar:jackson-databind-2.3.2.jar:org.apache.httpcomponents.httpclient_4.3.3.jar:commons-logging-1.1.1.jar:AmazonElastiCacheClusterClient-1.0.jar:guava-19.0.jar:. DuckDnsServer DuckDnsServer.conf