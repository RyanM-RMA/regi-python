:: The headless scripts can also be executed from a bat file as demonstrated below
..\jre64\bin\java.exe -Djava.util.logging.config.file=../config/properties/logging.properties -cp ..\lib\*;..\lib\cwms\*;..\lib\regi\*;..\lib\ext\*;..\lib\modules\*;..\lib\sys\* usace.rowcps.headless.RegiCLI -Drowcps.timezone=GMT -p ..\\examples\\credentials.properties -f ..\\examples\\Sigstages_Download.py
