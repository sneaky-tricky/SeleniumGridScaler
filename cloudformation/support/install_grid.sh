#!/bin/sh
GRID_DIR=/opt/grid
TMP_DIR=/tmp/install
echo "127.0.0.1 $(hostname)" >> /etc/hosts
sudo apt-get -y install unzip
sudo apt-get -y install openjdk-8-jdk
sudo apt-get -y install xvfb
sudo apt-get -y install s3cmd

sudo apt-get -y update
sudo apt-get -y -f install
mkdir $GRID_DIR
mkdir $TMP_DIR

cd $GRID_DIR
wget http://chromedriver.storage.googleapis.com/2.9/chromedriver_linux64.zip -O $TMP_DIR/chromedriver.zip
unzip -d $GRID_DIR $TMP_DIR/chromedriver.zip 
wget http://selenium-release.storage.googleapis.com/2.42/selenium-server-standalone-2.42.0.jar -O $GRID_DIR/selenium-server-node.jar
wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb -O $TMP_DIR/chrome.deb
sudo dpkg -i $TMP_DIR/chrome.deb 
wget https://ftp.mozilla.org/pub/firefox/releases/47.0/linux-x86_64/en-US/firefox-47.0.tar.bz2 -O $TMP_DIR/firefox.tar.bz2
tar xvjf $TMP_DIR/firefox.tar.bz2 -C /opt/
sudo ln -snf /opt/firefox/firefox /usr/bin/firefox
sudo crontab -l > $TMP_DIR/mycron
echo "@reboot $GRID_DIR/grid_start_node.sh >> $GRID_DIR/process.log 2>&1" >> $TMP_DIR/mycron
sudo crontab $TMP_DIR/mycron

rm -fr $TMP_DIR