# Windows version of restore.sh: undoes setup.ps1.
param([string]$TvIp = "10.73.1.89")

$Tv = "${TvIp}:5555"
$State = Join-Path $PSScriptRoot ".stock-launcher"
function TvShell { & adb -s $Tv shell @args }

& adb connect $Tv | Out-Null
if (Test-Path $State) {
    $stock = (Get-Content $State).Trim()
    TvShell pm enable $stock | Out-Null
    Write-Host "re-enabled $stock"
    Remove-Item $State
}
Get-Content (Join-Path $PSScriptRoot "debloat-list.txt") |
    Where-Object { $_ -notmatch '^\s*(#|$)' } | ForEach-Object {
        TvShell pm enable $_.Trim() 2>$null | Out-Null
    }
TvShell settings put global private_dns_mode opportunistic
TvShell settings delete global private_dns_specifier | Out-Null
& adb -s $Tv uninstall com.mytv.kidshome 2>$null | Out-Null
TvShell input keyevent KEYCODE_HOME
Write-Host "TV restored."
