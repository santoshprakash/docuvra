module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-junit-reporter'),
      require('karma-coverage')
    ],
    client: {
      jasmine: {},
      clearContext: false
    },
    jasmineHtmlReporter: {
      suppressAll: true
    },
    reporters: ['progress', 'kjhtml', 'junit', 'coverage'],
    junitReporter: {
      outputDir: 'test-results/karma',
      outputFile: 'test-results.xml',
      useBrowserName: false
    },
    coverageReporter: {
      dir: require('path').join(__dirname, 'coverage'),
      subdir: '.',
      reporters: [
        { type: 'html' },
        { type: 'lcovonly' },
        { type: 'text-summary' }
      ]
    },
    browsers: ['ChromeHeadless'],
    restartOnFileChange: true
  });
};
