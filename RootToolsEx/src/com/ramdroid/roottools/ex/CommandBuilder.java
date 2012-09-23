package com.ramdroid.roottools.ex;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Builder interface to create shell commands.
 *
 * Created so we can add more options (e.g. timeouts) in the future
 * without creating dozens of send(...) functions in {@link AsyncShell}
 */
public class CommandBuilder implements Parcelable {

    ArrayList<String> commands = new ArrayList<String>();

    public CommandBuilder() {
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

    @Override
    public int describeContents() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeStringList(commands);
    }

    public static final Parcelable.Creator<CommandBuilder> CREATOR
            = new Parcelable.Creator<CommandBuilder>() {
        public CommandBuilder createFromParcel(Parcel in) {
            return new CommandBuilder(in);
        }

        public CommandBuilder[] newArray(int size) {
            return new CommandBuilder[size];
        }
    };

    private CommandBuilder(Parcel in) {
        in.readStringList(commands);
    }

}
