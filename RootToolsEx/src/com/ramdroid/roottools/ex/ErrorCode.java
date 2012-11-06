package com.ramdroid.roottools.ex;

/**
 *    Copyright 2012 by Ronald Ammann (ramdroid)

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

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
    public static final int ALREADY_EXISTING        = 14;
    public static final int INTERNAL                = 15;
    public static final int ODEX_NOT_SUPPORTED      = 16;

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

    /**
     * Interface to return the error code and output from ShellService calls.
     */
    public interface OutputListenerWithId {
        void onResult(int id, int errorCode, List<String> output);
    }
}
