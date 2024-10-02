package com.tracelytics.ext.javax.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.tracelytics.ext.javax.annotation.meta.TypeQualifier;
import com.tracelytics.ext.javax.annotation.meta.TypeQualifierValidator;
import com.tracelytics.ext.javax.annotation.meta.When;

@Documented
@TypeQualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface Nonnull {
    When when() default When.ALWAYS;

    static class Checker implements TypeQualifierValidator<Nonnull> {

        public When forConstantValue(Nonnull qualifierqualifierArgument,
                Object value) {
            if (value == null)
                return When.NEVER;
            return When.ALWAYS;
        }
    }
}
