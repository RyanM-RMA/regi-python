# the java Calendar class is used to create java Date objects
from java.util import Calendar
from java.util import TimeZone
from usace.rowcps.headless.calculator.inflow import InflowComputationStorageOption
from usace.rowcps.headless.tests import TestVariables

# this gets a ScriptableInflow instance.
inflowCalc = registry.getCalculation(1.0, "Inflow")

# configure the start calendar
startCal = Calendar.getInstance(TimeZone.getTimeZone('US/Central'))


startCal.clear()
startCal.set(Calendar.YEAR, 2018)
startCal.set(Calendar.MONTH, 4)

endCal = Calendar.getInstance(TimeZone.getTimeZone('US/Central'))
endCal.clear()
endCal.set(Calendar.YEAR, 2018)
endCal.set(Calendar.MONTH, 4)
endCal.set(Calendar.DAY_OF_MONTH, 4)


# This code has been deprecated, but the API must exist.
storageOptionsSet = False
try:
	inflowCalc.setComputationStorageOptions(InflowComputationStorageOption.EVAP_AS_FLOW, InflowComputationStorageOption.PROJECT_RELEASES)
	storageOptionsSet = True
except:
	print "Exception occurred during setComputationStorageOptions, this is expected"

if storageOptionsSet:
	throw Exception("ScriptableInflow::setComputationStorageOptions should not be working")

# This computes and saves inflow for EUFA in May 2018 given the computation options set above
inflowCalc.computeInflow(TestVariables.OFFICE_ID, TestVariables.INFLOW_LOCATION, startCal.getTime(), endCal.getTime())
