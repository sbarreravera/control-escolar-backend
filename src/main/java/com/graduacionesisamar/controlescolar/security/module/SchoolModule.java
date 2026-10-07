package com.graduacionesisamar.controlescolar.security.module;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a school-facing module that can be assigned to configurable users.
 *
 * <p>The user administration screen discovers these declarations dynamically.
 * New modules only need to declare this annotation on their controller (or on
 * the specific controller methods that belong to the module).</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SchoolModule {

    String key();

    String name();

    String description() default "";

    boolean defaultGranted() default false;

    int order() default 1000;
}
