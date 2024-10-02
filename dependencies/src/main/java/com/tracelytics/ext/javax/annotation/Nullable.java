package com.tracelytics.ext.javax.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.tracelytics.ext.javax.annotation.meta.TypeQualifierNickname;
import com.tracelytics.ext.javax.annotation.meta.When;

@Documented
@TypeQualifierNickname
@Nonnull(when = When.UNKNOWN)
@Retention(RetentionPolicy.RUNTIME)
public @interface Nullable {

}
