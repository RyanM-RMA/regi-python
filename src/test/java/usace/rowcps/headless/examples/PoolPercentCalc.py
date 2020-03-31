# the java Calendar class is used to create java Date objects
from java.util import Calendar
from java.util import TimeZone

# this gets a scriptable Pool Percent object
percentCalc = registry.getCalculation(1.0, "Pool Percent")

# Time zone must be set because the Solaris time zone is UTC
timeZone = TimeZone.getTimeZone("US/Central")
# configure the start calendar
startCal = Calendar.getInstance(timeZone)
startCal.clear()
startCal.set(Calendar.YEAR, 2015)
startCal.set(Calendar.MONTH, 4)

# configure the end calendar
endCal = Calendar.getInstance(timeZone)
endCal.clear()
endCal.set(Calendar.YEAR, 2015)
endCal.set(Calendar.MONTH, 6)


# percentCalc contains a single callable method calculatePoolPercents which takes the followind arguments:
#   officeId
#   locationId
#   startDate
#   endDate
# This recomputes the pool percentages for SWF.LEWT2
percentCalc.calculatePoolPercents("SWF", "LEWT2",  startCal.getTime(), endCal.getTime())


