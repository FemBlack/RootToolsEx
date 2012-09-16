package com.ramdroid.roottools.ex;

import android.os.AsyncTask;
import com.stericson.RootTools.Command;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.Shell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

/**
 * Wrapper around the new shell interface from RootTools.
 *
 * Calls to {@link AsyncShell} don't block, so you can call it from the UI thread.
 */
public class AsyncShell {

    private ResultListener listener;
    private static int commandId = 0;

    /**
     * Executes one or more commands in a shell.
     *
     * @param useRoot True if you need a root shell
     * @param command One or more commands
     * @param listener Returns the result code and shell output.
     */
    public void send(boolean useRoot, String[] command, ResultListener listener) {
        this.listener = listener;
        this.commandId += 1;
        new Worker(useRoot, commandId, command).execute();
    }

    /**
     * Wrapper class around the shell class from RootTools.
     *
     * When using {@link AsyncShell} with the send(..) command then you don't need to
     * bother about this class. It's used internally by the {@link Worker} to execute
     * the shell commands.
     *
     * The class is still declared public because it's also used by other classes like
     * {@link ShellService} or {@link AppMover}.
     */
    public static class Exec {
        public ArrayList<String> output;

        private Shell rootShell;
        private boolean useRoot;

        public Exec(boolean useRoot) {
            this.useRoot = useRoot;
        }

        public int run(final int commandId, String... command) {
            int exitCode = -1;
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
                exitCode = rootShell.add(cmd).exitCode();
            } catch (IOException e) {
                output.add(e.toString());
            } catch (InterruptedException e) {
                output.add(e.toString());
            } catch (TimeoutException e) {
                output.add(e.toString());
            }
            return exitCode;
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

    /**
     * Used by {@link AsyncShell} to run shell commands in a separate thread.
     */
    private class Worker extends AsyncTask<Boolean, Void, Integer> {

        private Exec exec;
        private boolean useRoot;
        private int commandId;
        private String[] command;

        public Worker(boolean useRoot, int commandId, String[] command) {
            this.useRoot = useRoot;
            this.commandId = commandId;
            this.command = command;
        }

        protected Integer doInBackground(Boolean... params) {
            exec = new Exec(useRoot);
            int exitCode = exec.run(commandId, command);
            exec.destroy();
            return exitCode;
        }

        protected void onPreExecute() {

        }

        protected void onPostExecute(Integer exitCode) {
            if (listener != null) {
                listener.onFinished(exitCode, exec.output);
            }
        }
    }

}
