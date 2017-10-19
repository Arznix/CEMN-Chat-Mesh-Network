#!/bin/sh

if [ true != "$INIT_D_SCRIPT_SOURCED" ] ; then
    set "$0" "$@"; INIT_D_SCRIPT_SOURCED=true . /lib/init/init-d-script
fi

### BEGIN INIT INFO
# Provides:          cemn_registration
# Required-Start:    $all
# Required-Stop:     $all
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: CEMN Registration server
# Description:       CEMN Registration server
### END INIT INFO

export PATH=$PATH:/opt/node/bin
DAEMON_PATH="/home/pi/CEMN-Chat-Mesh-Network/Registration-Server"

DAEMON=node
DAEMONOPTS="server.js"
NAME=cemn_registration
DESC="cemn_registration"
PIDFILE=/var/run/$NAME.pid
SCRIPTNAME=/etc/init.d/$NAME

cd $DAEMON_PATH
PID=`$DAEMON $DAEMONOPTS > /dev/null 2>&1 & echo $!`
