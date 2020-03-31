from java.util import Calendar
from java.util import TimeZone
from usace.rowcps.headless.tests import TestVariables

names = registry.getNames(1.0)
print "names", names

gateCalc = registry.getCalculation(1.0, "Gate Flow")

# Time zone must be set because the Solaris time zone is UTC
timeZone = TimeZone.getTimeZone("US/Central")
startCal = Calendar.getInstance(timeZone)
startCal.clear()
startCal.set(Calendar.YEAR, 2015)
startCal.set(Calendar.MONTH, 4)

endCal = Calendar.getInstance(timeZone)
endCal.clear()
endCal.set(Calendar.YEAR, 2015)
endCal.set(Calendar.MONTH, 6)

gateCalc.computeAll(TestVariables.OFFICE_ID, TestVariables.GATE_LOCATION, startCal.getTimeInMillis(), endCal.getTimeInMillis())
