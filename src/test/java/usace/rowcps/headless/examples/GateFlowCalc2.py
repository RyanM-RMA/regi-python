from java.util import Calendar

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

#gateCalc.computeFlowGroup("SWF", "LEWT2",  startCal.getTimeInMillis(), endCal.getTimeInMillis(), "Flow.LEWT2.ConduitGate_Total")

gateCalc.computeAll("SWF", "LEWT2",  startCal.getTimeInMillis(), endCal.getTimeInMillis())


