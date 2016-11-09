from java.util import Calendar
from usace.rowcps.headless import LoggingOptions

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

# the gateCalc object can perform its calculation for a single flow group
# the computeFlowGroup method takes:
# officeId
# projectId
# startDate
# endDate
#gateCalc.computeFlowGroup("SWF", "LEWT2",  startCal.getTime(), endCal.getTime(), "Flow.LEWT2.ConduitGate_Total")

# the calculation can also be performed for all the associated groups
# the computeAll method takes:
# officeId
# projectId
# startDate
# endDate
gateCalc.computeAll("SWF", "LEWT2",  startCal.getTime(), endCal.getTime())


