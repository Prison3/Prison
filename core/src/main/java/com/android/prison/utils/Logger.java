/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.prison.utils;

import android.util.Log;

/**
 * Logger utility class wrapping Android Log functionality
 * All methods return void and delegate to a unified print method
 */
public final class Logger {
    /**
     * Unified print method that all logging methods delegate to
     */
    private static void print(int priority, String tag, String msg) {
        Log.println(priority, "Prison" + "[" + tag + "] ", msg);
    }

    public static void v(String tag, String msg) {
        print(Log.VERBOSE, tag, msg);
    }

    public static void v(String tag, String msg, Throwable tr) {
        print(Log.VERBOSE, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    public static void d(String tag, String msg) {
        print(Log.DEBUG, tag, msg);
    }

    public static void d(String tag, String msg, Throwable tr) {
        print(Log.DEBUG, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    public static void i(String tag, String msg) {
        print(Log.INFO, tag, msg);
    }

    public static void i(String tag, String msg, Throwable tr) {
        print(Log.INFO, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    public static void w(String tag, String msg) {
        print(Log.WARN, tag, msg);
    }

    public static void w(String tag, String msg, Throwable tr) {
        print(Log.WARN, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    public static void w(String tag, Throwable tr) {
        print(Log.WARN, tag, Log.getStackTraceString(tr));
    }

    public static void e(String tag, String msg) {
        print(Log.ERROR, tag, msg);
    }

    public static void e(String tag, String msg, Throwable tr) {
        print(Log.ERROR, tag, msg + '\n' + Log.getStackTraceString(tr));
    }
}
