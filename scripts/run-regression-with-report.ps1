$ErrorActionPreference = 'Stop'

$RootDir = Resolve-Path (Join-Path $PSScriptRoot '..')
$ReportsDir = Join-Path $RootDir 'reports'

if (Test-Path $ReportsDir) {
    Remove-Item -Recurse -Force $ReportsDir
}
New-Item -ItemType Directory -Force -Path `
    (Join-Path $ReportsDir 'backend'), `
    (Join-Path $ReportsDir 'frontend'), `
    (Join-Path $ReportsDir 'e2e\screenshots') | Out-Null

Write-Host 'Running backend tests and reports...'
Push-Location (Join-Path $RootDir 'backend')
mvn verify
mvn surefire-report:report-only
mvn jacoco:report
mvn allure:report
Pop-Location

Write-Host 'Running frontend unit tests and reports...'
Push-Location (Join-Path $RootDir 'frontend')
npm run test:ci -- --browsers=ChromeHeadless
Pop-Location

Write-Host 'Running Playwright UI regression tests...'
Push-Location (Join-Path $RootDir 'frontend')
npm run e2e
Pop-Location

Write-Host 'Collecting reports...'
Copy-Item -Force -ErrorAction SilentlyContinue (Join-Path $RootDir 'backend\target\site\surefire-report.html') (Join-Path $ReportsDir 'backend')
Copy-Item -Recurse -Force -ErrorAction SilentlyContinue (Join-Path $RootDir 'backend\target\site\jacoco') (Join-Path $ReportsDir 'backend\jacoco')
Copy-Item -Recurse -Force -ErrorAction SilentlyContinue (Join-Path $RootDir 'backend\target\allure-results') (Join-Path $ReportsDir 'backend\allure-results')
Copy-Item -Recurse -Force -ErrorAction SilentlyContinue (Join-Path $RootDir 'backend\target\allure-report') (Join-Path $ReportsDir 'backend\allure-report')
Copy-Item -Recurse -Force -ErrorAction SilentlyContinue (Join-Path $RootDir 'frontend\test-results\karma') (Join-Path $ReportsDir 'frontend\karma')
Copy-Item -Recurse -Force -ErrorAction SilentlyContinue (Join-Path $RootDir 'frontend\coverage') (Join-Path $ReportsDir 'frontend\coverage')
Copy-Item -Recurse -Force -ErrorAction SilentlyContinue (Join-Path $RootDir 'frontend\playwright-report') (Join-Path $ReportsDir 'e2e\playwright-report')
Copy-Item -Recurse -Force -ErrorAction SilentlyContinue (Join-Path $RootDir 'frontend\test-results\screenshots\*') (Join-Path $ReportsDir 'e2e\screenshots')

Write-Host ''
Write-Host 'Regression reports created:'
Write-Host "- Backend Surefire: $(Join-Path $ReportsDir 'backend\surefire-report.html')"
Write-Host "- Backend JaCoCo: $(Join-Path $ReportsDir 'backend\jacoco\index.html')"
Write-Host "- Backend Allure: $(Join-Path $ReportsDir 'backend\allure-report\index.html')"
Write-Host "- Frontend Karma JUnit: $(Join-Path $ReportsDir 'frontend\karma\test-results.xml')"
Write-Host "- Frontend coverage: $(Join-Path $ReportsDir 'frontend\coverage\index.html')"
Write-Host "- Playwright HTML: $(Join-Path $ReportsDir 'e2e\playwright-report\index.html')"
Write-Host "- UI screenshots: $(Join-Path $ReportsDir 'e2e\screenshots')"
