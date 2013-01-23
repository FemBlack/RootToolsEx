package com.ramdroid.roottools.ex;

/**
 *    Copyright 2012-2013 by Ronald Ammann (ramdroid)

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

import android.os.Looper;

import java.util.ArrayList;

/**
 * Wrapper around some common RootTools APIs.
 *
 * The {@link com.ramdroid.roottools.ex.Shell} class executes the command in
 * the current thread and returns the result immediately. You will find this
 * class handy if you're already working on a non-UI thread.
 *
 * Note: an exception will be thrown when you try to call from the main thread.
 */
public class Shell {

    private static final String ERR = "This function can not be called from the UI thread.";

    public static class Result {
        public int errorCode;
        public ArrayList<String> output;
    }

    /**
     * Check if root access is available and if we can get access.
     *
     * @return Returns the result: NONE if OK, or NO_ROOT_ACCESS on failure
     */
    public static int gotRoot() {
        if (Looper.myLooper() != null && Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException(ERR);
        }

        return new ShellExec(true).callApi(ShellExec.API_GOTROOT, null);
    }

    /**
     * Check if busybox is available.
     *
     * @return Returns the result: NONE if OK, or BUSYBOX on failure
     */
    public static int gotBusybox() {
        if (Looper.myLooper() != null && Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException(ERR);
        }

        return new ShellExec(true).callApi(ShellExec.API_GOTBUSYBOX, null);
    }

    /**
     * Send a command to a shell. This is the straight-forward solution if you only need to
     * send one simple command at a time.
     *
     * @param useRoot true if you need a root shell
     * @param command the command
     * @return Returns the error code and shell output
     */
    public static Result send(boolean useRoot, String command) {
        if (Looper.myLooper() != null && Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException(ERR);
        }

        Result result = new Result();
        ShellExec exec = new ShellExec(useRoot);
        result.errorCode = exec.callApi(ShellExec.API_SEND, new ParamBuilder().addCommand(command));
        result.output = exec.output;
        return result;
    }

    /**
     * Send one ore more commands to a shell. The list of commands is created using the
     * {@link ParamBuilder}. This way we can add more options in the future without
     * the need to create dozens of different send(..) commands.
     *
     * @param useRoot true if you need a root shell
     * @param params the list of commands created from a  {@link ParamBuilder}
     * @return Returns the error code and shell output
     */
    public static Result send(boolean useRoot, ParamBuilder params) {
        if (Looper.myLooper() != null && Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException(ERR);
        }

        Result result = new Result();
        ShellExec exec = new ShellExec(useRoot);
        result.errorCode = exec.callApi(ShellExec.API_SEND, params);
        result.output = exec.output;
        return result;
    }
}
