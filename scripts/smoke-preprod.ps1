[CmdletBinding()]
param(
    [string]$FrontendUrl = "http://localhost:8080",
    [string]$MotherApiUrl = "http://localhost:3200/api/children",
    [string[]]$ChildHealthUrls = @(
        "http://localhost:3101/health",
        "http://localhost:3102/health",
        "http://localhost:3103/health"
    ),
    [int]$TimeoutSec = 20
)

$ErrorActionPreference = "Stop"

$results = @()

function Add-Result {
    param(
        [string]$Name,
        [bool]$Passed,
        [string]$Details
    )

    $status = if ($Passed) { "OK" } else { "KO" }
    $results += [pscustomobject]@{
        Test    = $Name
        Status  = $status
        Details = $Details
    }
}

function Invoke-Http {
    param(
        [Parameter(Mandatory = $true)][string]$Url
    )

    return Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec $TimeoutSec
}

# Test 1: Frontend is reachable and returns HTTP 200.
try {
    $front = Invoke-Http -Url $FrontendUrl
    $frontOk = ($front.StatusCode -eq 200)
    Add-Result -Name "Frontend HTTP" -Passed $frontOk -Details "status=$($front.StatusCode) url=$FrontendUrl"
} catch {
    Add-Result -Name "Frontend HTTP" -Passed $false -Details $_.Exception.Message
}

# Test 2: Mother API is reachable and returns HTTP 200.
$motherJson = $null
try {
    $mother = Invoke-Http -Url $MotherApiUrl
    $motherOk = ($mother.StatusCode -eq 200)
    Add-Result -Name "Mother API HTTP" -Passed $motherOk -Details "status=$($mother.StatusCode) url=$MotherApiUrl"

    if ($motherOk) {
        try {
            $motherJson = $mother.Content | ConvertFrom-Json
            $jsonOk = ($null -ne $motherJson)
            Add-Result -Name "Mother API JSON" -Passed $jsonOk -Details "json_parse=ok"
        } catch {
            Add-Result -Name "Mother API JSON" -Passed $false -Details "json_parse=failed: $($_.Exception.Message)"
        }
    }
} catch {
    Add-Result -Name "Mother API HTTP" -Passed $false -Details $_.Exception.Message
}

# Test 3: Aggregation payload contains children.
$children = @()
if ($null -ne $motherJson -and $null -ne $motherJson.children) {
    $children = @($motherJson.children)
    $hasChildren = ($children.Count -gt 0)
    Add-Result -Name "Children list present" -Passed $hasChildren -Details "count=$($children.Count)"
} else {
    Add-Result -Name "Children list present" -Passed $false -Details "children missing in payload"
}

# Test 4: Every child is available=true.
if ($children.Count -gt 0) {
    $unavailable = @($children | Where-Object { -not $_.available })
    $allAvailable = ($unavailable.Count -eq 0)

    if ($allAvailable) {
        Add-Result -Name "Children availability" -Passed $true -Details "all children are available=true"
    } else {
        $names = ($unavailable | ForEach-Object { $_.name }) -join ", "
        Add-Result -Name "Children availability" -Passed $false -Details "unavailable=$names"
    }
}

# Test 5: Every child has lots and expeditions.
if ($children.Count -gt 0) {
    $missingData = @()

    foreach ($child in $children) {
        $lotCount = if ($null -ne $child.lots) { @($child.lots).Count } else { 0 }
        $expCount = if ($null -ne $child.expeditions) { @($child.expeditions).Count } else { 0 }

        if ($lotCount -le 0 -or $expCount -le 0) {
            $missingData += "$($child.name)(lots=$lotCount,expeditions=$expCount)"
        }
    }

    $dataOk = ($missingData.Count -eq 0)
    if ($dataOk) {
        Add-Result -Name "Children data completeness" -Passed $true -Details "all children have lots>0 and expeditions>0"
    } else {
        Add-Result -Name "Children data completeness" -Passed $false -Details (($missingData -join "; "))
    }
}

# Test 6: Direct child /health endpoints are reachable.
$childHealthFailures = @()
foreach ($url in $ChildHealthUrls) {
    try {
        $resp = Invoke-Http -Url $url
        if ($resp.StatusCode -ne 200) {
            $childHealthFailures += "$url(status=$($resp.StatusCode))"
        }
    } catch {
        $childHealthFailures += "$url(error=$($_.Exception.Message))"
    }
}

$healthOk = ($childHealthFailures.Count -eq 0)
if ($healthOk) {
    Add-Result -Name "Child direct health" -Passed $true -Details "all /health endpoints returned 200"
} else {
    Add-Result -Name "Child direct health" -Passed $false -Details (($childHealthFailures -join "; "))
}

Write-Host ""
Write-Host "Smoke test report" -ForegroundColor Cyan
$results | Format-Table -AutoSize

$failedCount = @($results | Where-Object { $_.Status -eq "KO" }).Count
Write-Host ""
if ($failedCount -eq 0) {
    Write-Host "Result: OK (all smoke tests passed)" -ForegroundColor Green
    exit 0
}

Write-Host "Result: KO ($failedCount failed test(s))" -ForegroundColor Red
exit 1
