#  Copyright (c) 2026
#  United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
#  All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
#  Source may not be released without written approval from HEC

import os
import logging
import jpype
import jpype.imports
from contextlib import contextmanager
from pathlib import Path

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger("regi-launcher")

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
LIB_PATH = os.path.join(BASE_DIR, "lib", "*")
java_home = os.environ.get('JAVA_HOME')
java_bin = os.path.join(java_home, 'bin')
if java_bin not in os.environ['PATH']:
    os.environ['PATH'] = java_bin + os.pathsep + os.environ['PATH']

@contextmanager
def regi_session():
    """
    Context manager to handle JVM lifecycle. 
    Usage:
        with regi_session():
            run_headless(my_func)
    """
    if not jpype.isJVMStarted():
        agent_jar = Path(os.path.join(BASE_DIR, "lib", "opentelemetry-javaagent.jar"))
        agent_flag = f"-javaagent:{agent_jar.absolute()}"

        logger.info(f"Starting JVM with agent: {agent_flag}")
        logger.info("Starting JVM...")
        jpype.startJVM(
            jpype.getDefaultJVMPath(),
            agent_flag,
            convertStrings=True,
            classpath=[LIB_PATH]
        )

    try:
        yield
    finally:
        if jpype.isJVMStarted():
            logger.info("Shutting down JVM...")
            jpype.shutdownJVM()

def run_headless(calculation_callback):
    # We must import these inside the function or after JVM starts
    from usace.rowcps.headless import HeadlessRegiDomainFactory, RegiCalcRegistry
    from usace.rowcps.regi.factories import RowcpsExecutorService
    from java.util.concurrent import TimeUnit
    GlobalOpenTelemetry = jpype.JClass("io.opentelemetry.api.GlobalOpenTelemetry")
    builder = GlobalOpenTelemetry.getTracer("regi-headless").spanBuilder("runHeadless")
    builder.setAttribute("cda.url", os.environ.get("CDA_URL", "unknown"))
    builder.setAttribute("cwms.office", os.environ.get("OFFICE_ID", "unknown"))
    root_span = builder.startSpan()
    scope = root_span.makeCurrent()
    try:
        factory = HeadlessRegiDomainFactory()
        logger.info("Attempting to create RegiDomain...")
        regi_domain = factory.createDomain()

        if regi_domain is not None:
            manager_id = factory.getManagerId()
            registry = RegiCalcRegistry(regi_domain, manager_id)

            try:
                logger.info("Executing callback...")
                calculation_callback(registry)
                regi_domain.commitData(manager_id)
            except Exception as e:
                logger.error("Execution failed.", exc_info=True)
                raise
            finally:
                _shutdown_executor(manager_id)
                regi_domain.closing()
    except (jpype.JException, Exception) as ex:
        # 2. Mirroring the Java catch block
        root_span.recordException(ex)
        StatusCode = jpype.JClass("io.opentelemetry.api.trace.StatusCode")
        root_span.setStatus(StatusCode.ERROR)

        if isinstance(ex, jpype.JException):
            logger.error("Java Exception occurred during headless execution:")
            ex.printStackTrace()
        else:
            logger.error(f"Python Exception occurred during headless execution: {ex}")

        # Mirroring: System.exit(-1)
        scope.close()
        root_span.end()
    finally:
        # 3. Ensuring cleanup
        scope.close()
        root_span.end()

def _shutdown_executor(manager_id):
    from usace.rowcps.regi.factories import RowcpsExecutorService
    from java.util.concurrent import TimeUnit
    res = RowcpsExecutorService.getInstance(manager_id)
    res.shutdown()
    if not res.awaitTermination(3000, TimeUnit.MILLISECONDS):
        res.shutdownNow()