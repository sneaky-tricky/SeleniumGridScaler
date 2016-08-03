#!/bin/sh
PATH=/sbin:/usr/sbin:/bin:/usr/bin:/opt/bin
export EC2_INSTANCE_ID="`wget -q -O - http://169.254.169.254/latest/meta-data/instance-id || die \"wget instance-id has failed: $?\"`"
export NODE_TEMPLATE="/opt/grid/nodeConfigTemplate.json"
wget http://169.254.169.254/latest/user-data -O /opt/grid/data.zip
# Now, unzip the data downloaded from the userdata
unzip -o /opt/grid/data.zip -d /opt/grid/
# Replace the instance ID in the node config file
sed "s/<INSTANCE_ID>/$EC2_INSTANCE_ID/g" $NODE_TEMPLATE > /opt/grid/nodeConfig.json
# Finally, run the java process in a window so browsers can run
xvfb-run --auto-servernum --server-args='-screen 0, 1600x1200x24 -ac +extension RANDR' \
  java -jar /opt/selenium/selenium-server-standalone.jar -role node \
  -nodeConfig /opt/grid/nodeConfig.json -host `wget -O - http://169.254.169.254/latest/meta-data/local-ipv4`

