param([string]$JdkPath = 'C:\Program Files\Android\Android Studio\jbr')
$ErrorActionPreference = 'Stop'
$testPath = Join-Path $PSScriptRoot 'work\tests'
New-Item -ItemType Directory -Force -Path $testPath | Out-Null
& "$JdkPath\bin\javac.exe" -encoding UTF-8 -d $testPath "$PSScriptRoot\app\src\main\java\local\countdown\probe\CountdownRules.java" "$PSScriptRoot\app\src\main\java\local\countdown\probe\RecurrenceDates.java" "$PSScriptRoot\app\src\main\java\local\countdown\probe\ImagePlacement.java" "$PSScriptRoot\tests\CountdownRulesTest.java" "$PSScriptRoot\tests\RecurrenceDatesTest.java" "$PSScriptRoot\tests\ImagePlacementTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
& "$JdkPath\bin\java.exe" -cp $testPath CountdownRulesTest
if ($LASTEXITCODE -ne 0) { throw 'Tests failed' }
& "$JdkPath\bin\java.exe" -cp $testPath RecurrenceDatesTest
if ($LASTEXITCODE -ne 0) { throw 'Recurrence date tests failed' }
& "$JdkPath\bin\java.exe" -cp $testPath ImagePlacementTest
if ($LASTEXITCODE -ne 0) { throw 'Photo crop tests failed' }
& "$JdkPath\bin\javac.exe" -encoding UTF-8 -cp $testPath -d $testPath "$PSScriptRoot\app\src\main\java\local\countdown\probe\AnniversaryRules.java" "$PSScriptRoot\tests\AnniversaryRulesTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Anniversary test compilation failed' }
& "$JdkPath\bin\java.exe" -cp $testPath AnniversaryRulesTest
if ($LASTEXITCODE -ne 0) { throw 'Anniversary tests failed' }
& "$JdkPath\bin\javac.exe" -encoding UTF-8 -cp $testPath -d $testPath "$PSScriptRoot\app\src\main\java\local\countdown\probe\SectionLayout.java" "$PSScriptRoot\tests\SectionLayoutTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Layout test compilation failed' }
& "$JdkPath\bin\java.exe" -cp $testPath SectionLayoutTest
if ($LASTEXITCODE -ne 0) { throw 'Layout tests failed' }
& "$JdkPath\bin\javac.exe" -encoding UTF-8 -cp $testPath -d $testPath "$PSScriptRoot\app\src\main\java\local\countdown\probe\HaloBackground.java" "$PSScriptRoot\tests\HaloBackgroundTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Halo test compilation failed' }
& "$JdkPath\bin\java.exe" -cp $testPath HaloBackgroundTest
if ($LASTEXITCODE -ne 0) { throw 'Halo palette tests failed' }
