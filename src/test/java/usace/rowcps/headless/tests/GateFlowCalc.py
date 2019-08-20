from java.util import Calendar
from usace.rowcps.headless.tests import TestVariables

names = registry.getNames(1.0)
print "names", names

gateCalc = registry.getCalculation(1.0, "Gate Flow")

startCal = Calendar.getInstance()
startCal.clear()
startCal.set(Calendar.YEAR, 2015)
startCal.set(Calendar.MONTH, 4)

endCal = Calendar.getInstance()
endCal.clear()
endCal.set(Calendar.YEAR, 2015)
endCal.set(Calendar.MONTH, 6)

gateCalc.computeAll(TestVariables.OFFICE_ID, TestVariables.GATE_LOCATION, startCal.getTimeInMillis(), endCal.getTimeInMillis())
