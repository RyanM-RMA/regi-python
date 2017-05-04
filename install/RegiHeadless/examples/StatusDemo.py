# the java Calendar class is used to create java Date objects
from java.util import Calendar
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
    startCal = Calendar.getInstance()
    startCal.clear()
    startCal.set(Calendar.YEAR, 2016)
    startCal.set(Calendar.MONTH, 3)
    startCal.set(Calendar.DATE, 2)
    startCal.set(Calendar.HOUR, -7)
    # Month 4 means May to java...

    # If the filepath does not end in .jpg then the image will be saved in png format.
    filepath = "J:\\temp\\headless\\stream.jpg"

    # Generate a single image,
    #   at a single location within a basin,
    #   at single date,
    #   with a single chart template
    #   and write to specified file.
    #print "Demonstrating a call to generateStreamStatusImage"
    #streamStatus.generateStreamStatusImage("SWF", "RSRT2", "00-Flood Control Focus View",
    #                      startCal.getTime(), 
    #                    800, 600,                        
    #                      filepath)

    #print "Demonstrating a call to generateReservoirStatusImage"
    #streamStatus.generateReservoirStatusImage("SWF", "GPVT2", "00-Flood Control Focus View",
    #                      startCal.getTime(), 
    #                    800, 600,                        
    #                      "J:\\temp\\headless\\reservoir_GPVT2.jpg")

    print "Demonstrating a call to generateReleasesStatusImage"
    streamStatus.generateReleasesStatusImage("SWF", "WTYT2", "00-Flood Control Focus View",
                          startCal.getTime(), 
                        800, 600,                        
                          "J:\\temp\\headless\\gate_WTYT2.jpg")
    
if __name__ == "__builtin__":
    Usage()
    headless_examples()

if __name__ == "__main__":
    Usage()