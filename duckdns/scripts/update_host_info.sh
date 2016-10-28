current=`ec2-metadata --public-hostname`
echo "public-hostname=$current:8090" > /apps/jetty/jetty_dev/resources/environment.properties