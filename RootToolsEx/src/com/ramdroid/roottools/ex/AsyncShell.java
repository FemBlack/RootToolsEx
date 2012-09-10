package com.ramdroid.roottools.ex;

import android.os.AsyncTask;
import com.stericson.RootTools.Command;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.Shell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around the new shell interface from RootTools.
 *
 * Calls to the shell don't block, so you can call it from the main thread.
 */
public class AsyncShell {

    private boolean useRoot;
    private int commandId;
    private String[] command;
    ResultListener listener;

    public void send(boolean useRoot, final int commandId, String... command) {
        this.useRoot = useRoot;
        this.commandId = commandId;
        this.command = command;
        new Worker().execute();
    }

    public interface ResultListener {
        void onFinished(int exitCode, List<String> output);
    }

    public void setResultListener(ResultListener listener) {
        this.listener = listener;
    }

    public static class Exec {
        private List<String> output = new ArrayList<String>();
        private Shell rootShell;
        private boolean useRoot;

        public Exec(boolean useRoot) {
            this.useRoot = useRoot;
        }

        public int run(final int commandId, String... command) {
            int exitCode = -1;
            Command cmd = new Command(0, command) {

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
            }
            catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            return exitCode;
        }

        public void destroy() {
            try {
                rootShell.close();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    private class Worker extends AsyncTask<Boolean, Void, Integer> {

        private Exec exec;

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
