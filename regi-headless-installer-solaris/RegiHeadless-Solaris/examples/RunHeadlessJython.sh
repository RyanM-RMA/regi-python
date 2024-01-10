#!/bin/bash

NOT_FOUND="notFound"

ORA_INST=`getProp cwms.dbi.ConnectUsingUrl @${NOT_FOUND} dbi.properties|cut -d@ -f2`
if [ $ORA_INST = $NOT_FOUND ]
then
	echo "Unable to locate Oracle URL from Cwms configuration. Will be taken from credentials.properties"
fi

echo "Found Oracle URL: $ORA_INST"

# Get the office id from cwms.properties using the CWMS getProp script
OFFICE_ID=`getProp cwms.OfficeID ${NOT_FOUND} cwms.properties $CWMS_HOME/config/properties`
if [ $OFFICE_ID = $NOT_FOUND ]
then
	echo "Unable to locate OfficeID from Cwms configuration. Will be taken from credentials.properties"
fi

echo "Found Office ID: $OFFICE_ID"

# The password file is expected to be at the following (full) path.
PASS_FILE=$CWMS_HOME/config/properties/dbi.conf
if [ ! -f $PASS_FILE ]
then
	echo "Unable to locate password file in Cwms configuration. Will be taken from credentials.properties"
fi

../jre64/bin/java -cp ../lib/*:../lib/ext/*:../lib/regi/*:../lib/cwms/*:../lib/sys/*: \
-Doracle.url="jdbc:oracle:thin:@${ORA_INST}" \
-Doracle.officeId=$OFFICE_ID \
-Dhec.passwd=$PASS_FILE \
-Djava.library.path=../lib64 \
-DPLUGINS=../lib/ext \
-Doracle.metrics.clientid="CWMS REGI-Headless-v4.0" \
-Djava.util.logging.config.file=../config/properties/logging.properties \
usace.rowcps.headless.RegiCLI \
-Drowcps.timezone=America/Chicago \
-p ..//examples//credentials.properties \
$@
