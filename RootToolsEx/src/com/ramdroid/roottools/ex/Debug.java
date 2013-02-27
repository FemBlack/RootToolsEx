package com.ramdroid.roottools.ex;

/**
 *    Copyright 2012-2013 by Ronald Ammann (ramdroid)

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

import android.util.Log;

/**
 * Enable debugging for any components of RootToolsEx.
 */
public class Debug {

    /**
     * By default logging is only enabled on DEBUG builds.
     */
    private static boolean Enabled = BuildConfig.DEBUG;

    /**
     * List all class for which you want to see debugging
     */
    private static final Class<?>[] Levels = {
            ShellExec.class
    };

    public static void setEnabled(boolean enabled) {
        Enabled = enabled;
    }

    public static void log(Object sender, String text) {
        if (Enabled) {
            for (Class<?> level : Levels) {
                if (level == sender.getClass()) {
                    Log.d(sender.getClass().getName(), text);
                }
            }
        }
    }
}
