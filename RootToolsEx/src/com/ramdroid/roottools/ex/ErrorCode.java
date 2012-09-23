package com.ramdroid.roottools.ex;

import java.util.List;

/**
 * Global list of all error codes used by the RootToolsEx classes.
 */
public class ErrorCode {

    public static final int NONE                    = 0;
    public static final int ACCESS_OUTPUTFILE       = 1;
    public static final int MISSING_EMAILADDRESS    = 2;
    public static final int INVALID_CONTEXT         = 3;
    public static final int WRITE_SYSTEM_INFO       = 4;
    public static final int MISSING_PERMISSION      = 5;
    public static final int BUSYBOX                 = 6;
    public static final int NOT_EXISTING            = 7;
    public static final int INSUFFICIENT_SPACE      = 8;
    public static final int REMOUNT_SYSTEM          = 9;
    public static final int NO_ROOT_ACCESS          = 10;
    public static final int NO_EXTERNAL_STORAGE     = 11;
    public static final int TIMEOUT                 = 12;
    public static final int COMMAND_FAILED          = 13;

    /**
     * Interface to receive the error code result.
     */
    public interface Listener {
        void onResult(int errorCode);
    }

    /**
     * Interface to return the error code and output from shell calls.
     */
    public interface OutputListener {
        void onResult(int errorCode, List<String> output);
    }
}