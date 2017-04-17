package com.github.teocci.virtualjoystick.utils;

import android.util.Log;

public class Debug
{
    public static final boolean DEBUG = true;	// 디버그 모드
    private static final String TAG = "tya";

    public static void log(Class cls, Object msg)
    {
        if ( DEBUG ) {
            Log.d(TAG, cls.getSimpleName() + msg.toString());
        }
    }

    public static void err(Class cls, String msg)
    {
        if ( DEBUG ) {
            Log.e(TAG, cls.getSimpleName()+msg);
        }
    }

    /**
     * 로그를 남긴다
     */
    public static void log(String msg)
    {
        if ( DEBUG ) {
            Log.d(TAG, msg);
        }
    }

    public static void log(CharSequence msg)
    {
        if ( DEBUG ) {
            Log.d(TAG, msg.toString());
        }
    }

    public static void err(String tag, Throwable e)
    {
        if ( DEBUG ) {
            StackTraceElement[] trace = e.getStackTrace();
            Log.e(tag, e.getClass().getName() + ":" + e.getMessage());
            for ( StackTraceElement t : trace ) {
                Log.e(TAG, t.toString());
            }
        }
    }

    public static void err(Exception e)
    {
        if ( DEBUG ) {
            StackTraceElement[] trace = e.getStackTrace();
            Log.e(TAG, e.getClass().getName() + ":" + e.getMessage());
            for ( StackTraceElement t : trace ) {
                Log.e(TAG, t.toString());
            }
        }
    }

    public static void log(Exception e)
    {
        if ( DEBUG ) {
            StackTraceElement[] trace = e.getStackTrace();
            Log.d(TAG, e.getClass().getName() + ":" + e.getMessage());
            for ( StackTraceElement t : trace ) {
                Log.e(TAG, t.toString());
            }
        }
    }

    public static void err(Object err)
    {
        if ( DEBUG ) {
            Log.e(TAG, err.toString());
        }
    }
}
