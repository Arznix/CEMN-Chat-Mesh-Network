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