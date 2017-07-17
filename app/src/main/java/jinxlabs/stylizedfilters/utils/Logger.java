package jinxlabs.stylizedfilters.utils;

import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Created by uditmukherjee on 17/07/17.
 */

public class Logger {
    private static String TAG = "Stylize";
    private static final int DEFAULT_LOG_LEVEL = Log.DEBUG;
    private final boolean isLoggable = true; // Set to false when it goes to production

    public Logger(final Class<?> callingClass) {
        TAG = callingClass.getSimpleName();
    }

    public Logger(final String tag) {
        TAG = tag;
    }

    private long startTime = 0;
    private String profilingTitle;

    public void startTimeLogging(@NonNull String profilingTitle) {
        startTime = System.currentTimeMillis();
        this.profilingTitle = profilingTitle;
    }

    public void logTimeTaken() {
        if (startTime == 0) return;

        long timeTaken = System.currentTimeMillis() - startTime;
        if (isLoggable) Log.d(TAG, profilingTitle + " took " + timeTaken + " ms");
        startTime = 0;
    }

    private String toMessage(final String format, final Object... args) {
        return (args.length > 0 ? String.format(format, args) : format);
    }

    public void v(final String format, final Object... args) {
        if (isLoggable) Log.v(TAG, toMessage(format, args));
    }

    public void d(final String format, final Object... args) {
        if (isLoggable) Log.d(TAG, toMessage(format, args));
    }

    public void e(final String format, final Object... args) {
        if (isLoggable) Log.e(TAG, toMessage(format, args));
    }

    public void w(final String format, final Object... args) {
        if (isLoggable) Log.w(TAG, toMessage(format, args));
    }

    public void i(final String format, final Object... args) {
        if (isLoggable) Log.i(TAG, toMessage(format, args));
    }
}
