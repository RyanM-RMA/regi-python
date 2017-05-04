from java.util import TimeZone

def printTimeZone():
    tz = TimeZone.getDefault()
    id = tz.getID()
    useDaylight = tz.useDaylightTime()
    print "{0}, Observes daylight: {1}".format(id, str(useDaylight)) 

def printAll():
    printTimeZone()