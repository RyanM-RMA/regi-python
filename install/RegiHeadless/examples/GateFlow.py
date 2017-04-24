from java.util import Calendar
from usace.rowcps.headless import LoggingOptions

def compute_All_Flowgroups(officeID, location, startCal, endCal):
    # Takes in locations defined by user in group and computes all flow groups
    try:
        gateCalc.computeAll(officeID, location, startCal.getTime(), endCal.getTime())
    except Exception as e:
        print "Error Computing all Flow Groups at {0} {1}".format(officeID, location)
        continue

def compute_Single_Flowgroup(officeID, location, startCal, endCal):
    try:
        gateCalc.computeFlowGroup(officeID, location, startCal.getTime(), endCal.getTime(), "Flow.{0}.Pump_Out_Total".format(location))
    except Exception as e:
        print "Error Computing Flow Group at {0} {1}".format(officeID, location)
        continue

# Description of: LoggingOptions.setDbMessageLevel(int level)
#
# Adds Time Series logging messages in the OracleTimeSeriesDaoImpl.  Recommended
# level is 2, as this provides basic information about the time series
# retrieval/storage.
#
# Message Level | Description
# --------------|-------------------------------------------------------------------------------------------------------------------------------|
# <=0           | Default value, does not do anything.  Lower values do not change behavior.                                                  |
# 1             | Logs message when no data is found.  Logs message when data is found, how much was retrieved or stored, and how long it took. |
# 2             | Adds message with name of time series, and the units to retrieve/store.                                                       |
# 3             | Adds message with the current time.                                                                                           |
# 4             | Adds message with first 10 dates and values from each time series.                                                            |
# >4            | Same as 4, but shows all values retrieved from each time series.  Higher values do not change behavior.                       |
# --------------|-------------------------------------------------------------------------------------------------------------------------------|

LoggingOptions.setDbMessageLevel(2)


# Description of: LoggingOptions.setMetricsEnabled(boolean value)
#
# Enables or disables the storage of REGI's Metric data pertaining to the
# performance of the application.  This is incredibly helpful for identifying
# issues where the application takes an excessive amount of time to operate.
#
# Metrics also log the location of the files as an INFO message if they are
# enabled.
#
# By default, Metrics are disabled.

# LoggingOptions.setMetricsEnabled(True)

# not all of Regi is scriptable, registry is an object created by the java class RegiCLI that contains a list of
# the implemented scriptable calculations
names = registry.getNames(1.0)

# this retrieves a Gate Flow calculation object
gateCalc = registry.getCalculation(1.0, "Gate Flow")

# the gate flow calculations requires a start and end time.
# here we create a java Calendar object that will be used to create the start Date
startCal = Calendar.getInstance()
startCal.clear()
startCal.set(Calendar.YEAR, 2015)
startCal.set(Calendar.MONTH, 4)

# create a java Calendar object that will be used to create the end Date
endCal = Calendar.getInstance()
endCal.clear()
endCal.set(Calendar.YEAR, 2015)
endCal.set(Calendar.MONTH, 6)

officeID = "SWF"

# the gateCalc object can perform its calculation for a single flow group
# the computeFlowGroup method takes:
# officeId
# projectId
# startDate
# endDate
# gateCalc.computeFlowGroup("SWF", "LEWT2",  startCal.getTime(), endCal.getTime(), "Flow.LEWT2.ConduitGate_Total")
calculateSingleFlowGroups = True
FlowGroupList = ["ACTT2",
                 "ALAT2",
                 "BDWT2",
                 "BLNT2",
                 "BNBT2",
                 "DAWT2",
                 "GGLT2",
                 "GNGT2",
                 "GPVT2",
                 "HORT2",
                 "JPLT2",
                 "LEWT2",
                 "LVNT2",
                 "PCTT2",
                 "RRLT2",
                 "FRHT2",
                 "SCLT2",
                 "SMCT2",
                 "SOMT2",
                 "STIT2",
                 "TXKT2",
                 "WTYT2",
                 "TRNT2",
                 "FFLT2",
                 "EAMT2",
                 "FLWT2",
                 "LLST2",
                 "GBYT2",
                 "PSMT2",
                 "SAGT2",
                 ]
# the calculation can also be performed for all the associated groups
# the computeAll method takes:
# officeId
# projectId
# startDate
# endDate
# gateCalc.computeAll("SWF", "LEWT2",  startCal.getTime(), endCal.getTime())
calculateAllFlowGroups = True
locationList = ["ACTT2",
                "ALAT2",
                "BDWT2",
                "BLNT2",
                "BNBT2",
                "CLDL1",
                "DAWT2",
                "GGLT2",
                "GNGT2",
                "GPVT2",
                "HORT2",
                "JFNT2",
                "JPLT2",
                "JSPT2",
                "LEWT2",
                "LVNT2",
                "PCTT2",
                "RRLT2",
                "FRHT2",
                "SCLT2",
                "SMCT2",
                "SOMT2",
                "STIT2",
                "TXKT2",
                "WTYT2",
                "TRNT2",
                "FFLT2",
                "BPRT2",
                "EAMT2",
                "FLWT2",
                "LLST2",
                "GBYT2",
                "PSMT2",
                "TBLT2",
                "TBRT2",
                "GPET2",
                "BSLT2",
                "SAGT2",
                "MSDT2",
                ]



if calculateAllFlowGroups:
    for location in locationList:
        compute_All_Flowgroups(officeID, location, startCal, endCal)

if calculateSingleFlowGroups:
    for location in FlowGroupList:
        compute_Single_Flowgroup(officeID, location, startCal, endCal)


