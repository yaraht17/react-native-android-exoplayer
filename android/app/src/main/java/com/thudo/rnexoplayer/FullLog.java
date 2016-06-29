package com.thudo.rnexoplayer;

import android.util.Log;
public class FullLog {
    public final static boolean DEBUG = true;
    public final static boolean INFOR = true;
    public final static boolean ERROR = true;
    public final static boolean WARNING = true;
    public final static boolean VERBOSE = true;

    public static void d(String message, Object... args) {
        if (DEBUG) {
            String fullClassName = Thread.currentThread().getStackTrace()[3].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
            int lineNumber = Thread.currentThread().getStackTrace()[3].getLineNumber();

            Log.d(className + "." + methodName + "():" + lineNumber, String.format(message, args));
        }
    }

    public static void i(String message, Object... args) {
        if (INFOR) {
            String fullClassName = Thread.currentThread().getStackTrace()[3].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
            int lineNumber = Thread.currentThread().getStackTrace()[3].getLineNumber();

            Log.i(className + "." + methodName + "():" + lineNumber, String.format(message, args));
        }
    }

    public static void e(String message, Object... args) {
        if (ERROR) {
            String fullClassName = Thread.currentThread().getStackTrace()[3].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
            int lineNumber = Thread.currentThread().getStackTrace()[3].getLineNumber();

            Log.e(className + "." + methodName + "():" + lineNumber, String.format(message, args));
        }
    }

    public static void w(String message, Object... args) {
        if (WARNING) {
            String fullClassName = Thread.currentThread().getStackTrace()[3].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
            int lineNumber = Thread.currentThread().getStackTrace()[3].getLineNumber();

            Log.w(className + "." + methodName + "():" + lineNumber, String.format(message, args));
        }
    }

    public static void v(String message, Object... args) {
        if (VERBOSE) {
            String fullClassName = Thread.currentThread().getStackTrace()[3].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
            int lineNumber = Thread.currentThread().getStackTrace()[3].getLineNumber();

            Log.v(className + "." + methodName + "():" + lineNumber, String.format(message, args));
        }
    }

}