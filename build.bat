@echo off

choice /C YN /N /M "Do you want to skip tests? (Y/N): "
if %errorlevel% equ 1 set YAFVA_SKIP_TESTS=-DskipTests

set YAFVA_IGS=-D--validator.ig[0]=il.core.fhir.r4#0.17.5
set YAFVA_TX_SERVER=-D--validator.tx-server=

mvnw.cmd clean package %YAFVA_SKIP_TESTS% %YAFVA_IGS% %YAFVA_TX_SERVER%