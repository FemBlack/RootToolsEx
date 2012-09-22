package com.ramdroid.roottools.ex;

import java.util.ArrayList;

/**
 * Builder interface to create shell commands.
 *
 * Created so we can add more options (e.g. timeouts) in the future
 * without creating dozens of send(...) functions in {@link AsyncShell}
 */
public class CommandBuilder {

    boolean useRoot = false;
    ArrayList<String> commands = new ArrayList<String>();

    public CommandBuilder() {
    }

    /**
     * Call this function if you need a root shell
     * @return the {@link CommandBuilder} object
     */
    public CommandBuilder useRoot() {
        this.useRoot = true;
        return this;
    }

    /**
     * Add a command to be executed in the shell.
     * Call this function multiple times for each command.
     *
     * @param command One shell command line
     * @return the {@link CommandBuilder} object
     */
    public CommandBuilder add(String command) {
        this.commands.add(command);
        return this;
    }
}
