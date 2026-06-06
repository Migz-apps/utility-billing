param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$ReportPath = "reports/swagger-endpoint-verification.json"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$script:Results = New-Object System.Collections.Generic.List[object]
$script:Tested = @{}
$script:Context = [ordered]@{}

function Parse-Json {
    param([string]$Raw)

    if ([string]::IsNullOrWhiteSpace($Raw)) {
        return $null
    }

    $trimmed = $Raw.Trim()
    if (-not ($trimmed.StartsWith("{") -or $trimmed.StartsWith("["))) {
        return $null
    }

    try {
        return $trimmed | ConvertFrom-Json
    } catch {
        return $null
    }
}

function Build-Uri {
    param(
        [string]$Path,
        [hashtable]$Query
    )

    $uri = "$BaseUrl$Path"
    if ($Query -and $Query.Count -gt 0) {
        $pairs = foreach ($key in $Query.Keys) {
            "{0}={1}" -f [uri]::EscapeDataString([string]$key), [uri]::EscapeDataString([string]$Query[$key])
        }
        $uri = "${uri}?$(($pairs -join '&'))"
    }

    return $uri
}

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        [hashtable]$Headers,
        $Body,
        [hashtable]$Query
    )

    $uri = Build-Uri -Path $Path -Query $Query
    $params = @{
        Uri             = $uri
        Method          = $Method
        Headers         = $Headers
        UseBasicParsing = $true
    }

    if ($null -ne $Body) {
        $params.ContentType = "application/json"
        $params.Body = ($Body | ConvertTo-Json -Depth 20)
    }

    try {
        $response = Invoke-WebRequest @params
        return [pscustomobject]@{
            Status = [int]$response.StatusCode
            Json   = Parse-Json -Raw $response.Content
            Raw    = $response.Content
            Uri    = $uri
        }
    } catch {
        $status = 0
        $raw = $_.Exception.Message
        $responseObject = $_.Exception.Response

        if ($null -ne $responseObject) {
            $status = [int]$responseObject.StatusCode
            $stream = $responseObject.GetResponseStream()
            if ($null -ne $stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                $raw = $reader.ReadToEnd()
                $reader.Dispose()
            }
        }

        return [pscustomobject]@{
            Status = $status
            Json   = Parse-Json -Raw $raw
            Raw    = $raw
            Uri    = $uri
        }
    }
}

function Add-Result {
    param(
        [string]$Key,
        [int]$Status,
        [bool]$Passed,
        [string]$Note
    )

    $script:Results.Add([pscustomobject]@{
            endpoint = $Key
            status   = $Status
            passed   = $Passed
            note     = $Note
        })
    $script:Tested[$Key] = $true
}

function Get-Headers {
    param([string]$Token)

    $headers = @{}
    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $headers.Authorization = "Bearer $Token"
    }
    return $headers
}

function Assert-Endpoint {
    param(
        [string]$Key,
        [string]$Method,
        [string]$Path,
        [string]$Token,
        $Body,
        [hashtable]$Query,
        [int[]]$ExpectedStatus = @(200, 201),
        [scriptblock]$OnSuccess,
        [string]$Note = ""
    )

    $response = Invoke-Api -Method $Method -Path $Path -Headers (Get-Headers -Token $Token) -Body $Body -Query $Query
    $passed = $ExpectedStatus -contains $response.Status
    $detail = $Note

    if ($passed -and $OnSuccess) {
        try {
            $callbackNote = & $OnSuccess $response
            if (-not [string]::IsNullOrWhiteSpace($callbackNote)) {
                $detail = if ([string]::IsNullOrWhiteSpace($detail)) { $callbackNote } else { "$detail | $callbackNote" }
            }
        } catch {
            $passed = $false
            $detail = "Validation failed: $($_.Exception.Message)"
        }
    }

    if (-not $passed) {
        $failureNote = "Expected $($ExpectedStatus -join ', ') but got $($response.Status)."
        if ($response.Raw) {
            $failureNote = "$failureNote Body: $($response.Raw)"
        }
        $detail = if ([string]::IsNullOrWhiteSpace($detail)) { $failureNote } else { "$detail | $failureNote" }
    }

    Add-Result -Key $Key -Status $response.Status -Passed $passed -Note $detail

    if (-not $passed) {
        throw "Endpoint failed: $Key"
    }

    return $response
}

function Login {
    param(
        [string]$Email,
        [string]$Password
    )

    $response = Assert-Endpoint `
        -Key "POST /api/v1/auth/login" `
        -Method "POST" `
        -Path "/api/v1/auth/login" `
        -Body @{
            email    = $Email
            password = $Password
        } `
        -ExpectedStatus @(200) `
        -OnSuccess {
            param($resp)
            if (-not $resp.Json.data.token) {
                throw "Missing token in login response"
            }
            return "Logged in as $Email"
        }

    return [string]$response.Json.data.token
}

function New-PhoneNumber {
    param([string]$Seed)
    return "07$($Seed.Substring($Seed.Length - 8, 8))"
}

function New-NationalId {
    param([string]$Seed)
    return -join (1..16 | ForEach-Object { Get-Random -Minimum 0 -Maximum 10 })
}

function Ensure-Directory {
    param([string]$Path)

    $dir = Split-Path -Path $Path -Parent
    if (-not [string]::IsNullOrWhiteSpace($dir) -and -not (Test-Path -LiteralPath $dir)) {
        New-Item -ItemType Directory -Path $dir | Out-Null
    }
}

function Get-AppProperty {
    param(
        [string]$Path,
        [string]$Key
    )

    $line = Get-Content -Path $Path | Where-Object { $_ -match "^$([regex]::Escape($Key))=" } | Select-Object -First 1
    if ([string]::IsNullOrWhiteSpace($line)) {
        throw "Property '$Key' was not found in $Path"
    }

    return ($line -split "=", 2)[1].Trim()
}

function Get-PostgresSettings {
    $propertiesPath = Join-Path $PSScriptRoot "..\src\main\resources\application.properties"
    $jdbcUrl = Get-AppProperty -Path $propertiesPath -Key "spring.datasource.url"
    $username = Get-AppProperty -Path $propertiesPath -Key "spring.datasource.username"
    $password = Get-AppProperty -Path $propertiesPath -Key "spring.datasource.password"

    if ($jdbcUrl -notmatch "^jdbc:postgresql://(?<host>[^:/]+)(:(?<port>\d+))?/(?<database>[^?]+)$") {
        throw "Unsupported PostgreSQL JDBC URL format: $jdbcUrl"
    }

    $psqlPath = Get-ChildItem "C:\Program Files\PostgreSQL" -Recurse -Filter "psql.exe" -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending |
        Select-Object -First 1 -ExpandProperty FullName

    if (-not $psqlPath) {
        throw "psql.exe was not found under C:\Program Files\PostgreSQL"
    }

    return [pscustomobject]@{
        psqlPath = $psqlPath
        host     = $Matches.host
        port     = if ($Matches.port) { $Matches.port } else { "5432" }
        database = $Matches.database
        username = $username
        password = $password
    }
}

function Get-LatestOtpCode {
    param([string]$Email)

    $settings = Get-PostgresSettings
    $escapedEmail = $Email.Replace("'", "''")
    $sql = "select e.code from email_verification_otps e join users u on u.id = e.user_id where lower(u.email) = lower('$escapedEmail') order by e.id desc limit 1;"

    $previousPassword = $env:PGPASSWORD
    $env:PGPASSWORD = $settings.password
    try {
        $code = & $settings.psqlPath -h $settings.host -p $settings.port -U $settings.username -d $settings.database -t -A -c $sql
        $trimmed = [string]$code
        $trimmed = $trimmed.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed)) {
            throw "No OTP code found in database for $Email"
        }
        return $trimmed
    } finally {
        $env:PGPASSWORD = $previousPassword
    }
}

function Get-BillByReading {
    param(
        [string]$FinanceToken,
        [long]$ReadingId
    )

    $response = Assert-Endpoint `
        -Key "GET /api/v1/bills" `
        -Method "GET" `
        -Path "/api/v1/bills" `
        -Token $FinanceToken `
        -Query @{ page = 0; size = 100 } `
        -ExpectedStatus @(200)

    $bill = @($response.Json.data | Where-Object { [string]$_.meterReadingId -eq [string]$ReadingId })[0]
    if ($null -eq $bill) {
        throw "Bill not found for reading $ReadingId"
    }

    return $bill
}

function Ensure-SeededCustomerNotification {
    param(
        [string]$CustomerToken,
        [string]$OperatorToken,
        [string]$FinanceToken
    )

    $notifications = Assert-Endpoint `
        -Key "GET /api/v1/customers/me/notifications" `
        -Method "GET" `
        -Path "/api/v1/customers/me/notifications" `
        -Token $CustomerToken `
        -Query @{ page = 0; size = 20 } `
        -ExpectedStatus @(200)

    if (@($notifications.Json.data).Count -gt 0) {
        return [long]$notifications.Json.data[0].id
    }

    $meters = Assert-Endpoint `
        -Key "GET /api/v1/customers/me/meters" `
        -Method "GET" `
        -Path "/api/v1/customers/me/meters" `
        -Token $CustomerToken `
        -Query @{ page = 0; size = 20 } `
        -ExpectedStatus @(200)

    $meterId = [long]$meters.Json.data[0].id

    $reading = Assert-Endpoint `
        -Key "POST /api/v1/readings" `
        -Method "POST" `
        -Path "/api/v1/readings" `
        -Token $OperatorToken `
        -Body @{
            meterId         = $meterId
            currentReading  = 9999
            readingDate     = "2026-01-04"
        } `
        -ExpectedStatus @(201)

    $seedBill = Get-BillByReading -FinanceToken $FinanceToken -ReadingId ([long]$reading.Json.data.id)
    $script:Context.SeededBillId = [long]$seedBill.id

    $notificationsAgain = Invoke-Api -Method "GET" -Path "/api/v1/customers/me/notifications" -Headers (Get-Headers -Token $CustomerToken) -Body $null -Query @{ page = 0; size = 20 }
    if (@($notificationsAgain.Json.data).Count -eq 0) {
        throw "Could not create a customer notification for seeded customer"
    }

    return [long]$notificationsAgain.Json.data[0].id
}

$openApi = Invoke-RestMethod -Uri "$BaseUrl/v3/api-docs"
$expectedKeys = New-Object System.Collections.Generic.List[string]
foreach ($path in ($openApi.paths.PSObject.Properties.Name | Sort-Object)) {
    foreach ($method in ($openApi.paths.$path.PSObject.Properties.Name | Sort-Object)) {
        $expectedKeys.Add(("{0} {1}" -f $method.ToUpper(), $path))
    }
}

$stamp = Get-Date -Format "yyyyMMddHHmmssfff"
$script:Context.PublicPassword = "Customer123!"
$script:Context.StaffPassword = "Staff123!"

$script:Context.PublicEmail = "public.$stamp@example.com"
$script:Context.PublicPhone = New-PhoneNumber -Seed $stamp
$script:Context.PublicCustomerNationalId = New-NationalId -Seed ($stamp + "00")
$script:Context.MainCustomerNationalId = New-NationalId -Seed ($stamp + "11")
$script:Context.DeleteCustomerNationalId = New-NationalId -Seed ($stamp + "22")
$script:Context.MainCustomerEmail = "main.customer.$stamp@example.com"
$script:Context.DeleteCustomerEmail = "delete.customer.$stamp@example.com"
$script:Context.MainCustomerPhone = New-PhoneNumber -Seed ($stamp + "11")
$script:Context.DeleteCustomerPhone = New-PhoneNumber -Seed ($stamp + "22")
$script:Context.StaffEmail = "staff.$stamp@example.com"
$script:Context.StaffPhone = New-PhoneNumber -Seed ($stamp + "33")
$script:Context.NewMeterNumber = "POSTMAN-$($stamp.Substring($stamp.Length - 8, 8))"
$script:Context.NewTariffVersion = [int]($stamp.Substring($stamp.Length - 6, 6))
$script:Context.FutureDate = "2030-01-01"

try {
    Assert-Endpoint `
        -Key "POST /api/v1/auth/register" `
        -Method "POST" `
        -Path "/api/v1/auth/register" `
        -Body @{
            fullName    = "Public Customer $stamp"
            email       = $script:Context.PublicEmail
            phoneNumber = $script:Context.PublicPhone
            password    = $script:Context.PublicPassword
        } `
        -ExpectedStatus @(201) `
        -OnSuccess {
            param($resp)
            if (-not $resp.Json.data.verificationRequired) {
                throw "Registration did not require email verification"
            }
            return "Registered $($script:Context.PublicEmail) and OTP dispatch was requested"
        } | Out-Null

    Assert-Endpoint `
        -Key "POST /api/v1/auth/resend-verification-otp" `
        -Method "POST" `
        -Path "/api/v1/auth/resend-verification-otp" `
        -Body @{ email = $script:Context.PublicEmail } `
        -ExpectedStatus @(200) | Out-Null

    $script:Context.PublicOtpCode = Get-LatestOtpCode -Email $script:Context.PublicEmail

    Assert-Endpoint `
        -Key "POST /api/v1/auth/verify-email" `
        -Method "POST" `
        -Path "/api/v1/auth/verify-email" `
        -Body @{
            email   = $script:Context.PublicEmail
            otpCode = $script:Context.PublicOtpCode
        } `
        -ExpectedStatus @(200) | Out-Null

    $script:Context.AdminToken = Login -Email "admin@utility.rw" -Password "Admin123!"
    $script:Context.OperatorToken = Login -Email "operator@utility.rw" -Password "Operator123!"
    $script:Context.FinanceToken = Login -Email "finance@utility.rw" -Password "Finance123!"
    $script:Context.CustomerToken = Login -Email "customer@utility.rw" -Password "Customer123!"
    $script:Context.PublicToken = Login -Email $script:Context.PublicEmail -Password $script:Context.PublicPassword

    Assert-Endpoint `
        -Key "GET /api/v1/auth/me" `
        -Method "GET" `
        -Path "/api/v1/auth/me" `
        -Token $script:Context.PublicToken `
        -ExpectedStatus @(200) | Out-Null

    $publicProfileResponse = Assert-Endpoint `
        -Key "POST /api/v1/customers/me/profile" `
        -Method "POST" `
        -Path "/api/v1/customers/me/profile" `
        -Token $script:Context.PublicToken `
        -Body @{
            nationalId = $script:Context.PublicCustomerNationalId
            address    = "Kigali Public Address $stamp"
        } `
        -ExpectedStatus @(200)
    $script:Context.PublicCustomerId = [long]$publicProfileResponse.Json.data.id

    Assert-Endpoint `
        -Key "GET /api/v1/customers/me" `
        -Method "GET" `
        -Path "/api/v1/customers/me" `
        -Token $script:Context.PublicToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "GET /api/v1/admin/users" `
        -Method "GET" `
        -Path "/api/v1/admin/users" `
        -Token $script:Context.AdminToken `
        -ExpectedStatus @(200) | Out-Null

    $staffResponse = Assert-Endpoint `
        -Key "POST /api/v1/admin/users/staff" `
        -Method "POST" `
        -Path "/api/v1/admin/users/staff" `
        -Token $script:Context.AdminToken `
        -Body @{
            fullName    = "Staff User $stamp"
            email       = $script:Context.StaffEmail
            phoneNumber = $script:Context.StaffPhone
            password    = $script:Context.StaffPassword
            role        = "ROLE_OPERATOR"
        } `
        -ExpectedStatus @(201)
    $script:Context.StaffUserId = [long]$staffResponse.Json.data.id

    Assert-Endpoint `
        -Key "PATCH /api/v1/admin/users/{id}/deactivate" `
        -Method "PATCH" `
        -Path "/api/v1/admin/users/$($script:Context.StaffUserId)/deactivate" `
        -Token $script:Context.AdminToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "PATCH /api/v1/admin/users/{id}/activate" `
        -Method "PATCH" `
        -Path "/api/v1/admin/users/$($script:Context.StaffUserId)/activate" `
        -Token $script:Context.AdminToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "GET /api/v1/customers" `
        -Method "GET" `
        -Path "/api/v1/customers" `
        -Token $script:Context.AdminToken `
        -Query @{ page = 0; size = 20 } `
        -ExpectedStatus @(200) | Out-Null

    $mainCustomerResponse = Assert-Endpoint `
        -Key "POST /api/v1/customers" `
        -Method "POST" `
        -Path "/api/v1/customers" `
        -Token $script:Context.AdminToken `
        -Body @{
            fullName    = "History Customer $stamp"
            nationalId  = $script:Context.MainCustomerNationalId
            email       = $script:Context.MainCustomerEmail
            phoneNumber = $script:Context.MainCustomerPhone
            address     = "Kigali Main Address $stamp"
            userId      = $null
        } `
        -ExpectedStatus @(201)
    $script:Context.MainCustomerId = [long]$mainCustomerResponse.Json.data.id

    $deleteCustomerResponse = Assert-Endpoint `
        -Method "POST" `
        -Path "/api/v1/customers" `
        -Token $script:Context.AdminToken `
        -Body @{
            fullName    = "Delete Customer $stamp"
            nationalId  = $script:Context.DeleteCustomerNationalId
            email       = $script:Context.DeleteCustomerEmail
            phoneNumber = $script:Context.DeleteCustomerPhone
            address     = "Kigali Delete Address $stamp"
            userId      = $null
        } `
        -ExpectedStatus @(201) `
        -Key "POST /api/v1/customers"
    $script:Context.DeleteCustomerId = [long]$deleteCustomerResponse.Json.data.id

    Assert-Endpoint `
        -Key "GET /api/v1/customers/{id}" `
        -Method "GET" `
        -Path "/api/v1/customers/$($script:Context.MainCustomerId)" `
        -Token $script:Context.AdminToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "PUT /api/v1/customers/{id}" `
        -Method "PUT" `
        -Path "/api/v1/customers/$($script:Context.MainCustomerId)" `
        -Token $script:Context.AdminToken `
        -Body @{
            fullName    = "History Customer Updated $stamp"
            nationalId  = $script:Context.MainCustomerNationalId
            email       = $script:Context.MainCustomerEmail
            phoneNumber = $script:Context.MainCustomerPhone
            address     = "Kigali Main Address Updated $stamp"
            userId      = $null
        } `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "PATCH /api/v1/customers/{id}/deactivate" `
        -Method "PATCH" `
        -Path "/api/v1/customers/$($script:Context.MainCustomerId)/deactivate" `
        -Token $script:Context.AdminToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "PATCH /api/v1/customers/{id}/activate" `
        -Method "PATCH" `
        -Path "/api/v1/customers/$($script:Context.MainCustomerId)/activate" `
        -Token $script:Context.AdminToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "DELETE /api/v1/customers/{id}" `
        -Method "DELETE" `
        -Path "/api/v1/customers/$($script:Context.DeleteCustomerId)" `
        -Token $script:Context.AdminToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Method "PATCH" `
        -Path "/api/v1/customers/$($script:Context.PublicCustomerId)/activate" `
        -Token $script:Context.AdminToken `
        -ExpectedStatus @(200) `
        -Key "PATCH /api/v1/customers/{id}/activate" | Out-Null

    $meterResponse = Assert-Endpoint `
        -Key "POST /api/v1/meters" `
        -Method "POST" `
        -Path "/api/v1/meters" `
        -Token $script:Context.AdminToken `
        -Body @{
            meterNumber      = $script:Context.NewMeterNumber
            meterType        = "WATER"
            installationDate = "2026-01-01"
            customerId       = $script:Context.PublicCustomerId
        } `
        -ExpectedStatus @(201)
    $script:Context.MeterId = [long]$meterResponse.Json.data.id

    Assert-Endpoint `
        -Key "GET /api/v1/meters" `
        -Method "GET" `
        -Path "/api/v1/meters" `
        -Token $script:Context.AdminToken `
        -Query @{ page = 0; size = 20 } `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "GET /api/v1/meters/active" `
        -Method "GET" `
        -Path "/api/v1/meters/active" `
        -Token $script:Context.OperatorToken `
        -Query @{ page = 0; size = 20 } `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "GET /api/v1/meters/{id}" `
        -Method "GET" `
        -Path "/api/v1/meters/$($script:Context.MeterId)" `
        -Token $script:Context.OperatorToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "PUT /api/v1/meters/{id}" `
        -Method "PUT" `
        -Path "/api/v1/meters/$($script:Context.MeterId)" `
        -Token $script:Context.AdminToken `
        -Body @{
            meterNumber      = "$($script:Context.NewMeterNumber)-UPD"
            meterType        = "WATER"
            installationDate = "2026-01-01"
            customerId       = $script:Context.PublicCustomerId
        } `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "PATCH /api/v1/meters/{id}/deactivate" `
        -Method "PATCH" `
        -Path "/api/v1/meters/$($script:Context.MeterId)/deactivate" `
        -Token $script:Context.AdminToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "PATCH /api/v1/meters/{id}/activate" `
        -Method "PATCH" `
        -Path "/api/v1/meters/$($script:Context.MeterId)/activate" `
        -Token $script:Context.AdminToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "GET /api/v1/customers/{customerId}/meters" `
        -Method "GET" `
        -Path "/api/v1/customers/$($script:Context.PublicCustomerId)/meters" `
        -Token $script:Context.AdminToken `
        -Query @{ page = 0; size = 20 } `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "GET /api/v1/readings" `
        -Method "GET" `
        -Path "/api/v1/readings" `
        -Token $script:Context.OperatorToken `
        -Query @{ page = 0; size = 20 } `
        -ExpectedStatus @(200) | Out-Null

    $readingOne = Assert-Endpoint `
        -Key "POST /api/v1/readings" `
        -Method "POST" `
        -Path "/api/v1/readings" `
        -Token $script:Context.OperatorToken `
        -Body @{
            meterId        = $script:Context.MeterId
            currentReading = 100
            readingDate    = "2026-02-05"
        } `
        -ExpectedStatus @(201)
    $script:Context.ReadingOneId = [long]$readingOne.Json.data.id

    Assert-Endpoint `
        -Key "GET /api/v1/readings/{id}" `
        -Method "GET" `
        -Path "/api/v1/readings/$($script:Context.ReadingOneId)" `
        -Token $script:Context.OperatorToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "GET /api/v1/readings/meter/{meterId}" `
        -Method "GET" `
        -Path "/api/v1/readings/meter/$($script:Context.MeterId)" `
        -Token $script:Context.OperatorToken `
        -Query @{ page = 0; size = 20 } `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "GET /api/v1/readings/monthly" `
        -Method "GET" `
        -Path "/api/v1/readings/monthly" `
        -Token $script:Context.OperatorToken `
        -Query @{ month = 2; year = 2026; page = 0; size = 20 } `
        -ExpectedStatus @(200) | Out-Null

    $billOne = Get-BillByReading -FinanceToken $script:Context.FinanceToken -ReadingId $script:Context.ReadingOneId
    $script:Context.BillOneId = [long]$billOne.id
    $script:Context.BillOneReference = [string]$billOne.billReference

    Assert-Endpoint `
        -Key "POST /api/v1/bills/generate/{readingId}" `
        -Method "POST" `
        -Path "/api/v1/bills/generate/$($script:Context.ReadingOneId)" `
        -Token $script:Context.FinanceToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "POST /api/v1/bills/generate-monthly" `
        -Method "POST" `
        -Path "/api/v1/bills/generate-monthly" `
        -Token $script:Context.FinanceToken `
        -Query @{ month = 2; year = 2026 } `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "GET /api/v1/bills/{id}" `
        -Method "GET" `
        -Path "/api/v1/bills/$($script:Context.BillOneId)" `
        -Token $script:Context.FinanceToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "GET /api/v1/bills/reference/{billReference}" `
        -Method "GET" `
        -Path "/api/v1/bills/reference/$($script:Context.BillOneReference)" `
        -Token $script:Context.FinanceToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "GET /api/v1/bills/customer/{customerId}" `
        -Method "GET" `
        -Path "/api/v1/bills/customer/$($script:Context.PublicCustomerId)" `
        -Token $script:Context.FinanceToken `
        -Query @{ page = 0; size = 20 } `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "PATCH /api/v1/bills/{id}/cancel" `
        -Method "PATCH" `
        -Path "/api/v1/bills/$($script:Context.BillOneId)/cancel" `
        -Token $script:Context.FinanceToken `
        -ExpectedStatus @(200) | Out-Null

    $readingTwo = Assert-Endpoint `
        -Method "POST" `
        -Path "/api/v1/readings" `
        -Token $script:Context.OperatorToken `
        -Body @{
            meterId        = $script:Context.MeterId
            currentReading = 200
            readingDate    = "2026-03-05"
        } `
        -ExpectedStatus @(201) `
        -Key "POST /api/v1/readings"
    $script:Context.ReadingTwoId = [long]$readingTwo.Json.data.id

    $billTwo = Get-BillByReading -FinanceToken $script:Context.FinanceToken -ReadingId $script:Context.ReadingTwoId
    $script:Context.BillTwoId = [long]$billTwo.id
    $script:Context.BillTwoReference = [string]$billTwo.billReference

    Assert-Endpoint `
        -Key "PATCH /api/v1/bills/{id}/approve" `
        -Method "PATCH" `
        -Path "/api/v1/bills/$($script:Context.BillTwoId)/approve" `
        -Token $script:Context.FinanceToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "GET /api/v1/bills/me" `
        -Method "GET" `
        -Path "/api/v1/bills/me" `
        -Token $script:Context.PublicToken `
        -Query @{ page = 0; size = 20 } `
        -ExpectedStatus @(200) | Out-Null

    $paymentResponse = Assert-Endpoint `
        -Key "POST /api/v1/payments" `
        -Method "POST" `
        -Path "/api/v1/payments" `
        -Token $script:Context.FinanceToken `
        -Body @{
            billReference = $script:Context.BillTwoReference
            amountPaid    = 50
            paymentMethod = "CASH"
            paymentDate   = "2026-03-06"
        } `
        -ExpectedStatus @(201)
    $script:Context.PaymentId = [long]$paymentResponse.Json.data.id

    Assert-Endpoint `
        -Key "GET /api/v1/payments" `
        -Method "GET" `
        -Path "/api/v1/payments" `
        -Token $script:Context.FinanceToken `
        -Query @{ page = 0; size = 20 } `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "GET /api/v1/payments/{id}" `
        -Method "GET" `
        -Path "/api/v1/payments/$($script:Context.PaymentId)" `
        -Token $script:Context.FinanceToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "GET /api/v1/payments/bill/{billId}" `
        -Method "GET" `
        -Path "/api/v1/payments/bill/$($script:Context.BillTwoId)" `
        -Token $script:Context.FinanceToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "GET /api/v1/payments/customer/{customerId}" `
        -Method "GET" `
        -Path "/api/v1/payments/customer/$($script:Context.PublicCustomerId)" `
        -Token $script:Context.FinanceToken `
        -Query @{ page = 0; size = 20 } `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "GET /api/v1/payments/me" `
        -Method "GET" `
        -Path "/api/v1/payments/me" `
        -Token $script:Context.PublicToken `
        -Query @{ page = 0; size = 20 } `
        -ExpectedStatus @(200) | Out-Null

    $readingThree = Assert-Endpoint `
        -Method "POST" `
        -Path "/api/v1/readings" `
        -Token $script:Context.OperatorToken `
        -Body @{
            meterId        = $script:Context.MeterId
            currentReading = 300
            readingDate    = "2026-04-05"
        } `
        -ExpectedStatus @(201) `
        -Key "POST /api/v1/readings"
    $script:Context.ReadingThreeId = [long]$readingThree.Json.data.id

    $billThree = Get-BillByReading -FinanceToken $script:Context.FinanceToken -ReadingId $script:Context.ReadingThreeId
    $script:Context.BillThreeId = [long]$billThree.id

    Assert-Endpoint `
        -Method "PATCH" `
        -Path "/api/v1/bills/$($script:Context.BillThreeId)/approve" `
        -Token $script:Context.FinanceToken `
        -ExpectedStatus @(200) `
        -Key "PATCH /api/v1/bills/{id}/approve" | Out-Null

    Assert-Endpoint `
        -Key "POST /api/v1/bills/process-overdue" `
        -Method "POST" `
        -Path "/api/v1/bills/process-overdue" `
        -Token $script:Context.FinanceToken `
        -ExpectedStatus @(200) | Out-Null

    $notificationsResponse = Assert-Endpoint `
        -Key "GET /api/v1/notifications" `
        -Method "GET" `
        -Path "/api/v1/notifications" `
        -Token $script:Context.FinanceToken `
        -Query @{ page = 0; size = 50 } `
        -ExpectedStatus @(200)

    $mainCustomerNotification = @($notificationsResponse.Json.data | Where-Object { [string]$_.customerId -eq [string]$script:Context.PublicCustomerId })[0]
    if ($null -eq $mainCustomerNotification) {
        throw "No notification found for main customer"
    }
    $script:Context.NotificationId = [long]$mainCustomerNotification.id

    Assert-Endpoint `
        -Key "GET /api/v1/notifications/customer/{customerId}" `
        -Method "GET" `
        -Path "/api/v1/notifications/customer/$($script:Context.PublicCustomerId)" `
        -Token $script:Context.FinanceToken `
        -Query @{ page = 0; size = 50 } `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "PATCH /api/v1/notifications/{id}/read" `
        -Method "PATCH" `
        -Path "/api/v1/notifications/$($script:Context.NotificationId)/read" `
        -Token $script:Context.FinanceToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "GET /api/v1/notifications/me" `
        -Method "GET" `
        -Path "/api/v1/notifications/me" `
        -Token $script:Context.PublicToken `
        -Query @{ page = 0; size = 20 } `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Method "GET" `
        -Path "/api/v1/customers/me/meters" `
        -Token $script:Context.PublicToken `
        -Query @{ page = 0; size = 20 } `
        -ExpectedStatus @(200) `
        -Key "GET /api/v1/customers/me/meters" | Out-Null

    Assert-Endpoint `
        -Method "GET" `
        -Path "/api/v1/customers/me/bills" `
        -Token $script:Context.PublicToken `
        -Query @{ page = 0; size = 20 } `
        -ExpectedStatus @(200) `
        -Key "GET /api/v1/customers/me/bills" | Out-Null

    Assert-Endpoint `
        -Method "GET" `
        -Path "/api/v1/customers/me/payments" `
        -Token $script:Context.PublicToken `
        -Query @{ page = 0; size = 20 } `
        -ExpectedStatus @(200) `
        -Key "GET /api/v1/customers/me/payments" | Out-Null

    Assert-Endpoint `
        -Method "GET" `
        -Path "/api/v1/customers/me/notifications" `
        -Token $script:Context.PublicToken `
        -Query @{ page = 0; size = 20 } `
        -ExpectedStatus @(200) `
        -Key "GET /api/v1/customers/me/notifications" | Out-Null

    Assert-Endpoint `
        -Key "PATCH /api/v1/customers/me/notifications/{id}/read" `
        -Method "PATCH" `
        -Path "/api/v1/customers/me/notifications/$($script:Context.NotificationId)/read" `
        -Token $script:Context.PublicToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "POST /api/v1/auth/logout" `
        -Method "POST" `
        -Path "/api/v1/auth/logout" `
        -Token $script:Context.PublicToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "GET /api/v1/tariffs" `
        -Method "GET" `
        -Path "/api/v1/tariffs" `
        -Token $script:Context.AdminToken `
        -ExpectedStatus @(200) | Out-Null

    $tariffResponse = Assert-Endpoint `
        -Key "POST /api/v1/tariffs" `
        -Method "POST" `
        -Path "/api/v1/tariffs" `
        -Token $script:Context.AdminToken `
        -Body @{
            name         = "Tiered Tariff $stamp"
            meterType    = "WATER"
            tariffType   = "TIERED"
            ratePerUnit  = 1
            version      = $script:Context.NewTariffVersion
            effectiveFrom = $script:Context.FutureDate
            effectiveTo  = $null
            active       = $true
        } `
        -ExpectedStatus @(201)
    $script:Context.TariffId = [long]$tariffResponse.Json.data.id

    Assert-Endpoint `
        -Key "GET /api/v1/tariffs/{id}" `
        -Method "GET" `
        -Path "/api/v1/tariffs/$($script:Context.TariffId)" `
        -Token $script:Context.AdminToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "POST /api/v1/tariffs/{id}/tiers" `
        -Method "POST" `
        -Path "/api/v1/tariffs/$($script:Context.TariffId)/tiers" `
        -Token $script:Context.AdminToken `
        -Body @{
            minUnits    = 0
            maxUnits    = 100
            ratePerUnit = 50
        } `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "PATCH /api/v1/tariffs/{id}/deactivate" `
        -Method "PATCH" `
        -Path "/api/v1/tariffs/$($script:Context.TariffId)/deactivate" `
        -Token $script:Context.AdminToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "GET /api/v1/fixed-charges" `
        -Method "GET" `
        -Path "/api/v1/fixed-charges" `
        -Token $script:Context.AdminToken `
        -ExpectedStatus @(200) | Out-Null

    $fixedChargeResponse = Assert-Endpoint `
        -Key "POST /api/v1/fixed-charges" `
        -Method "POST" `
        -Path "/api/v1/fixed-charges" `
        -Token $script:Context.AdminToken `
        -Body @{
            meterType    = "WATER"
            amount       = 500
            version      = $script:Context.NewTariffVersion
            effectiveFrom = $script:Context.FutureDate
            effectiveTo  = $null
            active       = $true
        } `
        -ExpectedStatus @(201)
    $script:Context.FixedChargeId = [long]$fixedChargeResponse.Json.data.id

    Assert-Endpoint `
        -Key "GET /api/v1/fixed-charges/{id}" `
        -Method "GET" `
        -Path "/api/v1/fixed-charges/$($script:Context.FixedChargeId)" `
        -Token $script:Context.AdminToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "PATCH /api/v1/fixed-charges/{id}/deactivate" `
        -Method "PATCH" `
        -Path "/api/v1/fixed-charges/$($script:Context.FixedChargeId)/deactivate" `
        -Token $script:Context.AdminToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "GET /api/v1/taxes" `
        -Method "GET" `
        -Path "/api/v1/taxes" `
        -Token $script:Context.AdminToken `
        -ExpectedStatus @(200) | Out-Null

    $taxResponse = Assert-Endpoint `
        -Key "POST /api/v1/taxes" `
        -Method "POST" `
        -Path "/api/v1/taxes" `
        -Token $script:Context.AdminToken `
        -Body @{
            name         = "Tax $stamp"
            percentage   = 17
            active       = $true
            effectiveFrom = $script:Context.FutureDate
            effectiveTo  = $null
        } `
        -ExpectedStatus @(201)
    $script:Context.TaxId = [long]$taxResponse.Json.data.id

    Assert-Endpoint `
        -Key "GET /api/v1/taxes/{id}" `
        -Method "GET" `
        -Path "/api/v1/taxes/$($script:Context.TaxId)" `
        -Token $script:Context.AdminToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "PATCH /api/v1/taxes/{id}/deactivate" `
        -Method "PATCH" `
        -Path "/api/v1/taxes/$($script:Context.TaxId)/deactivate" `
        -Token $script:Context.AdminToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "GET /api/v1/penalties" `
        -Method "GET" `
        -Path "/api/v1/penalties" `
        -Token $script:Context.AdminToken `
        -ExpectedStatus @(200) | Out-Null

    $penaltyResponse = Assert-Endpoint `
        -Key "POST /api/v1/penalties" `
        -Method "POST" `
        -Path "/api/v1/penalties" `
        -Token $script:Context.AdminToken `
        -Body @{
            name               = "Penalty $stamp"
            penaltyType        = "PERCENTAGE"
            amountOrPercentage = 5
            gracePeriodDays    = 3
            active             = $true
            effectiveFrom      = $script:Context.FutureDate
            effectiveTo        = $null
        } `
        -ExpectedStatus @(201)
    $script:Context.PenaltyId = [long]$penaltyResponse.Json.data.id

    Assert-Endpoint `
        -Key "GET /api/v1/penalties/{id}" `
        -Method "GET" `
        -Path "/api/v1/penalties/$($script:Context.PenaltyId)" `
        -Token $script:Context.AdminToken `
        -ExpectedStatus @(200) | Out-Null

    Assert-Endpoint `
        -Key "PATCH /api/v1/penalties/{id}/deactivate" `
        -Method "PATCH" `
        -Path "/api/v1/penalties/$($script:Context.PenaltyId)/deactivate" `
        -Token $script:Context.AdminToken `
        -ExpectedStatus @(200) | Out-Null

    foreach ($expectedKey in $expectedKeys) {
        if (-not $script:Tested.ContainsKey($expectedKey)) {
            Add-Result -Key $expectedKey -Status 0 -Passed $false -Note "Endpoint present in Swagger but not exercised by the verifier"
        }
    }
} catch {
    Write-Error $_
} finally {
    Ensure-Directory -Path $ReportPath

    $summary = [pscustomobject]@{
        baseUrl             = $BaseUrl
        executedAt          = (Get-Date).ToString("s")
        expectedEndpointCount = $expectedKeys.Count
        testedEndpointCount = $script:Results.Count
        passedCount         = @($script:Results | Where-Object { $_.passed }).Count
        failedCount         = @($script:Results | Where-Object { -not $_.passed }).Count
        results             = $script:Results
    }

    $summary | ConvertTo-Json -Depth 10 | Set-Content -Path $ReportPath
    $summary
}
