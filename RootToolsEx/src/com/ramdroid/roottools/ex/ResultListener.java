package com.ramdroid.roottools.ex;

import java.util.List;

/**
 * Interface to return the error code and output from shell calls.
 */
public interface ResultListener {
    void onFinished(int errorCode, List<String> output);
}