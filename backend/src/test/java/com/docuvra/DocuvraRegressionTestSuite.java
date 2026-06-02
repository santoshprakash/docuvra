package com.docuvra;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        DocumentIntegrationTest.class,
        AnnotationRoleIntegrationTest.class,
        AssignmentNotificationIntegrationTest.class
})
public class DocuvraRegressionTestSuite {
}
