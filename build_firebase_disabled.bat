@echo off
REM build_firebase_disabled.bat - Creates placeholder google-services.json if missing and runs assembleDebug
SETLOCAL EnableDelayedExpansion
cd /d %~dp0
IF NOT EXIST app\google-services.json (
  echo Creating placeholder app\google-services.json ...
  > app\google-services.json echo {^
  >> app\google-services.json echo   "project_info": {^
  >> app\google-services.json echo     "project_number": "000000000000",^
  >> app\google-services.json echo     "project_id": "placeholder-project",^
  >> app\google-services.json echo     "storage_bucket": "placeholder-bucket"^
  >> app\google-services.json echo   },^
  >> app\google-services.json echo   "client": [^
  >> app\google-services.json echo     {^
  >> app\google-services.json echo       "client_info": {^
  >> app\google-services.json echo         "mobilesdk_app_id": "1:000000000000:android:0000000000000000",^
  >> app\google-services.json echo         "android_client_info": {^
  >> app\google-services.json echo           "package_name": "com.example.tugis3"^
  >> app\google-services.json echo         }^
  >> app\google-services.json echo       },^
  >> app\google-services.json echo       "oauth_client": [],^
  >> app\google-services.json echo       "api_key": [^
  >> app\google-services.json echo         {^
  >> app\google-services.json echo           "current_key": "PLACEHOLDER_API_KEY"^
  >> app\google-services.json echo         }^
  >> app\google-services.json echo       ],^
  >> app\google-services.json echo       "services": {^
  >> app\google-services.json echo         "analytics_service": {^
  >> app\google-services.json echo           "status": "ENABLED"^
  >> app\google-services.json echo         }^
  >> app\google-services.json echo       }^
  >> app\google-services.json echo     }^
  >> app\google-services.json echo   ],^
  >> app\google-services.json echo   "configuration_version": "1"^
  >> app\google-services.json echo }^
) ELSE (
  echo app\google-services.json already exists, skipping placeholder creation.
)

necho Optionally cleaning Gradle caches? (y/N)
set /p CLEAN=
if /i "%CLEAN%"=="Y" (
  echo Stopping Gradle daemons...
  call .\gradlew.bat --stop
  echo Removing configuration-cache and kotlin-dsl caches...
  rmdir /s /q .gradle\configuration-cache
  rmdir /s /q .gradle\8.13\kotlin-dsl
)
echo Running assembleDebug...
call .\gradlew.bat assembleDebug --stacktrace
ENDLOCAL
pause

