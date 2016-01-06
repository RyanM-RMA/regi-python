@echo off  
:: The headless scripts can also be executed from a bat file as demonstrated below
..\jre64\bin\java.exe -cp ..\lib\*;..\lib\ext\*;..\lib\modules\*;..\lib\sys\* usace.rowcps.headless.RegiCLI -Drowcps.timezone=GMT -p ..\\examples\\credentials.properties -f ..\\examples\\GateFlow.py
