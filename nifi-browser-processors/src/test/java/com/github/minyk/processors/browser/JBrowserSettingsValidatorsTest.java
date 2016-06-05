package com.github.minyk.processors.browser;


import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.apache.nifi.processor.util.StandardValidators;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class JBrowserSettingsValidatorsTest {

    @Test
    public void testPortRangeValidator() {
        Validator val = JBrowserSettingsValidators.PORT_RANGE_VALIDATOR;

        ValidationContext vc = mock(ValidationContext.class);
        ValidationResult vr = val.validate("foo", "50001-59999", vc);
        assertTrue(vr.isValid());

        vr = val.validate("foo", "50000-59999", vc);
        assertFalse(vr.isValid());

    }

    @Test
    public void testTimezoneValidator() {
        Validator val = JBrowserSettingsValidators.TIMEZONE_VALIDATOR;

        ValidationContext vc = mock(ValidationContext.class);
        ValidationResult vr = val.validate("foo", "Asia/Seoul", vc);
        assertTrue(vr.isValid());

        vr = val.validate("foo", "Asia/NotVailid", vc);
        assertFalse(vr.isValid());
    }
}
