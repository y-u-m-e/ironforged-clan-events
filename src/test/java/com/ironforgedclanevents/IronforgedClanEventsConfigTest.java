package com.ironforgedclanevents;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

/** The Hub requires every switch that contacts a third-party server to default to off. */
public class IronforgedClanEventsConfigTest
{
    private final IronforgedClanEventsConfig config = new IronforgedClanEventsConfig() {};

    @Test
    public void everyNetworkSwitchDefaultsToOff()
    {
        assertFalse(config.enableEventsFeed());
        assertFalse(config.showSeasonalEventPassphrase());
        assertFalse(config.enableTeamRosterSync());
    }
}
