# the java Calendar class is used to create java Date objects
from java.util import Calendar

# this gets a scriptable Gate Settings object
gateSettings = registry.getCalculation(1.0, "Gate Settings")

# configure the start calendar
startCal = Calendar.getInstance()
startCal.clear()
startCal.set(Calendar.YEAR, 2015)
startCal.set(Calendar.MONTH, 4)

# configure the end calendar
endCal = Calendar.getInstance()
endCal.clear()
endCal.set(Calendar.YEAR, 2015)
endCal.set(Calendar.MONTH, 6)


# gateSettings contains four callable methods
#   void createGateSettings(String officeId, String locationStr, Date startDate, Date end) throws Exception;
#	void createGateSettingsGroup(String officeId, String locationStr, Date startDate, Date end, String groupId) throws Exception;
#	void createGateSettingsOutlet(String officeId, String locationStr, Date startDate, Date end, String outletId) throws Exception;
#	void createGateSettingsOutletFromTs(String officeId, String locationStr, Date startDate, Date end, String outletId, String tsId) throws Exception;

# This is an example of a call that would create gate settings at TainterGate 1 at WTYT2  from the specified input time series.
gateSettings.createGateSettingsOutletFromTs("SWF", "WTYT2",  startCal.getTime(), endCal.getTime(), "TG1", "WTYT2-TG1.Opening-Spillway_Gate.Const.0.0.MANUAL" )

# This is an example of a call that would create gate settings at TainterGate 1 at WTYT2 from the regi association configured input time series.
gateSettings.createGateSettingsOutlet("SWF", "WTYT2",  startCal.getTime(), endCal.getTime(), "TG1")

# This is an example of a call that would create gate settings at all outlets at WTYT2 which are in the TainterGateWTY group from the association configured time series.
gateSettings.createGateSettingsGroup("SWF", "WTYT2",  startCal.getTime(), endCal.getTime(), "WTYT2-TainterGateWTY" )

# This is an example of a call that would create gate settings for every outlet in a rating group at WTYT2.
gateSettings.createGateSettings("SWF", "WTYT2",  startCal.getTime(), endCal.getTime() )



