package com.ramdroid.roottools.ex;

import com.stericson.RootTools.Command;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.Shell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

/**
 * Wrapper around the new shell interface from RootTools.
 *
 * Calls to {@link ShellExec} are blocking the current thread.
 * Only for internal use in {@link AsyncShell} and {@link ShellService}!
 */
class ShellExec {

    public ArrayList<String> output;

    private Shell rootShell;
    private boolean useRoot;

    private static int commandId = 0;

    public ShellExec(boolean useRoot) {
        this.useRoot = useRoot;
    }

    public int run(String... command) {
        int errorCode = ErrorCode.COMMAND_FAILED;
        commandId += 1;
        output = new ArrayList<String>();
        Command cmd = new Command(commandId, command) {

            @Override
            public void output(int id, String line) {
                if (id == commandId && line != null && line.length() > 0) {
                    output.add(line);
                }
            }
        };

        try {
            rootShell = RootTools.getShell(useRoot);
            int exitCode = rootShell.add(cmd).exitCode();
            if (exitCode == 0) {
                errorCode = ErrorCode.NONE;
            }
        } catch (IOException e) {
            output.add(e.toString());
        } catch (InterruptedException e) {
            output.add(e.toString());
        } catch (TimeoutException e) {
            output.add(e.toString());
            errorCode = ErrorCode.TIMEOUT;
        } catch (Shell.RootDeniedException e) {
            output.add(e.toString());
            errorCode = ErrorCode.NO_ROOT_ACCESS;
        }
        return errorCode;
    }

    public void destroy() {
        try {
            if (rootShell != null) {
                rootShell.close();
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
