from hec.data import Duration
from hec.data import Interval
from hec.data import ParameterType
from hec.data import Version
from hec.data.location import LocationTemplate
from java.lang import System
from java.util import Calendar
from java.util import Collections
from java.util import GregorianCalendar
from java.util import List
from java.util import TimeZone
import os.path
import sys
from usace.rowcps.computation.flowgroup import FlowGroupCalc
from usace.rowcps.regi.model import AtFlowGroupManager
from usace.rowcps.regi.model import AtManagerType
from usace.rowcps.regi.model import AtOutletManager
from usace.rowcps.regi.model import AtProjectManager
from usace.rowcps.regi.model import CacheUsage


print "Now executing GateFlowCalc.py"
print "os.arch:", System.getProperty("os.arch")

print "sys.path", sys.path
#curDir = open(".")
print "Working dir:", os.path.abspath(".")

property = System.getProperty("java.library.path")
print "Library path: ", property

sys.stdout.flush()

print "regiDomain.getName()", regiDomain.getName()

#print AtManagerType
#print AtManagerType.DATABASE
#RegiDomain regiDomain = getRegiDomain()

#lets get a lot of managers. remember that the managers can be pulled from regidomain thru the following:
#
#regiDomain.getAtProjectManager()
#
#but its coded below to make sure that we have the Oracle manager.
projManager = regiDomain.getAtManager(managerId, AtManagerType.DATABASE, AtProjectManager.AT_PROJECT_MANAGER_NAME, AtProjectManager)
#
outletManager = regiDomain.getAtManager(managerId, AtManagerType.DATABASE, AtOutletManager.AT_OUTLET_MANAGER_NAME, AtOutletManager)
##
flowGroupManager = regiDomain.getAtManager(managerId, AtManagerType.DATABASE, AtFlowGroupManager.AT_FLOW_GROUP_MANAGER_NAME, AtFlowGroupManager)

#our office
officeId = "SWF"
#this stuff would be grabbed from the project descriptor.
projectId = "LEWT2"
projectLocRef = LocationTemplate(officeId, projectId, None)
cg1LocRef = LocationTemplate(officeId, projectId + "-CG1", None)

conduitGateFlowGroupMap = flowGroupManager.retrieveFlowGroups(projectLocRef, None, CacheUsage.NORMAL)
entrySet = conduitGateFlowGroupMap.entrySet()

conduitGateFlowGroup = None
for entry in entrySet:
    key = entry.getKey()
    value = entry.getValue()
    if "ConduitGate_Total" in key.getId():
		conduitGateFlowGroup = key
		break


if conduitGateFlowGroup is None:
#    Assert.fail("Couldnt find conduit gate flow group")
	print "Couldnt find conduit gate flow group"
else:
	startCal = Calendar.getInstance()
	startCal.clear()
	startCal.set(Calendar.YEAR, 2015)
	startCal.set(Calendar.MONTH, 1)
	startTime = startCal.getTimeInMillis()

	#this could be INST too.
	parameterType = ParameterType(ParameterType.AVE)
	interval = Interval("5Minutes")
	duration = Duration("5Minutes")
	version = Version("CALC")
	intervalOffsetSeconds = 0
	newFlowGroupTimeSeries = conduitGateFlowGroup.newFlowGroupTimeSeries(parameterType, interval, duration, version, intervalOffsetSeconds, startCal.getTime(), None)

	flowGroupCalc = FlowGroupCalc()
	outputTimeSeriesList = Collections.singletonList(newFlowGroupTimeSeries)

	endCal = Calendar.getInstance()
	endCal.clear()
	endCal.set(Calendar.YEAR, 2013)
	endCal.set(Calendar.MONTH, 1)
	endTime = endCal.getTimeInMillis()

	calcTimeSeries = flowGroupCalc.calcTimeSeries(managerId, conduitGateFlowGroup, startTime, endTime, outputTimeSeriesList)
	computationResult = calcTimeSeries.get(newFlowGroupTimeSeries)
	#it could also be an error
	computationData = computationResult
	timeSeriesData = computationData.getTimeSeriesData()
	timeSeriesData.tabulateValues()