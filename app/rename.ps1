$targetDir = "d:\dev\apps\backups\PurePdf with tools 1st time\app"
$oldPackage = "com.saverio.pdfviewer"
$newPackage = "com.outlandishomar.purepdf"

Write-Host "Updating files..."
Get-ChildItem -Path $targetDir -Recurse -File | Where-Object { 
    $_.Extension -match "\.(kt|xml|kts|pro|json)$"
} | ForEach-Object {
    try {
        $content = Get-Content -LiteralPath $_.FullName -Raw -Encoding UTF8
        if ($null -ne $content -and $content.Contains($oldPackage)) {
            $newContent = $content.Replace($oldPackage, $newPackage)
            $utf8NoBom = New-Object System.Text.UTF8Encoding $False
            [System.IO.File]::WriteAllText($_.FullName, $newContent, $utf8NoBom)
            Write-Host "Updated $($_.FullName)"
        }
    } catch {
        Write-Host "Error processing $($_.FullName): $_"
    }
}

Write-Host "Moving directories..."
$srcDirs = @(
    "$targetDir\src\main\java",
    "$targetDir\src\androidTest\java",
    "$targetDir\src\test\java"
)

foreach ($dir in $srcDirs) {
    $oldDir = "$dir\com\saverio\pdfviewer"
    if (Test-Path $oldDir) {
        $newDir = "$dir\com\outlandishomar\purepdf"
        New-Item -ItemType Directory -Force -Path $newDir | Out-Null
        
        Copy-Item -Path "$oldDir\*" -Destination $newDir -Recurse -Force
        Remove-Item -Path $oldDir -Recurse -Force
        
        if ((Get-ChildItem -Path "$dir\com\saverio").Count -eq 0) {
            Remove-Item -Path "$dir\com\saverio" -Force
        }
        Write-Host "Moved $oldDir to $newDir"
    }
}
Write-Host "Done"
