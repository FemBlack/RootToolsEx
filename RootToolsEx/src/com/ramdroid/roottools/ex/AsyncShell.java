package com.ramdroid.roottools.ex;

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
     * {@link CommandBuilder}. This way we can add more options in the future without
     * the need to create dozens of different send(..) commands.
     *
     * @param useRoot true if you need a root shell
     * @param builder the list of commands created from a  {@link CommandBuilder}
     * @param listener Returns the error code and shell output
     */
    public static void send(boolean useRoot, CommandBuilder builder, ErrorCode.OutputListener listener) {
        new ShellExec.Worker(ShellExec.API_SEND, useRoot, builder, listener).execute();
    }
}
