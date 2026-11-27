package com.ironforgedclanevents;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

/** The Hub requires every switch that contacts a third-party server to default to off. The passphrase box is local, but stays off by default too. */
public class IronforgedClanEventsConfigTest
{
    private final IronforgedClanEventsConfig config = new IronforgedClanEventsConfig() {};

    @Test
    public void everyNetworkSwitchDefaultsToOff()
    {
        assertFalse(config.enableEventsFeed());
        assertFalse(config.enableTeamRosterSync());
    }

    @Test
    public void passphraseOverlayDefaultsToOff()
    {
        assertFalse(config.showSeasonalEventPassphrase());
    }
}
