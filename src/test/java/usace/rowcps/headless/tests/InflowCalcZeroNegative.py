# the java Calendar class is used to create java Date objects
from java.util import Calendar
from java.util import TimeZone
from usace.rowcps.headless.tests import TestVariables

# this gets a ScriptableInflow instance.
inflowCalc = registry.getCalculation(1.0, "Inflow")

# configure the start calendar
startCal = Calendar.getInstance(TimeZone.getTimeZone('US/Central'))

startCal.clear()
startCal.set(Calendar.YEAR, 2018)
startCal.set(Calendar.MONTH, 7)


# inflowCalc contains 4 callable methods:
# autoAdjust
# balanceAll
# cloneInflows
# zeroNegatives

# Each method takes the following arguments:
#   officeId
#   locationId
#   startDate

# autoAdjust also takes booleans:
#	useLimits
#	freezeRain

# This autoBalances ALAT2
inflowCalc.zeroNegatives(TestVariables.OFFICE_ID, TestVariables.INFLOW_LOCATION,  startCal.getTime())

