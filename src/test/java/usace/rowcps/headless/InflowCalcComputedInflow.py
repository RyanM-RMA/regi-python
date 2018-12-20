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

# inflowCalc includes a setComputationStorageOptions function, which takes in either one or many
# InflowComputationStorageOption values.  This is used by computeInflow to determine if it should save additional
# computed values like evaporation as flow and project releases.
# 
# The InflowComputationStorageOption enum contains two values:
# 	EVAP_AS_FLOW
# 	PROJECT_RELEASES
#
# None is supported by setComputationStorageOptions as well, and clears out all storage options.
# 
# inflowCalc.setComputationStorageOptions is entirely optional, and if it's not used no additional computed values will
# be stored with the computed inflow
# 
# Example uses:
#inflowCalc.setComputationStorageOptions(None)
#inflowCalc.setComputationStorageOptions(InflowComputationStorageOption.EVAP_AS_FLOW)
#inflowCalc.setComputationStorageOptions(InflowComputationStorageOption.PROJECT_RELEASES)
inflowCalc.setComputationStorageOptions(InflowComputationStorageOption.EVAP_AS_FLOW, InflowComputationStorageOption.PROJECT_RELEASES)

# This computes and saves inflow for EUFA in May 2018 given the computation options set above
inflowCalc.computeInflow("SWT", "EUFA", startCal.getTime(), endCal.getTime())
