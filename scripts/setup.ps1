# Windows version of setup.sh. Run in PowerShell:
#   .\setup.ps1                     install Kids Home, disable ad apps, family DNS
#   .\setup.ps1 -Replace            also disable the stock TCL launcher
#   .\setup.ps1 -Scan               just list TCL / Google packages on the TV
#   .\setup.ps1 -TvIp 10.73.1.89    use a different TV address
param(
    [string]$TvIp = "10.73.1.89",
    [switch]$Replace,
    [switch]$Scan
)

$Tv = "${TvIp}:5555"
$Apk = Join-Path $PSScriptRoot "KidsHome.apk"
$ApkUrl = "https://github.com/ayahya1983/My-tv/releases/latest/download/KidsHome.apk"
$KidsHome = "com.mytv.kidshome/.HomeActivity"
$State = Join-Path $PSScriptRoot ".stock-launcher"

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    Write-Host "adb not found. Install it with:  winget install Google.PlatformTools"
    exit 1
}

function TvShell { & adb -s $Tv shell @args }

Write-Host "Connecting to $Tv ... (accept 'Allow debugging' on the TV if asked)"
for ($i = 0; $i -lt 30; $i++) {
    & adb connect $Tv | Out-Null
    if ((& adb -s $Tv get-state 2>$null) -eq "device") { break }
    Start-Sleep -Seconds 2
}
if ((& adb -s $Tv get-state 2>$null) -ne "device") {
    Write-Host "Could not connect. Check the TV is on, Network/USB debugging is enabled and the IP is right."
    exit 1
}

if ($Scan) {
    TvShell pm list packages | ForEach-Object { $_ -replace '^package:', '' } |
        Where-Object { $_ -match 'tcl|recommend|partner|ads' } | Sort-Object
    exit 0
}

Write-Host "== 1/4 Installing Kids Home"
if (-not (Test-Path $Apk)) {
    Write-Host "Downloading $ApkUrl"
    try { Invoke-WebRequest -Uri $ApkUrl -OutFile $Apk -ErrorAction Stop }
    catch {
        Write-Host "Download failed. Get KidsHome.apk from the repo's Releases page and put it next to this script."
        exit 1
    }
}
& adb -s $Tv install -r $Apk

Write-Host "== 2/4 Disabling ad / promo apps"
$installed = TvShell pm list packages | ForEach-Object { $_.Trim() }
Get-Content (Join-Path $PSScriptRoot "debloat-list.txt") |
    Where-Object { $_ -notmatch '^\s*(#|$)' } | ForEach-Object {
        $pkg = $_.Trim()
        if ($installed -contains "package:$pkg") {
            TvShell pm disable-user --user 0 $pkg | Out-Null
            Write-Host "  disabled $pkg"
        }
    }

Write-Host "== 3/4 Family-safe, ad-blocking DNS (AdGuard Family)"
TvShell settings put global private_dns_mode hostname
TvShell settings put global private_dns_specifier family.adguard-dns.com

Write-Host "== 4/4 Making Kids Home the home screen"
try { TvShell cmd package set-home-activity $KidsHome | Out-Null } catch { }
$current = (TvShell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME |
    Select-Object -Last 1).Trim()
if (-not $current.StartsWith("com.mytv.kidshome/")) {
    if ($Replace) {
        $stock = $current.Split("/")[0]
        Set-Content -Path $State -Value $stock
        TvShell pm disable-user --user 0 $stock | Out-Null
        Write-Host "  disabled stock launcher $stock"
    } else {
        Write-Host "  The TV still prefers: $current"
        Write-Host "  If the Home button doesn't open Kids Home, run: .\setup.ps1 -Replace"
    }
}
TvShell input keyevent KEYCODE_HOME

Write-Host ""
Write-Host "Done. Default parent PIN is 1234 - change it from the Parents button."
