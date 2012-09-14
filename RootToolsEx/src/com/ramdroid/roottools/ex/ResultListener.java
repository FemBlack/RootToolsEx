package com.ramdroid.roottools.ex;

import java.util.List;

/**
 * Interface to return the exit code and output from shell calls.
 */
public interface ResultListener {
    void onFinished(int exitCode, List<String> output);
}