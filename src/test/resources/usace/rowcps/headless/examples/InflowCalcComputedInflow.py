# the java Calendar class is used to create java Date objects
from java.util import Calendar
from java.util import TimeZone
from usace.rowcps.headless.calculator.inflow import InflowComputationStorageOption

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

# This computes and saves inflow for EUFA in May 2018 given the computation options set above
inflowCalc.computeInflow("SWT", "EUFA", startCal.getTime(), endCal.getTime())
