package com.ironforgedclanevents;

/**
 * When a poller is next due: one interval after a success, and after failures the interval
 * doubled per failure in a row, capped at 60 minutes. Gated on the last attempt, not the last
 * success, so an outage does not turn into a request every scheduler tick.
 */
final class RefreshBackoff
{
    static final long MAX_BACKOFF_MS = 60 * 60_000L;

    private long lastAttemptTime;
    private int failures;

    synchronized boolean isDue(int intervalMinutes, long now)
    {
        if (lastAttemptTime == 0)
        {
            return true;
        }
        long wait = Math.max(1, intervalMinutes) * 60_000L;
        if (failures > 0)
        {
            wait = Math.min(wait << Math.min(failures, 6), Math.max(wait, MAX_BACKOFF_MS));
        }
        return now - lastAttemptTime >= wait;
    }

    synchronized void succeeded(long now)
    {
        lastAttemptTime = now;
        failures = 0;
    }

    /** @return true for the first failure in a row, so callers warn once and log repeats at debug. */
    synchronized boolean failed(long now)
    {
        lastAttemptTime = now;
        return failures++ == 0;
    }

    synchronized void reset()
    {
        lastAttemptTime = 0;
        failures = 0;
    }
}
