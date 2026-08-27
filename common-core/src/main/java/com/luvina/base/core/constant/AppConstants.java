package com.luvina.base.core.constant;

/**
 * Constants shared by every module.
 */
public final class AppConstants {

    /** Message thrown when someone tries to instantiate a utility class. */
    public static final String UTILITY_CLASS_ERROR = "Utility class, must not be instantiated";

    private AppConstants() {
        throw new UnsupportedOperationException(UTILITY_CLASS_ERROR);
    }
}
