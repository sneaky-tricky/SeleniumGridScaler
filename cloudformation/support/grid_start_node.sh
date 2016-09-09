#!/bin/sh
PATH=/sbin:/usr/sbin:/bin:/usr/bin
GRID_INSTALL=/opt/grid

export EC2_INSTANCE_ID="`wget -q -O - http://169.254.169.254/latest/meta-data/instance-id`"
# Pull down the user data, which will be a zip file containing necessary information
export NODE_TEMPLATE="$GRID_INSTALL/nodeConfigTemplate.json"
wget http://169.254.169.254/latest/user-data -O $GRID_INSTALL/data.zip

# Now, unzip the data downloaded from the userdata
unzip -o $GRID_INSTALL/data.zip -d $GRID_INSTALL
# Replace the instance ID in the node config file
sed "s/<INSTANCE_ID>/$EC2_INSTANCE_ID/g" $NODE_TEMPLATE > $GRID_INSTALL/nodeConfig.json
# Finally, run the java process in a window so browsers can run
xvfb-run --auto-servernum --server-args='-screen 0, 1600x1200x24' java -jar $GRID_INSTALL/selenium-server-node.jar -role node -nodeConfig $GRID_INSTALL/nodeConfig.json -Dwebdriver.chrome.driver="$GRID_INSTALL/chromedriver" -log $GRID_INSTALL/grid.log &

