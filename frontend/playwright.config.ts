import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  outputDir: 'test-results/playwright',
  fullyParallel: false,
  retries: process.env.CI ? 1 : 0,
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['junit', { outputFile: 'test-results/playwright/junit-results.xml' }]
  ],
  use: {
    baseURL: 'http://127.0.0.1:4200',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'retain-on-failure'
  },
  webServer: {
    command: 'npm start -- --host 127.0.0.1 --port 4200',
    url: 'http://127.0.0.1:4200',
    reuseExistingServer: true,
    timeout: 120_000
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    }
  ]
});
