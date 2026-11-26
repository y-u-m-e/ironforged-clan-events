package com.ironforgedclanevents;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * While the API is down the scheduler ticks every minute; the backoff is what keeps that from
 * becoming a request per minute.
 */
public class RefreshBackoffTest
{
    private static final long MIN = 60_000L;
    private static final long T0 = 1_000_000_000L;

    @Test
    public void dueImmediatelyBeforeTheFirstAttempt()
    {
        assertTrue(new RefreshBackoff().isDue(15, T0));
    }

    @Test
    public void waitsOneIntervalAfterASuccess()
    {
        RefreshBackoff b = new RefreshBackoff();
        b.succeeded(T0);
        assertFalse(b.isDue(15, T0 + 14 * MIN));
        assertTrue(b.isDue(15, T0 + 15 * MIN));
    }

    @Test
    public void doublesAfterEachFailureCappedAtAnHour()
    {
        RefreshBackoff b = new RefreshBackoff();
        assertTrue("first failure warns", b.failed(T0));
        assertFalse(b.isDue(15, T0 + 29 * MIN));
        assertTrue(b.isDue(15, T0 + 30 * MIN));

        assertFalse("repeat failures do not warn", b.failed(T0));
        assertFalse(b.isDue(15, T0 + 59 * MIN));
        assertTrue(b.isDue(15, T0 + 60 * MIN));

        for (int i = 0; i < 20; i++)
        {
            b.failed(T0);
        }
        assertTrue("capped at 60 minutes", b.isDue(15, T0 + 60 * MIN));
    }

    @Test
    public void aSuccessClearsTheBackoff()
    {
        RefreshBackoff b = new RefreshBackoff();
        b.failed(T0);
        b.failed(T0);
        b.succeeded(T0);
        assertTrue(b.isDue(15, T0 + 15 * MIN));
        assertTrue("next failure warns again", b.failed(T0));
    }

    @Test
    public void resetMakesItDueNow()
    {
        RefreshBackoff b = new RefreshBackoff();
        b.succeeded(T0);
        b.reset();
        assertTrue(b.isDue(15, T0));
    }
}
