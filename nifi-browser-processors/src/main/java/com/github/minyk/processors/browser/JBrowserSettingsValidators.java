package com.github.minyk.processors.browser;

import com.machinepublishers.jbrowserdriver.Timezone;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;

import java.util.HashSet;
import java.util.Set;


public class JBrowserSettingsValidators {
    public static final Validator TIMEZONE_VALIDATOR = new Validator() {
        @Override
        public ValidationResult validate(final String subject, final String input, final ValidationContext context) {
            if (context.isExpressionLanguageSupported(subject) && context.isExpressionLanguagePresent(input)) {
                return new ValidationResult.Builder().subject(subject).input(input).explanation("Expression Language Present").valid(false).build();
            }

            try {
                String name = Timezone.byName(input).name();
                return new ValidationResult.Builder().subject(subject).input(input).explanation("Valid Timezone").valid(true).build();
            } catch (final Exception e) {
                return new ValidationResult.Builder().subject(subject).input(input).explanation("Not a valid Timezone").valid(false).build();
            }
        }
    };

    public static final Set<String> getAllTimezone() {
        Set<String> results = new HashSet<>();
        for(Timezone tz : Timezone.ALL_ZONES) {
            results.add(tz.name());
        }
        return results;
    }
}
