from java.util import Calendar
from usace.rowcps.headless import LoggingOptions

def compute_All_Flowgroups(officeID, location, startCal, endCal):
    # Takes in locations defined by user in group and computes all flow groups
    try:
        gateCalc.computeAll(officeID, location, startCal.getTime(), endCal.getTime())
    except:
#         print "Error Computing all Flow Groups at {0} {1}".format(officeID, location)
        print ''


def compute_Single_Flowgroup(officeID, location, startCal, endCal):
    try:
        gateCalc.computeFlowGroup(officeID, location, startCal.getTime(), endCal.getTime(), "Flow.{0}.Pump_Out_Total".format(location))
    except:
#         print "Error Computing Flow Group at {0} {1}".format(officeID, location)
        print ''

    # #gateCalc.computeFlowGroup("SWF", "ACTT2",  startCal.getTime(), endCal.getTime(), "Flow.ACTT2.Pump_Out_Total")
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
# startCal.clear()
startCal.add(Calendar.DAY_OF_MONTH, -6)
startCal.set(Calendar.HOUR, 0)
startCal.set(Calendar.MINUTE, 0)
startCal.set(Calendar.SECOND, 0)
# startCal.set(Calendar.YEAR, 2015)
# startCal.set(Calendar.MONTH, 9)

# create a java Calendar object that will be used to create the end Date
endCal = Calendar.getInstance()
# endCal.clear()
endCal.add(Calendar.DAY_OF_MONTH, 5)
endCal.set(Calendar.HOUR, 0)
endCal.set(Calendar.MINUTE, 0)
endCal.set(Calendar.SECOND, 0)
# endCal.set(Calendar.YEAR, 2015)
# endCal.set(Calendar.MONTH, 11)

officeID = "SWF"

# the gateCalc object can perform its calculation for a single flow group
# the computeFlowGroup method takes:
# officeId
# projectId
# startDate
# endDate
# gateCalc.computeFlowGroup("SWF", "LEWT2",  startCal.getTime(), endCal.getTime(), "Flow.LEWT2.ConduitGate_Total")
# By setting the following parameter to True, the following flow groups in the list FlowGroupList will all be calculated. Items can be commented out
# or commented back in individually. To turn this option off, set the following parameter to False.
calculateSingleFlowGroups = False
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
#                 "STIT2",
#                 "TXKT2",
#                 "WTYT2",
#                 "TRNT2",
#                 "FFLT2",
#                 "BPRT2",
#                 "EAMT2",
#                 "FLWT2",
#                 "LLST2",
#                 "GBYT2",
#                 "PSMT2",
#                 "TBLT2",
#                 "TBRT2",
#                 "GPET2",
#                 "BSLT2",
#                 "SAGT2",
#                 "MSDT2",
                ]

# the calculation can also be performed for all the associated groups
# the computeAll method takes:
# officeId
# projectId
# startDate
# endDate
# By setting the following parameter to True, all of the following flow groups in each location in the list locationList will all be calculated.
# Items can be commented out or commented back in individually. To turn this option off, set the following parameter to False.

calculateAllFlowGroups = True
FlowGroupList = ["ACTT2",
                 "ALAT2",
                 "BDWT2",
                 "BLNT2",
                 "BNBT2",
                 "DAWT2",
                 "GGLT2",
#                  "GNGT2",
#                  "GPVT2",
#                  "HORT2",
#                  "JPLT2",
#                  "LEWT2",
#                  "LVNT2",
#                  "PCTT2",
#                  "RRLT2",
#                  "FRHT2",
#                  "SCLT2",
#                  "SMCT2",
#                  "SOMT2",
#                  "STIT2",
#                  "TXKT2",
#                  "WTYT2",
#                  "TRNT2",
#                  "FFLT2",
#                  "EAMT2",
#                  "FLWT2",
#                  "LLST2",
#                  "GBYT2",
#                  "PSMT2",
#                  "SAGT2",
                 ]


if calculateAllFlowGroups:
    for location in FlowGroupList:
        print "Now Running", location, "GROUP"
        compute_All_Flowgroups(officeID, location, startCal, endCal)

if calculateSingleFlowGroups:
    for location in locationList:
        print "Now Running", location, "SINGLE"
        compute_Single_Flowgroup(officeID, location, startCal, endCal)


# gateCalc.computeAll("SWF", "ACTT2",  startCal.getTime(), endCal.getTime())
# gateCalc.computeAll("SWF", "ALAT2",  startCal.getTime(), endCal.getTime())
# gateCalc.computeAll("SWF", "BDWT2",  startCal.getTime(), endCal.getTime())
# gateCalc.computeAll("SWF", "BLNT2",  startCal.getTime(), endCal.getTime())
# gateCalc.computeAll("SWF", "BNBT2",  startCal.getTime(), endCal.getTime())
# gateCalc.computeAll("SWF", "CLDL1",  startCal.getTime(), endCal.getTime())
# gateCalc.computeAll("SWF", "DAWT2",  startCal.getTime(), endCal.getTime())
# gateCalc.computeAll("SWF", "GGLT2",  startCal.getTime(), endCal.getTime())

# #gateCalc.computeAll("SWF", "GNGT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "GPVT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "HORT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "JFNT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "JPLT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "JSPT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "LEWT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "LVNT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "PCTT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "RRLT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "FRHT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "SCLT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "SMCT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "SOMT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "STIT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "TXKT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "WTYT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "TRNT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "FFLT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "BPRT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "EAMT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "FLWT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "LLST2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "GBYT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "PSMT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "TBLT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "TBRT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "GPET2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "BSLT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "SAGT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeAll("SWF", "MSDT2",  startCal.getTime(), endCal.getTime())
# #gateCalc.computeFlowGroup("SWF", "ACTT2",  startCal.getTime(), endCal.getTime(), "Flow.ACTT2.Pump_Out_Total")
# #gateCalc.computeFlowGroup("SWF", "ALAT2",  startCal.getTime(), endCal.getTime(), "Flow.ALAT2.Pump_Out_Total")
# #gateCalc.computeFlowGroup("SWF", "BDWT2",  startCal.getTime(), endCal.getTime(), "Flow.BDWT2.Pump_Out_Total")
# #gateCalc.computeFlowGroup("SWF", "BLNT2",  startCal.getTime(), endCal.getTime(), "Flow.BLNT2.Pump_Out_Total")
# #gateCalc.computeFlowGroup("SWF", "BNBT2",  startCal.getTime(), endCal.getTime(), "Flow.BNBT2.Pump_Out_Total")
# #gateCalc.computeFlowGroup("SWF", "DAWT2",  startCal.getTime(), endCal.getTime(), "Flow.DAWT2.Pump_Out_Total")
# #gateCalc.computeFlowGroup("SWF", "GGLT2",  startCal.getTime(), endCal.getTime(), "Flow.GGLT2.Pump_Out_Total")
# #gateCalc.computeFlowGroup("SWF", "GNGT2",  startCal.getTime(), endCal.getTime(), "Flow.GNGT2.Pump_Out_Total")
# #gateCalc.computeFlowGroup("SWF", "GPVT2",  startCal.getTime(), endCal.getTime(), "Flow.GPVT2.Pump_Out_Total")
# #gateCalc.computeFlowGroup("SWF", "HORT2",  startCal.getTime(), endCal.getTime(), "Flow.HORT2.Pump_Out_Total")
# #gateCalc.computeFlowGroup("SWF", "JPLT2",  startCal.getTime(), endCal.getTime(), "Flow.JPLT2.Pump_Out_Total")
# #gateCalc.computeFlowGroup("SWF", "LEWT2",  startCal.getTime(), endCal.getTime(), "Flow.LEWT2.Pump_Out_Total")
# #gateCalc.computeFlowGroup("SWF", "LVNT2",  startCal.getTime(), endCal.getTime(), "Flow.LVNT2.Pump_Out_Total")
####gateCalc.computeFlowGroup("SWF", "PCTT2",  startCal.getTime(), endCal.getTime(), "Flow.PCTT2.Pump_Out_Total")
# #gateCalc.computeFlowGroup("SWF", "RRLT2",  startCal.getTime(), endCal.getTime(), "Flow.RRLT2.Pump_Out_Total")
####gateCalc.computeFlowGroup("SWF", "FRHT2",  startCal.getTime(), endCal.getTime(), "Flow.FRHT2.Pump_Out_Total")
# #gateCalc.computeFlowGroup("SWF", "SCLT2",  startCal.getTime(), endCal.getTime(), "Flow.SCLT2.Pump_Out_Total")
# #gateCalc.computeFlowGroup("SWF", "SMCT2",  startCal.getTime(), endCal.getTime(), "Flow.SMCT2.Pump_Out_Total")
# #gateCalc.computeFlowGroup("SWF", "SOMT2",  startCal.getTime(), endCal.getTime(), "Flow.SOMT2.Pump_Out_Total")
# #gateCalc.computeFlowGroup("SWF", "STIT2",  startCal.getTime(), endCal.getTime(), "Flow.STIT2.Pump_Out_Total")
####gateCalc.computeFlowGroup("SWF", "TXKT2",  startCal.getTime(), endCal.getTime(), "Flow.TXKT2.Pump_Out_Total")
# #gateCalc.computeFlowGroup("SWF", "WTYT2",  startCal.getTime(), endCal.getTime(), "Flow.WTYT2.Pump_Out_Total")
####gateCalc.computeFlowGroup("SWF", "TRNT2",  startCal.getTime(), endCal.getTime(), "Flow.TRNT2.Pump_Out_Total")
####gateCalc.computeFlowGroup("SWF", "FFLT2",  startCal.getTime(), endCal.getTime(), "Flow.FFLT2.Pump_Out_Total")
####gateCalc.computeFlowGroup("SWF", "EAMT2",  startCal.getTime(), endCal.getTime(), "Flow.EAMT2.Pump_Out_Total")
####gateCalc.computeFlowGroup("SWF", "FLWT2",  startCal.getTime(), endCal.getTime(), "Flow.FLWT2.Pump_Out_Total")
# #gateCalc.computeFlowGroup("SWF", "LLST2",  startCal.getTime(), endCal.getTime(), "Flow.LLST2.Pump_Out_Total")
####gateCalc.computeFlowGroup("SWF", "GBYT2",  startCal.getTime(), endCal.getTime(), "Flow.GBYT2.Pump_Out_Total")
# #gateCalc.computeFlowGroup("SWF", "PSMT2",  startCal.getTime(), endCal.getTime(), "Flow.PSMT2.Pump_Out_Total")
# #gateCalc.computeFlowGroup("SWF", "SAGT2",  startCal.getTime(), endCal.getTime(), "Flow.SAGT2.Pump_Out_Total")
