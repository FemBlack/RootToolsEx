package com.ramdroid.roottools.ex;

/**
 * Wrapper around some common RootTools APIs.
 *
 * The {@link AsyncShell} class makes sure that all calls to a shell are not
 * executed in the UI thread.
 *
 * Note: there are also some APIs used by {@link AppMover} that are not
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
     * Send a command to a shell.
     *
     * @param useRoot true if you need a root shell
     * @param command the command
     * @param listener Returns the error code and shell output
     */
    public static void send(boolean useRoot, String command, ErrorCode.OutputListener listener) {
        new ShellExec.Worker(ShellExec.API_EX_SEND, useRoot, command, listener).execute();
    }

    /**
     * Send a list of commands to a shell.
     *
     * @param useRoot true if you need a root shell
     * @param commands the commands
     * @param listener Returns the error code and shell output
     */
    public static void send(boolean useRoot, String[] commands, ErrorCode.OutputListener listener) {
        new ShellExec.Worker(ShellExec.API_EX_SEND, useRoot, commands, listener).execute();
    }
}
