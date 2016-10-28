## INTRO
This is a Highly Concurrent DNS Server that listens on UDP port 53
The base of the DNS Zone is in the conf/*.conf files
All the Dynamic data is in the Dynamo DB database
All Lookups are cached in memory
A listener on port 10025 is informed of when to flush cache items
The server knows how to respond to A AAAA MX SOA requests
The main WWW response is a CNAME to an Amazon ELB
Events are passed to Google ANALYTICS for real time monitoring

## CODE
This workspace is designed to be opened in ECLIPSE
The Project is designed to be run under SUN Java 1.8

## CREATE JAR
To create the DuckDnsServer.jar
Add an ANT view into Eclipse (Window | Show View | Other - ANT)
Drag the build XML into the ant window
Double click on the "Duck DNS Server" ant file
The jar will be built at : dist/DuckDnsServer.jar
This task also copies your Secrets from the secrets project - see the ../samplesecrets project
../secrets/aws/AwsCredentials.properties
../secrets/dns/secrets.properties

## AUTO SETUP
There is a setup job in
../localsetup/build.xml setup-dns

## CONFIG
You will need to place all the library Jars and the AwsCredentials.properties in the same directory as the DuckDNSServer.jar
Copy the file conf/duckdns.org-ns1 and rename it to duckdns.org

## FILE LAYOUT
DuckDnsServer.jar
*ALL LIB JARS*.jar
DuckDnsServer.conf
AwsCredentials.properties
secrets.properties
/conf/duckdns.org

## BEFORE STARTING
You must be running the local DYNAMO DB
See ../localsetup/README.txt
to make the DAO layer connect to your local DB you must set your secrets.properties to have
local-db=http://localhost:8000

## RUNNING THE SERVER
AS ROOT (because its going to listen on port 53) RUN THIS
sudo su
${JAVA_HOME}/bin/java -cp DuckDnsServer.jar:dnsjava-2.1.7.jar:aws-java-sdk-1.7.5.jar:org.apache.httpcomponents.httpcore_4.3.2.jar:joda-time-2.3.jar:jackson-core-2.3.2.jar:jackson-annotations-2.3.2.jar:jackson-databind-2.3.2.jar:org.apache.httpcomponents.httpclient_4.3.3.jar:commons-logging-1.1.1.jar:AmazonElastiCacheClusterClient-1.0.jar:guava-19.0.jar:. DuckDnsServer DuckDnsServer.conf

## TESTING
Then in another term, try these commands

dig www.duckdns.org @localhost

dig duckdns.org NS @localhost

dig duckdns.org MX @localhost

dig duckdns.org SOA @localhost

dig www.duckdns.org A @localhost

dig www.duckdns.org AAAA @localhost

dig test.duckdns.org A @localhost

dig test.duckdns.org AAAA @localhost

dig nothere.duckdns.org A @localhost

dig nothere.duckdns.org AAAA @localhost


## RUNNING IN THE BACKGROUND
There are sample scripts in scripts/*
These will : 

Start as root : dnsjava.sh
This will NOHUP : dns_monitor
This will in a loop run : dns_test.sh
This starts the Server : dnsjava_run.sh
if you kill the dnsjava_run.sh process - then the loop above will re-start it
