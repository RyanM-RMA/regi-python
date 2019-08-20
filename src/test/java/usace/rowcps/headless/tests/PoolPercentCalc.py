# the java Calendar class is used to create java Date objects
from java.util import Calendar
from usace.rowcps.headless.tests import TestVariables

# this gets a scriptable Pool Percent object
percentCalc = registry.getCalculation(1.0, "Pool Percent")

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


# percentCalc contains a single callable method calculatePoolPercents which takes the following arguments:
#   officeId
#   locationId
#   startDate
#   endDate
# This recomputes the pool percentages for SWT.LEWT2
percentCalc.calculatePoolPercents(TestVariables.OFFICE_ID, TestVariables.POOL_LOCATION, startCal.getTime(), endCal.getTime())


