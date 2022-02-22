@rem 프로그램 경로 설정 변경
@set INSTALL_HOME=C:\InternetEntSvrClient

@set CONFIG_FILE=%INSTALL_HOME%\env.cfg

@set SERVICE_CHECK=N

java -jar %INSTALL_HOME%\lib\ksnetInternetEntSvrClient.jar %CONFIG_FILE% %SERVICE_CHECK% -out %INSTALL_HOME%\log\out.log -err %INSTALL_HOME%\log\err.log
