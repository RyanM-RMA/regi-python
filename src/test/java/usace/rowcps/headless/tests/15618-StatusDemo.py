# the java Calendar class is used to create java Date objects
from java.util import Calendar
from java.util import TimeZone
import sys
import getopt

def Usage():
    msg = """
    This file demonstrates how to utilize the Headless Stream Status functionality.
    This script is not meant to be executed by itself but should be executed from the RegiCLI.
    Among other things the RegiCLI opens the specified study and connects to the database using the specified
    credentials.  Once RegiCLI has completed the preliminary steps it will execute the specified python scripts
    and allow the scripts to call into the Java classes and methods.
    This file is an example of how the Stream Status functionality can be scripted.
    """
    print msg

def headless_examples():
    # This gets a scriptable Stream Status object.
    streamStatus = registry.getCalculation(1.0, "Status")

    # Configure the calendar
	#Time zone must be set because the Solaris time zone is UTC
    timeZone = TimeZone.getTimeZone("US/Central")
    startCal = Calendar.getInstance(timeZone)
    startCal.clear()
    startCal.set(Calendar.YEAR, 2018)
    startCal.set(Calendar.MONTH, 7)
    startCal.set(Calendar.DATE, 27)
    startCal.set(Calendar.HOUR_OF_DAY, 0)
    # Month 4 means May to java...

    # If the filepath does not end in .jpg then the image will be saved in png format.
    # Generate a single image,
    #   at a single location within a basin,
    #   at single date,
    #   with a single chart template
    #   and write to specified file.
    print "Demonstrating a call to generateStreamStatusImage"
    filepath = "E:\\temp\\15618\\"
    maptemplate = "OpsSupport_133658"
    officeID = "SWT"
    #streamFilepath = "J:\\temp\\headless\\StatusGraphics\\streamStatus.jpg"
    fileStream = filepath + "A_Stream_"
    fileReservoir = filepath + "B_Reservoir_"
    fileReleases = filepath + "C_Releases_"

    width = 640
    height = 480

    print "Demonstrating a call to generateStreamStatusImage"
    #streamStatus.generateStreamStatusImage(officeID, "RIPL", maptemplate, startCal.getTime(), width, height, fileStream+"RIPL.png")
    #streamStatus.generateStreamStatusImage(officeID, "CHEW", maptemplate, startCal.getTime(), width, height, fileStream+"CHEW.png")
    #streamStatus.generateStreamStatusImage(officeID, "TULA", maptemplate, startCal.getTime(), width, height, fileStream+"TULA.png")

    print "Demonstrating a call  to generateReservoirStatusImage"
    #reservoirFilePath = "J:\\temp\\headless\\StatusGraphics\\reservoirStatus.jpg"
    #streamStatus.generateReservoirStatusImage(officeID, "KEYS", maptemplate, startCal.getTime(), width, height, fileReservoir+"KEYS.png")
    #streamStatus.generateReservoirStatusImage(officeID, "FGIB", maptemplate, startCal.getTime(), width, height, fileReservoir+"FGIB.png")

    for i in range(1):
        streamStatus.generateReservoirStatusImage(officeID, "SKIA", maptemplate, startCal.getTime(), width, height, fileReservoir+"SKIA" + str(i) + ".png")
        startCal.add(Calendar.HOUR_OF_DAY, 1)

    print "Demonstrating a call to generateReleasesStatusImage"
    #releasesFilePath = "J:\\temp\\headless\\StatusGraphics\\releasesStatus.jpg"
    #streamStatus.generateReleasesStatusImage(officeID, "FGIB", maptemplate, startCal.getTime(), width, height, fileReleases+"FGIB.png")
    #streamStatus.generateReleasesStatusImage(officeID, "KEYS", maptemplate, startCal.getTime(), width, height, fileReleases+"KEYS.png")
    #streamStatus.generateReleasesStatusImage(officeID, "TENK", maptemplate, startCal.getTime(), width, height, fileReleases+"TENK.png")
    
if __name__ == "__builtin__":
    Usage()
    headless_examples()

if __name__ == "__main__":
    Usage()