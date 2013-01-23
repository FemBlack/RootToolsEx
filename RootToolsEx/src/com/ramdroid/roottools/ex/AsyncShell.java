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

/**
 * Wrapper around some common RootTools APIs.
 *
 * The {@link AsyncShell} class makes sure that all calls to a shell are not
 * executed in the UI thread.
 *
 * Note: there are also some APIs used by {@link AppManager} that are not
 * part of the RootTools APIs.
 */
public class AsyncShell {

    /**
     * Check if root access is available and if we can get access.
     *
     * @param listener Returns the result: NONE if OK, or NO_ROOT_ACCESS on failure
     */
    public static void gotRoot(ErrorCode.OutputListener listener) {
        new ShellExec.Worker(ShellExec.API_GOTROOT, listener).execute();
    }

    /**
     * Check if busybox is available.
     *
     * @param listener Returns the result: NONE if OK, or BUSYBOX on failure
     */
    public static void gotBusybox(ErrorCode.OutputListener listener) {
        new ShellExec.Worker(ShellExec.API_GOTBUSYBOX, listener).execute();
    }

    /**
     * Send a command to a shell. This is the straight-forward solution if you only need to
     * send one simple command at a time.
     *
     * @param useRoot true if you need a root shell
     * @param command the command
     * @param listener Returns the error code and shell output
     */
    public static void send(boolean useRoot, String command, ErrorCode.OutputListener listener) {
        new ShellExec.Worker(ShellExec.API_SEND, useRoot, command, listener).execute();
    }

    /**
     * Send one ore more commands to a shell. The list of commands is created using the
     * {@link ParamBuilder}. This way we can add more options in the future without
     * the need to create dozens of different send(..) commands.
     *
     * @param useRoot true if you need a root shell
     * @param builder the list of commands created from a  {@link ParamBuilder}
     * @param listener Returns the error code and shell output
     */
    public static void send(boolean useRoot, ParamBuilder builder, ErrorCode.OutputListener listener) {
        new ShellExec.Worker(ShellExec.API_SEND, useRoot, builder, listener).execute();
    }
}
