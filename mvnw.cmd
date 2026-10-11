@echo off
setlocal
set "MAVEN_VERSION=3.9.11"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%"
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $url='https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip'; $zip=Join-Path $env:TEMP 'apache-maven-%MAVEN_VERSION%-bin.zip'; New-Item -ItemType Directory -Force -Path '%MAVEN_HOME%' | Out-Null; Invoke-WebRequest -Uri $url -OutFile $zip; Expand-Archive -Path $zip -DestinationPath '%MAVEN_HOME%' -Force; $inner=Join-Path '%MAVEN_HOME%' 'apache-maven-%MAVEN_VERSION%'; Get-ChildItem -LiteralPath $inner | Move-Item -Destination '%MAVEN_HOME%' -Force; Remove-Item -LiteralPath $inner -Recurse -Force; Remove-Item -LiteralPath $zip -Force"
  if errorlevel 1 exit /b 1
)
call "%MAVEN_HOME%\bin\mvn.cmd" -Dmaven.repo.local="%CD%\.m2\repository" %*
