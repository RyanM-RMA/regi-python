import logging
from regi_cli import regi_session, run_headless

# Initialize logger for this specific script
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def compute_gate_flow(registry):
    """
    This function contains your specific calculation logic.
    The 'registry' object is passed in by the launcher.
    """
    # Java imports must happen here (after JVM has started)
    from java.util import Calendar, TimeZone

    logger.info("Starting Gate Flow Calculation...")

    # Use the registry passed into the function
    gate_calc = registry.getCalculation(1.0, "Gate Flow")

    # Time zone must be set because the Solaris time zone is UTC
    time_zone = TimeZone.getTimeZone("US/Central")

    start_cal = Calendar.getInstance(time_zone)
    start_cal.clear()
    start_cal.set(2022, 4, 1) # YEAR, MONTH (0-indexed, 4=May), DAY

    end_cal = Calendar.getInstance(time_zone)
    end_cal.clear()
    end_cal.set(2022, 6, 1) # YEAR, MONTH (0-indexed, 6=July), DAY

    logger.info(f"Computing for SWT/TENK from {start_cal.getTime()} to {end_cal.getTime()}")

    # Execute the calculation
    gate_calc.computeAll("SWT", "TENK", start_cal.getTimeInMillis(), end_cal.getTimeInMillis())

    logger.info("Calculation successful.")

if __name__ == "__main__":
    # Use the context manager to handle JVM lifecycle and run the logic
    with regi_session():
        run_headless(compute_gate_flow)


