param(
    [string]$Servidor = "localhost,1433"
)

$ErrorActionPreference = "Stop"
$login = "Blexsyy"
$bases = @("VeterinariaDB", "VeterinariaMiniDB")
$securePassword = Read-Host "Contraseña nueva para el login de aplicación $login" -AsSecureString
$pointer = [IntPtr]::Zero
$plainPassword = $null
$connection = $null

try {
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    if ([string]::IsNullOrWhiteSpace($plainPassword) -or $plainPassword.Length -lt 14) {
        throw "La contraseña debe tener al menos 14 caracteres."
    }

    # La conexión integrada se usa solo para aprovisionar el servidor local.
    # Luego la aplicación usa el login SQL dedicado con permisos limitados.
    $builder = [System.Data.SqlClient.SqlConnectionStringBuilder]::new()
    $builder.DataSource = $Servidor
    $builder.InitialCatalog = "master"
    $builder.IntegratedSecurity = $true
    $builder.Encrypt = $true
    $builder.TrustServerCertificate = $true
    $builder.ConnectTimeout = 10
    $connection = [System.Data.SqlClient.SqlConnection]::new($builder.ConnectionString)
    $connection.Open()

    $escapedPassword = $plainPassword.Replace("'", "''")
    $loginSql = "IF SUSER_ID(N'$login') IS NOT NULL THROW 51000, 'El login ya existe; no se modificó su contraseña.', 1; EXEC(N'CREATE LOGIN [$login] WITH PASSWORD=N''$escapedPassword'', CHECK_POLICY=ON, CHECK_EXPIRATION=ON')"
    $command = $connection.CreateCommand()
    $command.CommandText = $loginSql
    [void]$command.ExecuteNonQuery()

    foreach ($database in $bases) {
        $createDatabase = "IF DB_ID(N'$database') IS NULL EXEC(N'CREATE DATABASE [$database]')"
        $command.CommandText = $createDatabase
        [void]$command.ExecuteNonQuery()

        $command.CommandText = @"
USE [$database];
IF USER_ID(N'$login') IS NULL CREATE USER [$login] FOR LOGIN [$login];
IF NOT EXISTS (SELECT 1 FROM sys.database_role_members drm JOIN sys.database_principals r ON r.principal_id=drm.role_principal_id JOIN sys.database_principals u ON u.principal_id=drm.member_principal_id WHERE r.name=N'db_datareader' AND u.name=N'$login') ALTER ROLE db_datareader ADD MEMBER [$login];
IF NOT EXISTS (SELECT 1 FROM sys.database_role_members drm JOIN sys.database_principals r ON r.principal_id=drm.role_principal_id JOIN sys.database_principals u ON u.principal_id=drm.member_principal_id WHERE r.name=N'db_datawriter' AND u.name=N'$login') ALTER ROLE db_datawriter ADD MEMBER [$login];
IF NOT EXISTS (SELECT 1 FROM sys.database_role_members drm JOIN sys.database_principals r ON r.principal_id=drm.role_principal_id JOIN sys.database_principals u ON u.principal_id=drm.member_principal_id WHERE r.name=N'db_ddladmin' AND u.name=N'$login') ALTER ROLE db_ddladmin ADD MEMBER [$login];
"@
        [void]$command.ExecuteNonQuery()
    }

    Write-Output "Bases de datos y login SQL de aplicación preparados. Configura DB_USERNAME y DB_PASSWORD en el entorno local."
}
finally {
    if ($connection) { $connection.Dispose() }
    if ($pointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
    $plainPassword = $null
    $escapedPassword = $null
    $securePassword.Dispose()
}
