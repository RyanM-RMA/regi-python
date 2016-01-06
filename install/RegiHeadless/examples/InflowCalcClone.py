# the java Calendar class is used to create java Date objects
from java.util import Calendar
from java.util import TimeZone
from java.util import GregorianCalendar

# this gets a scriptable Pool Percent object
inflowCalc = registry.getCalculation(1.0, "Inflow")

# configure the start calendar

startCal = GregorianCalendar(TimeZone.getTimeZone('US/Central'))


startCal.clear()
startCal.set(Calendar.YEAR, 2015)
startCal.set(Calendar.MONTH, 4)


# inflowCalc contains 4 callable methods:
# autoAdjust
# balanceAll
# cloneInflows
# zeroNegatives

# Each method takes the followind arguments:
#   officeId
#   locationId
#   startDate

# autoAdjust also takes booleans:
#	useLimits
#	freezeRain

# This autoBalances ALAT2
inflowCalc.cloneInflows("SWF", "ALAT2",  startCal.getTime(), False, False)


