##############################
######## DYNAMOD DB ##########
##############################
make a directory at /apps that you are owner
Run the Setup-dynamodb ANT job
Execute the jar at /apps/dynamodb/DynamoDBLocal.jar 

cd /apps/dynamodb
java -jar DynamoDBLocal.jar 

open a browser at : http://localhost:8000/shell/

Set Account ID as : <<SEE SECRETS AWS ACCESS KEY>> (in GUI) (top cog)
Run the saved scripts 
paste the content of the files in localsetup/resources/dynamo-scripts
Run each one - bask in glory
DB can be wiped by boshing the file : /apps/dynamodb/<<SEE SECRETS AWS ACCESS KEY>>_us-west-2.db

##############################
##### Test Duck command ######
##############################
to make the DAO layer connect to your local DB you must set your environment.properties to have
local-db=http://localhost:8000

Export the Jar

cd ~/workspace_dnsjava/duckcommand/dist
java -jar duckcommand.jar accountStats

##############################
###### For the Website #######
##############################
extract to /apps
localsetup/resources/apache-tomcat-7.0.57.tar.gz

Then add a new tomcat 7 server - use this as the file location - also select JDK 8
Set the domain as localdev.duckdns.org (I have this set to 127.0.0.1)

to make the DAO layer connect to your local DB you must set your environment.properties to have
local-db=http://localhost:8000

Then RUN-AS on Server - for the duckdns project

When you try to login - you will get returned back to 
https://www.duckdns.org/LOTS OF STUFF
Change the broken return in the browser to be
http://localdev.duckdns.org:8080/LOTS OF STUFF

If you want the CACHE clear to work then you will need to update all the IP's in secrets.properites to be 127.0.0.1
	
Don't forget to re-deploy if you do this

##############################
###### FOR DNS SERVER ########
##############################
to make the DAO layer connect to your local DB you must set your secrets.properties to have
local-db=http://localhost:8000

Run the ANT BUILD normally - it will drop a jar in the dist/

Run the setuplocal ANT task setup-dns

sudo su
export PATH=/home/steven/Apps/jdk1.8.0/bin:$PATH
cd /apps/dnsjava
./dnsjava_run.sh 

dig test.duckdns.org @localhost
 
Make sure you have logged in and registered a name "test"

###############################
### NGINX - MAKE LOGIN WORK ###
###############################
Copy the file to /etc/nginx/sites-available
	localsetup/resources/nginx/duckdns
Link it into sites-enabled
remove default for now (if you have 443 in default it will not start

copy 2 key files into /etc/nginx/ 
	localsetup/resources/nginx/duckdns/*.pem
	
Add into host file 
	127.0.0.1 www.duckdns.org

Restart Nginx
	service nginx restart

Point browser : accept duff cert
	https://www.duckdns.org/
