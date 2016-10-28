## INTRO
Duck Command is designed as a command tool to run on the server for administration
This tool has been used for migrations and backup

quack V2 - valid commands are : 
 findUser(domain)
 lockAccount(domain)
 unlockAccount(domain)
 accountStats
 domainStats
 upgradeAccount(email)
 downgradeAccount(email)
 backupDomains(file)
 backupAccounts(file)
 restoreDomains(file)
 restoreAccounts(file)
 clearDomains
 clearAccounts
 updateApp

 ## CODE
This workspace is designed to be opened in ECLIPSE
The Project is designed to be run under SUN Java 1.8

## COPY SECRETS
Add an ANT view into Eclipse (Window | Show View | Other - ANT)
Drag the build XML into the ant window
Double click on the "Duck Command" ant file
This task copies your Secrets from the secrets project - see the ../samplesecrets project
../secrets/aws/AwsCredentials.properties
../secrets/command/secrets.properties

## BEFORE STARTING
You must be running the local DYNAMO DB
See ../localsetup/README.txt
to make the DAO layer connect to your local DB you must set your environment.properties to have
local-db=http://localhost:8000

## CREATE JAR
Right Click on the duckcommand project in Project Explorer
Choose Export | Runnable JAR

Account Tool | DuckCommand
duckcommand/dist/duckcommand.jar
Package Required Libraries into generated JAR


## TESTING
You can use the files in scripts/*
Manually : 
Export the Jar

cd ~/workspace_dnsjava/duckcommand/dist
java -jar duckcommand.jar accountStats