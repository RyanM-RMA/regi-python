# the java Calendar class is used to create java Date objects
from java.util import Calendar
from java.util import TimeZone
from usace.rowcps.headless.calculator.inflow import InflowStorageOptions

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

# This example creates storage options which can be used by the inflow calc to identify additional computed data that will be saved to the database.
# Usage of this class is entirely optional, and if it is not used, it will save any additional computed data along with the compute.
# InflowStorageOptions is also only used by the computeInflow function.
# 
# There are three methods for creating this object:
# 	inflowStorageOptions = InflowStorageOptions()
# 	inflowStorageOptions = InflowStorageOptions.storeAllComputedData()
# 	inflowStorageOptions = InflowStorageOptions.doNotStoreAllComputedData()
# 
inflowStorageOptions = InflowStorageOptions()

# InflowStorageOptions has 4 publicly available methods for storing or not storing computed data
# 	inflowStorageOptions.storeComputedEvapAsFlow()
# 	inflowStorageOptions.storeComputedProjectReleases()
# 	inflowStorageOptions.doNotStoreComputedEvapAsFlow()
# 	inflowStorageOptions.doNotStoreComputedProjectReleases()
# 
inflowStorageOptions.storeComputedEvapAsFlow()
inflowStorageOptions.storeComputedProjectReleases()

# inflowCalc  includes a setStorageOptions function, which takes in an InflowStorageOptions instance, or None
# 
# Passing in None is effectively the same as calling:
# 	inflowCalc.setStorageOptions(InflowStorageOptions.doNotStoreAllComputedData())
#
#inflowCalc.setStorageOptions(inflowStorageOptions)
inflowCalc.setStorageOptions(None)

# This computes and saves inflow for EUFA in May 2018 given the InflowStorageOptions set above
inflowCalc.computeInflow("SWT", "EUFA",  startCal.getTime(), endCal.getTime())
