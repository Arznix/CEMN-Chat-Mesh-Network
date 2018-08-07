# CEMN-Chat-Mesh-Network
Community Engagement Mesh Network - a WiFi mesh network to facility chat capabilities in rural environments where telecom services are expensive or non-existent. 


# Setup Instructions

1.	Download and install the necessary packages

  1.1. Install the necessary packages ( All nodes )
```
sudo apt-get update
sudo apt-get install git
sudo apt-get install iw
sudo apt install libnl-3-dev libnl-genl-3-dev
sudo apt-get install -y bridge-utils hostapd dnsmasq batctl
```

  1.2.	Download the source code from github 
  
```git clone https://github.com/Arznix/CEMN-Chat-Mesh-Network.git```

2.	Download and install the Registration server node (Registration node)

  2.1.	Install Mongodb
  2.1.1.	Install mongodb 
  
```sudo apt-get install mongodb-server```

2.1.2.	Start it as a service when the raspberry pi starts

```sudo service mongodb start```

2.2.	Install nodejs and npm
2.2.1.	Download nodejs

```
wget https://nodejs.org/dist/latest-v6.x/node-v6.11.4-linux-armv7l.tar.gz
tar -xvzf node-v6.11.4-linux-armv7l.tar.gz
sudo mv node-v6.11.4-linux-armv7l /opt/node
sudo mkdir /opt/bin
sudo ln -s /opt/node/bin/* /opt/bin/
```

2.2.2.	Add the binaries in the PATH

```
sudo nano /etc/profile
PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/opt/bin"
```

2.2.3.	Check that everything is okay by taping these commands:

```
npm --version
node -v
```

2.3.	Setup Registration server and add it as a service
2.3.1.	Install Registration server packages

```
cd  ~/CEMN-Chat-Mesh-Network/Registration-Server
npm install
```

2.3.2.	Test Registration server

```
node server.js
```

2.3.3.	Add cemn_registration.sh startup scripts to init.d as a service

```
sudo cp /home/pi/CEMN-Chat-Mesh-Network/Registration-Server/cemn_registration.sh /etc/init.d/cemn_registration
```

2.3.4.	Next, we’ll need to make the cemn_registration executable.

```
sudo chmod 755 /etc/init.d/cemn_registration
```

2.3.5.	Add cemn_registration service to startup services.

```
sudo update-rc.d cemn_registration defaults
```

2.3.6.	Check if service is running

```
sudo service --status-all
```

3.	Install and configure XMPP if it's XMPP server node ( XMPP node )

```
sudo apt-get install ejabberd
sudo dpkg-reconfigure ejabberd
```

Note: Select localhost as default service name

4.	Setup batman-adv mesh network ( All nodes )
4.1.	Make sure both USB Wifi cards are running using iw dev command. We use wlan2 to setup batman-adv mesh network and wlan0 to setup Wi-Fi access point since we are using an external USB Wi-fi card to setup mesh network and the Raspberry pi 3 internal Wifi chip to setup access point.

```
iw dev
```

4.2.	Update the wlan number in cemn_mesh.sh located in Mesh-Network directory. It will be the wlan you selected in previous step to setup batman-adv mesh.

```
cd  ~/CEMN-Chat-Mesh-Network/Mesh-Network
sudo nano cemn_mesh.sh
```

Content of cemn_mesh.sh:

```
#!/bin/bash

### BEGIN INIT INFO
# Provides:          cemn_mesh
# Required-Start:    dbus
# Required-Stop:     dbus
# Should-Start:      $syslog
# Should-Stop:       $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: CEMN mesh server
# Description:       CEMN Mesh server
### END INIT INFO


# Activate batman-adv
sudo modprobe batman-adv
# Disable and configure wlan2
sudo ip link set wlan2 down
sudo ifconfig wlan2 mtu 1532
sudo iwconfig wlan2 mode ad-hoc
sudo iwconfig wlan2 essid cemn-mesh-network
sudo iwconfig wlan2 ap 02:12:34:56:78:90
sudo iwconfig wlan2 channel 3
sleep 1s
sudo ip link set wlan2 up
sleep 1s
sudo batctl if add wlan2
sleep 1s
sudo batctl ap_isolation 1
sleep 1s
sudo brctl addif br0 bat0
sleep 1s
sudo ifconfig wlan2 up
sudo ifconfig bat0 up
sleep 5s
# Use different IPv4 addresses for each device
#sudo ifconfig bat0 172.27.0.xxx/16
```

4.3.	Add cemn_mesh.sh startup scripts to init.d as a service

```
sudo cp /home/pi/CEMN-Chat-Mesh-Network/Mesh-Network/cemn_mesh.sh /etc/init.d/cemn_mesh
```

4.4.	Next, we’ll need to make the cemn_mesh executable.

```
sudo chmod 755 /etc/init.d/cemn_mesh
```

4.5.	Add cemn_mesh service to startup services.

```
sudo update-rc.d cemn_mesh defaults
```

4.6.	Comment out the unused script in /etc/rc.local
4.7.	Remove psk and ssid from wpa_supplicant.conf

```
sudo nano /etc/wpa_supplicant/wpa_supplicant.conf
```

5.	Setup Access point and bridge ( All nodes )
5.1.	Edit line #20 in /lib/udev/rules.d/75-persistent-net-generator.rule file and add "wlan*|"

```
sudo nano /lib/udev/rules.d/75-persistent-net-generator.rule
```
Note: This will generate /etc/udev/rules.d/70-persistent-net.rules file

5.2.	Reboot the node

```
sudo reboot
```

5.3.	Check the wifi chips have correct wlan interface 

```
sudo nano /etc/udev/rules.d/70-persistent-net.rules 
```

Note: We are using another USB Wifi card to setup Wi-Fi access point in Raspberry pi 3. so, We will be using wlan0.

5.4.	Configure a static IP for wlan0 by opening up the interface configuration file with following command:

```
sudo nano /etc/network/interfaces

allow-hotplug wlan0  
iface wlan2 inet static  
    address 10.0.0.xxx
    netmask 255.255.255.0
	

auto br0
iface br0 inet static
        address 192.168.4.xxx
        netmask 255.255.255.0
        bridge_ports bat0 wlan0
        bridge_stp off

iface default inet dhcp
```

For example: The address for first node will be 10.0.0.1

5.5.	Restart dhcpcd

```
sudo service dhcpcd restart
```

5.6.	Reload the configuration for wlan0 with following command

```
sudo ifdown wlan0; sudo ifup wlan0
```

5.7.	Create a new configuration file and add Access point configuration settings to it 

```
sudo nano /etc/hostapd/hostapd.conf 
```

Content of /etc/hostapd/hostapd.conf:

```
# This is the name of the WiFi interface we configured above
interface=wlan0
# This is the name of the bridge interface which is used if you are bridging mesh network and access point
bridge=br0
# Use the nl80211 driver with the brcmfmac driver
driver=nl80211
# This is the control interface
ctrl_interface=/var/run/hostapd
ctrl_interface_group=0
# This is the name of the AP and should be different for each node
ssid=CEMN-AP1
# Use the 2.4GHz band
hw_mode=g
# Use channel 6
channel=6
# Enable 802.11n
ieee80211n=1
# Enable WMM
wmm_enabled=1
# Enable isolation mode
ap_isolate=1
# Accept all MAC addresses
macaddr_acl=0
# Use WPA authentication
auth_algs=1
# Require clients to know the network name
ignore_broadcast_ssid=0
# Use WPA2
wpa=2
# Use a pre-shared key
wpa_key_mgmt=WPA-PSK
# The network passphrase
wpa_passphrase=cemnberry
# Use AES, instead of TKIP
rsn_pairwise=CCMP
beacon_int=100
ieee80211n=1
```

5.8.	Check if it's working at this stage by running following command:

```
sudo /usr/sbin/hostapd /etc/hostapd/hostapd.conf
```

You should get something similar to following message:

```
"Configuration file: /etc/hostapd/hostapd.conf
Failed to create interface mon.wlan2: -95 (Operation not supported)
wlan2: Could not connect to kernel driver
Using interface wlan0 with hwaddr b8:27:eb:69:f2:10 and ssid "CEMN-AP1"
random: Only 18/20 bytes of strong random data available from /dev/random
random: Not enough entropy pool available for secure operations
WPA: Not enough entropy in random pool for secure operations - update keys later when the first station connects
wlan0: interface state UNINITIALIZED->ENABLED
wlan0: AP-ENABLED"
```

5.9.	Hit CTRL+C to brake operation
5.10.	Set Host Access Point Daemon to start his job at system startup:

```
sudo sed -i "s/^.*DAEMON_CONF=.*$/DAEMON_CONF=\"\/etc\/hostapd\/hostapd.conf\"/" /etc/default/hostapd
```

5.11.	Install DHCP and DNS server configuration:

```
cat > /etc/dnsmasq.conf <<EOF
interface=br0
address=/#/192.168.4.1
address=/google.com/0.0.0.0
dhcp-range=192.168.4.101,192.168.4.254,255.255.255.0,1h
EOF
```

5.12.	Enable IPv4 packet forwarding instantly:

```
sudo sh -c "echo 1 > /proc/sys/net/ipv4/ip_forward"
```

5.13.	Enable IPv4 packet forwarding at system startup:

```
sudo sed -i "s/^.*net\.ipv4\.ip_forward=.*$/net\.ipv4\.ip_forward=1/" /etc/sysctl.conf
```

5.14.	Restart wireless network interface, Host Access Point Daemon, DHCP and DNS server:

```
sudo ifdown wlan0;sudo ifup wlan0
sudo service hostapd restart
sudo service dnsmasq restart
```

5.15.	Reboot the node

```
sudo reboot
```

6.	Testing
6.1.	Testing mesh network and access point

```
iw dev
sudo ifconfig
sudo batctl o
```

6.2.	Test XMPP server
6.2.1.	Connect to AP and login using following link:

```
http://172.24.1.1:5280/admin/
```

6.3.	Change the Symmetric key and IP address of registration server in android App to match registration server node IP address and Symmetric key

