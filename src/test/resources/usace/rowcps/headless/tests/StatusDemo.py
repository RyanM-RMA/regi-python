# the java Calendar class is used to create java Date objects
from java.util import Calendar
from java.util import TimeZone
from usace.rowcps.headless.tests import TestVariables

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
    timeZone = TimeZone.getTimeZone("US/Central")
    startCal = Calendar.getInstance(timeZone)
    startCal.clear()
    startCal.set(Calendar.YEAR, 2018)
    startCal.set(Calendar.MONTH, 6)
    startCal.set(Calendar.DATE, 2)
    # Month 4 means May to java...

    # If the filepath does not end in .jpg then the image will be saved in png format.

    # Generate a single image,
    #   at a single location within a basin,
    #   at single date,
    #   with a single chart template
    #   and write to specified file.
    filePath = TestVariables.HEADLESS_FILE_LOCATION + "StatusGraphics\\"
    office = TestVariables.OFFICE_ID
    streamLoc = TestVariables.STREAM_GAGE_LOCATION
    projectLoc = TestVariables.INFLOW_LOCATION
    releasesLoc = TestVariables.GATE_LOCATION
    template = "RyanM Headless Testing"
    time = startCal.getTime()
    width = 800
    height = 600

    streamStatus.generateStreamStatusImage(office, streamLoc, template, time, width, height, filePath + "streamStatus.jpg")
    streamStatus.generateReservoirStatusImage(office, projectLoc, template, time, width, height, filePath + "reservoirStatus.jpg")
    streamStatus.generateReleasesStatusImage(office, releasesLoc, template, time, width, height, filePath + "releasesStatus.jpg")
    
if __name__ == "__builtin__":
    Usage()
    headless_examples()

if __name__ == "__main__":
    Usage()