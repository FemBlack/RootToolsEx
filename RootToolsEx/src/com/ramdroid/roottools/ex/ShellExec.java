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

import android.os.AsyncTask;
import android.util.Log;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.Command;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.Shell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;


/**
 * Wrapper around the new shell interface from RootTools.
 *
 * Calls to {@link ShellExec} are blocking the current thread.
 * Only for internal use in {@link AsyncShell} and {@link ShellService}
 */
class ShellExec {

    // RootTools wrapper APIs
    public static final int API_GOTROOT                     = 1;
    public static final int API_GOTBUSYBOX                  = 2;
    public static final int API_SEND                        = 3;

    // More APIs that are not part of the RootTools classes
    public static final int API_EX_APPEXISTSONPARTITION     = 101;
    public static final int API_EX_APPFITSONPARTITION       = 102;
    public static final int API_EX_MOVEAPPEX                = 103;

    public ArrayList<String> output;

    private Shell rootShell;
    private boolean useRoot;

    private static int commandId = 0;

    private static final String TAG = "ShellExec";

    public ShellExec(boolean useRoot) {
        this.useRoot = useRoot;
    }

    public int run(String... command) {
        return run(0, command);
    }

    public int run(int timeout, String... command) {
        int errorCode = ErrorCode.COMMAND_FAILED;
        commandId += 1;
        output = new ArrayList<String>();
        Log.d(TAG, "Cmd " + commandId + ": " + command.toString());
        Command cmd = new Command(commandId, command) {

            @Override
            public void output(int id, String line) {
                if (id == commandId && line != null && line.length() > 0) {
                    Log.d(TAG, "ID " + id + ": " + line);
                    output.add(line);
                }
            }
        };

        try {
            rootShell = RootTools.getShell(useRoot);
            int exitCode;
            if (timeout > 0) {
                exitCode = rootShell.add(cmd).exitCode(timeout);
            }
            else {
                exitCode = rootShell.add(cmd).exitCode();
            }
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
        } catch (RootDeniedException e) {
            output.add(e.toString());
            errorCode = ErrorCode.NO_ROOT_ACCESS;
        }
        return errorCode;
    }

    public int callApi(int api, Params params, Integer... flags) {
        int errorCode = ErrorCode.NONE;
        if (api == API_GOTROOT) {
            boolean gotRoot = false;
            try {
                if (RootTools.isRootAvailable() && RootTools.isAccessGiven()) {
                    gotRoot = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            errorCode = gotRoot ? ErrorCode.NONE : ErrorCode.NO_ROOT_ACCESS;
        }
        else if (api == API_GOTBUSYBOX) {
            String version = RootTools.getBusyBoxVersion();
            boolean available = (version != null && version.length() > 0);
            errorCode = available ? ErrorCode.NONE : ErrorCode.BUSYBOX;
        }
        else if (api == API_SEND) {
            errorCode = run(params.timeout, params.commands);
        }
        else if (api == API_EX_APPEXISTSONPARTITION) {
            errorCode = AppManager.Internal.appExistsOnPartition(this, params.packageName, params.partition);
        }
        else if (api == API_EX_APPFITSONPARTITION) {
            errorCode = AppManager.Internal.appFitsOnPartition(this, params.packageName, params.partition);
        }
        else if (api == API_EX_MOVEAPPEX) {
            errorCode = AppManager.Internal.moveAppEx(
                    this,
                    params.packageName,
                    params.partition,
                    params.target,
                    flags[0]);
        }
        clear();
        return errorCode;
    }

    public void clear() {
        try {
            if (rootShell != null) {
                rootShell.close();
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public static class Params {
        private String[] commands;
        private String packageName;
        private String partition;
        private String target;
        private int timeout;

        public Params() {
        }

        public Params(String command) {
            this.commands = new String[] { command };
        }

        public Params(CommandBuilder builder) {
            this.commands = builder.commands.toArray(new String[builder.commands.size()]);
        }
    }

    /**
     * Worker to execute all shell commands in a separate thread.
     */
    public static class Worker extends AsyncTask<Integer, Void, Integer> {

        private int api;
        private ErrorCode.OutputListener listener;
        private ShellExec exec;
        private boolean useRoot;
        private Params params = new Params();

        public Worker(int api, ErrorCode.OutputListener listener) {
            this.api = api;
            this.listener = listener;
            this.useRoot = true;
            params.timeout = 0;
        }

        public Worker(int api, boolean useRoot, String command, ErrorCode.OutputListener listener) {
            this.api = api;
            this.useRoot = useRoot;
            this.listener = listener;
            params.commands = new String[] { command };
            params.timeout = 0;
        }

        public Worker(int api, boolean useRoot, CommandBuilder builder, ErrorCode.OutputListener listener) {
            this.api = api;
            this.useRoot = useRoot;
            this.listener = listener;
            params.commands = builder.commands.toArray(new String[builder.commands.size()]);
            params.timeout = builder.timeout;
        }

        public Worker(int api, String packageName, String partition, ErrorCode.OutputListener listener) {
            this.api = api;
            this.useRoot = true;
            this.listener = listener;
            params.packageName = packageName;
            params.partition = partition;
            params.timeout = 0;
        }

        public Worker(int api, String packageName, String partition, String target, ErrorCode.OutputListener listener) {
            this.api = api;
            this.useRoot = true;
            this.listener = listener;
            params.packageName = packageName;
            params.partition = partition;
            params.target = target;
            params.timeout = 0;
        }

        @Override
        protected Integer doInBackground(Integer... flags) {
            exec = new ShellExec(useRoot);
            int errorCode = exec.callApi(api, params, flags);
            return errorCode;
        }

        protected void onPostExecute(Integer errorCode) {
            if (listener != null) {
                if (exec.output == null) {
                    exec.output = new ArrayList<String>();
                }
                listener.onResult(errorCode, exec.output);
            }
        }
    }
}
