#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORTS_DIR="$ROOT_DIR/reports"

rm -rf "$REPORTS_DIR"
mkdir -p "$REPORTS_DIR/backend" "$REPORTS_DIR/frontend" "$REPORTS_DIR/e2e/screenshots"

echo "Running backend tests and reports..."
(
  cd "$ROOT_DIR/backend"
  mvn verify
  mvn surefire-report:report-only
  mvn jacoco:report
  mvn allure:report
)

echo "Running frontend unit tests and reports..."
(
  cd "$ROOT_DIR/frontend"
  npm run test:ci -- --browsers=ChromeHeadless
)

echo "Running Playwright UI regression tests..."
(
  cd "$ROOT_DIR/frontend"
  npm run e2e
)

echo "Collecting reports..."
cp -f "$ROOT_DIR/backend/target/site/surefire-report.html" "$REPORTS_DIR/backend/" 2>/dev/null || true
cp -R "$ROOT_DIR/backend/target/site/jacoco" "$REPORTS_DIR/backend/jacoco" 2>/dev/null || true
cp -R "$ROOT_DIR/backend/target/allure-results" "$REPORTS_DIR/backend/allure-results" 2>/dev/null || true
cp -R "$ROOT_DIR/backend/target/allure-report" "$REPORTS_DIR/backend/allure-report" 2>/dev/null || true
cp -R "$ROOT_DIR/frontend/test-results/karma" "$REPORTS_DIR/frontend/karma" 2>/dev/null || true
cp -R "$ROOT_DIR/frontend/coverage" "$REPORTS_DIR/frontend/coverage" 2>/dev/null || true
cp -R "$ROOT_DIR/frontend/playwright-report" "$REPORTS_DIR/e2e/playwright-report" 2>/dev/null || true
cp -R "$ROOT_DIR/frontend/test-results/screenshots/." "$REPORTS_DIR/e2e/screenshots/" 2>/dev/null || true

echo
echo "Regression reports created:"
echo "- Backend Surefire: $REPORTS_DIR/backend/surefire-report.html"
echo "- Backend JaCoCo: $REPORTS_DIR/backend/jacoco/index.html"
echo "- Backend Allure: $REPORTS_DIR/backend/allure-report/index.html"
echo "- Frontend Karma JUnit: $REPORTS_DIR/frontend/karma/test-results.xml"
echo "- Frontend coverage: $REPORTS_DIR/frontend/coverage/index.html"
echo "- Playwright HTML: $REPORTS_DIR/e2e/playwright-report/index.html"
echo "- UI screenshots: $REPORTS_DIR/e2e/screenshots"
