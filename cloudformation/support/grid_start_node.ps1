$instanceId=(Invoke-webrequest -Uri http://169.254.169.254/latest/meta-data/instance-id).content
$instanceIp=(Invoke-webrequest -Uri http://169.254.169.254/latest/meta-data/local-ipv4).content
Invoke-webrequest "http://169.254.169.254/latest/user-data" -OutFile "c:\grid\data.zip" -passthru | select -Expand headers
$shell = new-object -com shell.application
$zip = $shell.NameSpace(“C:\grid\data.zip”)
foreach($item in $zip.items())
{
$shell.Namespace(“C:\grid”).copyhere($item)
}
echo $instanceId
(Get-Content c:\grid/nodeConfigTemplate.json).replace('<INSTANCE_ID>', $instanceId) | Set-Content c:\grid\nodeConfig.json
(Get-Content c:\grid\nodeConfig.json).replace('<IP>', $instanceIp) | Set-Content c:\grid\nodeConfig.json
java -jar c:\grid\selenium-server-standalone.jar -role node -nodeConfig /grid/nodeConfig.json -D"webdriver.ie.driver=/grid/IEDriverServer.exe" -D"webdriver.chrome.driver=/grid/chromedrive.exe"  -D"webdriver.gecko.driver=/grid/geckodriver.exe" -log /grid/grid.log