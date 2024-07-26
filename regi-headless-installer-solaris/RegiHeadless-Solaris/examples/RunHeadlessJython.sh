#!/bin/bash

NOT_FOUND="notFound"

PROG_ROOT=".."
JAR_DIR=$PROG_ROOT/lib
SYS=$JAR_DIR/sys
EXT=$JAR_DIR/ext
REGI=$JAR_DIR/regi
LIB_PATH=$PROG_ROOT/lib64

ORA_INST=$(getProp cwms.dbi.ConnectUsingUrl @${NOT_FOUND} dbi.properties|cut -d@ -f2)
if [ $ORA_INST = $NOT_FOUND ]
then
	echo "Unable to locate Oracle URL from Cwms configuration. Will be taken from credentials.properties"
fi

echo "Found Oracle URL: $ORA_INST"

# Get the office id from cwms.properties using the CWMS getProp script
OFFICE_ID=$(getProp cwms.OfficeID ${NOT_FOUND} cwms.properties $CWMS_HOME/config/properties)
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


JAVA_CMD="../jre64/bin/java -cp $JAR_DIR/*:$EXT/*:$REGI/*:$CWMS/*:$SYS/*:" \
" -Doracle.url=jdbc:oracle:thin:@${ORA_INST}" \
" -Doracle.officeId=$OFFICE_ID" \
" -Dhec.passwd=$PASS_FILE" \
" -Djava.library.path=$LIB_PATH" \
" -DPLUGINS=$EXT" \
" -Doracle.metrics.clientid=\"CWMS REGI-Headless-v5.0\"" \
" -Djava.util.logging.config.file=../config/properties/logging.properties" \
" -Drowcps.timezone=America/Chicago" \
" -p ..//examples//credentials.properties" \
" usace.rowcps.headless.RegiCLI"

OUTFILE=$CWMS_HOME/cronjobs/headless/logs/$(getStartFN headless-"$SCRIPT")

echo "Running jython $SCRIPT"
echo "Output going to: $OUTFILE"
echo "$JAVA_CMD"
#$JAVA_CMD "$ARGS" &> "$OUTFILE"

echo "$SCRIPT process exited with code $?"