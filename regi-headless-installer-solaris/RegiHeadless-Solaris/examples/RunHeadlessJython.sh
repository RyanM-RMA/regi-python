#!/bin/bash

NOT_FOUND="notFound"

PROG_ROOT=".."
JAR_DIR=$PROG_ROOT/lib
SYS=$JAR_DIR/sys
CWMS=$JAR_DIR/cwms
REGI=$JAR_DIR/regi
LIB_PATH=$PROG_ROOT/lib64
JAVA_EXE=$PROG_ROOT/jre64/bin/java
ARGS=("$@")

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

SCRIPT=""
NAME_FOUND=false

for PARAMETER in "${ARGS[@]}"
do
  PARAM_NAME=$(echo "$PARAMETER" | cut -d= -f1 | tr \[a-z\] \[A-Z\])
  if [ "$NAME_FOUND" ] ; then
    BASENAME="$(basename "${PARAMETER}")" #Get only the base file name.
    SCRIPT="${BASENAME%%.*}" #Remove the extension
    break
  elif [ "$PARAM_NAME" = "f" ] ; then
    NAME_FOUND=true
  fi
done


JAVA_CMD="-cp $JAR_DIR/*:$REGI/*:$CWMS/*:$SYS/*:"\
" -Doracle.url=jdbc:oracle:thin:@${ORA_INST}"\
" -Doracle.officeId=$OFFICE_ID"\
" -Dhec.passwd=$PASS_FILE"\
" -Djava.library.path=$LIB_PATH"\
" -DPLUGINS=$EXT"\
" -Doracle.metrics.clientid=\"CWMS REGI-Headless-v5.0\""\
" -Djava.util.logging.config.file=../config/properties/logging.properties"\
" -Drowcps.timezone=America/Chicago"\
" usace.rowcps.headless.RegiCLI"\
" -p ..//examples//credentials.properties"\
" ${ARGS[*]}"

OUTFILE=$CWMS_HOME/cronjobs/headless/logs/$(getStartFN regi-headless-"$SCRIPT")

echo "Running jython $SCRIPT"
echo "Output going to: $OUTFILE"
echo "Java Command:"
echo "$JAVA_EXE $JAVA_CMD"
eval "$JAVA_EXE" $JAVA_CMD &> "$OUTFILE"
STATUS=$?

echo "$SCRIPT process exited with code $STATUS"