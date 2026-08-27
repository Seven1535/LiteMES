# Temp script: publish latest snapshot to Nacos (UTF-8 byte-safe), delete after run
$nacos = "http://localhost:8848"
$name = "litemes-base.yml"
$bytes = [System.IO.File]::ReadAllBytes("d:\desktop\java\program\LiteMES\lite-mes\nacos-config\$name")
$form = "dataId=$name&group=DEFAULT_GROUP&tenant=litemes-dev&type=yaml&content=" + [System.Uri]::EscapeDataString([System.Text.Encoding]::UTF8.GetString($bytes))
$body = [System.Text.Encoding]::ASCII.GetBytes($form)
$result = Invoke-RestMethod -Method Post -Uri "$nacos/nacos/v1/cs/configs" -ContentType "application/x-www-form-urlencoded" -Body $body
Write-Host "$name publish => $result"
