# the java Calendar class is used to create java Date objects
from java.util import Calendar
import os, sys
import getopt
sys.path.insert(0, os.path.abspath(".."))
from examples import printInfo

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
    printInfo.printAll()
    # This gets a scriptable Stream Status object.
    streamStatus = registry.getCalculation(1.0, "Status")

    # To generate the status graphics, the following parameters are required
    # office_id: The office id used for finding the specified locations and map_template
    # location_id: The location for which the status graphic is desired
    # map_template_id: The id of the map template to use.
    # date and time: This specifies the date and time for which the image should be generated
    # width: width in pixels
    # height: height in pixels
    # filepath: Path of where to write the file.  The images may be generated with the extensions of either .jpg or .png

    # Configure the calendar for the date and time of the Basin Pie graphic
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