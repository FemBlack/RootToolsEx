package com.ramdroid.roottools.ex;

import android.os.AsyncTask;
import com.stericson.RootTools.Command;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.Shell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Wrapper around the new shell interface from RootTools.
 *
 * Calls to the shell don't block, so you can call it from the main thread.
 */
public class AsyncShell {

    private ResultListener listener;

    private static int commandId = 0;

    public void send(boolean useRoot, String[] command, ResultListener listener) {
        this.listener = listener;
        commandId += 1;

        Worker w = new Worker();
        w.useRoot = useRoot;
        w.commandId = commandId;
        w.command = command;
        w.execute();
    }

    public interface ResultListener {
        void onFinished(int exitCode, List<String> output);
    }

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
                    if (line != null && id == commandId) {
                        output.add(line);
                    }
                }
            };

            try {
                rootShell = RootTools.getShell(useRoot);
                exitCode = rootShell.add(cmd).exitCode();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (TimeoutException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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

    private class Worker extends AsyncTask<Boolean, Void, Integer> {

        private Exec exec;
        private boolean useRoot;
        private int commandId;
        private String[] command;

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
