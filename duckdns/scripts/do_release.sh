#!/bin/bash
echo "Copying war file into place"
cp /var/tmp/duckdns.war /apps/jetty/jetty_config/webapps/root.war
echo "Expanding web files"
unzip -d /apps/www/ -o /var/tmp/duckdns.war *.ico *.css *.js *.gif *.jpeg *.jpg *.png *.html sitemap.xml
echo "All done"