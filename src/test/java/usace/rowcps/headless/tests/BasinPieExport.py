# the java Calendar class is used to create java Date objects
from java.util import Calendar
import os, sys
import getopt
from usace.rowcps.headless.tests import TestVariables
sys.path.insert(0, os.path.abspath(".."))
#from examples import printInfo

def Usage():
    msg = """
    This file demonstrates how to utilize the Headless Basin Pie functionality.
    This script is not meant to be executed by itself but should be executed from the RegiCLI.
    Among other things the RegiCLI opens the specified study and connects to the database using the specified
    credentials.  Once RegiCLI has completed the preliminary steps it will execute the specified python scripts
    and allow the scripts to call into the Java classes and methods.
    This file is an example of how the Basin Pie functionality can be scripted.
    """
    print msg

def headless_examples():
    # Configure the calendar for the date and time of the Basin Pie graphic
    startCal = Calendar.getInstance()
    startCal.clear()
    startCal.set(Calendar.YEAR, 2016)
    startCal.set(Calendar.MONTH, 4)
    startCal.set(Calendar.DATE, 5)
    # Month 4 means May to java...

    # Generate multiple images
    # First built the dates
    for i in range(0, 3):
        startCal.add(Calendar.DATE, 1)

    # When generating multiple image files the filepath may contain replacement keywords.
    # If these keywords are found in the filepath they are replaced with named piece of data.
    # Once the replacements are made, illegal characters in the filename are replaced with '_'.
    # The following keywords are currently recognized:
    #     %date%
    #     %office_id%
    #     %location_id%
    #     %chart_template_id%
    #     %basin_id%
    #     %image_format%
    # Example: filepath like:
    #       "J:\\temp\\headless\\%office_id%_%basin_id%_%location_id%_%chart_template_id%_%date%.png"
    # Will generate the following files:
    #       J:\\temp\\headless\\SWT_Trinity_R_Basin_RSRT2_Conservation Pool (static)_2016-04-02T00_00_00Z.png

    fileName = "test_%office_id%_%basin_id%_%location_id%_%chart_template_id%_%date%.png"
    filePath = TestVariables.HEADLESS_FILE_LOCATION + "BasinPieExport\\"

    # Generate a basin pie image,
    #   for each of the specified locations in the specified basin,
    #   for each of the dates,
    #   for each of the chart templates
    #   replace %% patterns in the specified filepath template
    #   and write each image to its generated filepath
#    print "Demonstrating a call to generateBasinPieImages"
#    generateBasinPieImages("SWT", ["LOLT2"], "Trinity_R_Basin",
#                          dates, 700, 807,
#                          ["Design Capacity"],
#                          filepath)

    # Generate an image
    #   for the locations found in a basin
    #   for each of the dates,
    #   for each of the chart templates
    #   replace %% patterns in the specified filepath template
    #   and write each image to its generated filepath

    office = TestVariables.OFFICE_ID
    templateId1 = "RyanM Headless Testing2"
    templateId2 = "Conservation Pool (static)"
    time = startCal.getTime()
    loc1 = TestVariables.GATE_LOCATION
    loc2 = TestVariables.INFLOW_LOCATION
    loc3 = TestVariables.POOL_LOCATION
    loc4 = TestVariables.LOCATION_4
    basin = "CANADIAN_BASIN"
    group = "A Small Group"
    width = 797
    height = 682

    print "Demonstrating a call to generateBasinPieImageForGroup"    
    generateBasinPieImageForGroup(office, loc1, group, time, width, height, templateId1, filePath + "generateBasinPieImageForGroup\\" + fileName)
    
    print "Demonstrating a call to generateBasinPieImagesForGroup"
    generateBasinPieImagesForGroup(office, [loc3,loc2], group, [time], width, height, [templateId1], filePath + "generateBasinPieImagesForGroup\\" + fileName)

    print "Demonstrating a call to generateBasinPieImageForBasin"
    generateBasinPieImageForBasin(office, loc1, basin, time, width, height, templateId1, filePath + "generateBasinPieImageForBasin\\" + fileName)

    print "Demonstrating a call to generateBasinPieImagesForBasin"
    generateBasinPieImagesForBasin(office, [loc4, loc1], basin, [time], width, height,[templateId1], filePath + "generateBasinPieImagesForBasin\\" + fileName)

    print "Demonstrating a call to generateAllBasinPieImagesForBasin"
    generateAllBasinPieImagesForBasin(office, basin, [time], width, height, [templateId2], filePath + "generateAllForBasin\\" + fileName)

    print "Demonstrating a call to generateAllBasinPieImagesForGroup"
    generateAllBasinPieImagesForGroup(office, group, [time], width, height, [templateId2], filePath + "generateAllForGroup\\" + fileName)


def generateBasinPieImageForBasin(office_id, location_id, basin_id,
                          date, width, height,
                          chart_template_id,
                          filepath):
    """

    :param office_id: The office id used for finding the specified locations, basin and chart_template
    :param location_id: The location for which the basin pie image is desired
    :param basin_id: The id of the Basin
    :param date: This specifies the time for which the image should be generated
    :param width: width in pixels
    :param height: height in pixels
    :param chart_template_id: The id of the chart template to use.
    :param filepath: Path of where to write the file.
    """
    basinPie = registry.getCalculation(1.0, "Status")
    basinPie.generateBasinPieImageForBasin(office_id, location_id, basin_id,
                          date, width, height,
                          chart_template_id,
                          filepath)

def generateBasinPieImagesForBasin(office_id, location_ids, basin_id,
                          dates, width, height,
                          chart_template_ids,
                          filepath):
    # type: (string, string, string, [date], int, int, [string], string) -> void
    """

    :param office_id: The office id used for finding the specified locations, basin and chart_template
    :param location_ids: A list of the locations for which basin pie images are desired
    :param basin_id: The id of the Basin
    :param dates: A list of the times for which the image should be generated
    :param width: width in pixels
    :param height: height in pixels
    :param chart_template_ids: The ids of the chart template to use.
    :param filepath: Path (template) of where to write the file.
    """
    basinPie = registry.getCalculation(1.0, "Status")
    basinPie.generateBasinPieImagesForBasin(office_id, location_ids, basin_id,
                          dates, width, height,
                          chart_template_ids,
                          filepath)

def generateBasinPieImageForGroup(office_id, location_id, group_id,
                          date, width, height,
                          chart_template_id,
                          filepath):
    # type: (string, string, string, [date], int, int, [string], string) -> void
    """

    :param office_id: The office id used for finding the specified locations, basin and chart_template
    :param location_id: A list of the locations for which basin pie images are desired
    :param group_id: The id of the Group
    :param date: A list of the times for which the image should be generated
    :param width: width in pixels
    :param height: height in pixels
    :param chart_template_id: The ids of the chart template to use.
    :param filepath: Path (template) of where to write the file.
    """
    basinPie = registry.getCalculation(1.0, "Status")
    basinPie.generateBasinPieImageForGroup(office_id, location_id, group_id,
                          date, width, height,
                          chart_template_id,
                          filepath)

def generateBasinPieImagesForGroup(office_id, location_ids, group_id,
                          dates, width, height,
                          chart_template_ids,
                          filepath):
    # type: (string, string, string, [date], int, int, [string], string) -> void
    """

    :param office_id: The office id used for finding the specified locations, basin and chart_template
    :param location_ids: A list of the locations for which basin pie images are desired
    :param group_id: The id of the Group
    :param dates: A list of the times for which the image should be generated
    :param width: width in pixels
    :param height: height in pixels
    :param chart_template_ids: The ids of the chart template to use.
    :param filepath: Path (template) of where to write the file.
    """
    basinPie = registry.getCalculation(1.0, "Status")
    basinPie.generateBasinPieImagesForGroup(office_id, location_ids, group_id,
                          dates, width, height,
                          chart_template_ids,
                          filepath)

def generateAllBasinPieImagesForBasin(office_id, basin_id,
                          dates, width, height,
                          chart_template_ids,
                          filepath):
    basinPie = registry.getCalculation(1.0, "Status")
    basinPie.generateAllBasinPieImagesForBasin(office_id,  basin_id,
                          dates, width, height,
                          chart_template_ids,
                          filepath)

def generateAllBasinPieImagesForGroup(office_id, group_id,
                          dates, width, height,
                          chart_template_ids,
                          filepath):
    basinPie = registry.getCalculation(1.0, "Status")
    basinPie.generateAllBasinPieImagesForGroup(office_id,  group_id,
                          dates, width, height,
                          chart_template_ids,
                          filepath)

if __name__ == "__builtin__":
    Usage()
    headless_examples()

if __name__ == "__main__":
    Usage()