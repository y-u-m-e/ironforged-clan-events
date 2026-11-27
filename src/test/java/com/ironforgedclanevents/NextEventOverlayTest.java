package com.ironforgedclanevents;

import org.junit.Test;
import ws.yume.toolkit.overlay.WrappedText;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Which event the Next Event box shows, and the lines it builds. */
public class NextEventOverlayTest
{
    private static ClanEvent event(String name, long startMinutesFromNow, long lengthMinutes)
    {
        LocalDateTime start = LocalDateTime.now(ZoneId.of("UTC")).plusMinutes(startMinutesFromNow);
        ClanEvent e = new ClanEvent();
        e.setName(name);
        e.setDateTime(start);
        e.setEndDateTime(start.plusMinutes(lengthMinutes));
        return e;
    }

    private static List<String> texts(List<WrappedText.Segment> segments)
    {
        List<String> out = new ArrayList<>();
        for (WrappedText.Segment s : segments)
        {
            out.add(s.text());
        }
        return out;
    }

    @Test
    public void nextIsTheSoonestUpcomingEvent()
    {
        ClanEvent later = event("Later", 300, 60);
        ClanEvent soon = event("Soon", 74, 60);
        ClanEvent past = event("Past", -600, 60);

        assertSame(soon, NextEventOverlay.pickNext(Arrays.asList(later, past, soon), true));
        assertNull(NextEventOverlay.pickNext(Collections.singletonList(past), true));
        assertNull(NextEventOverlay.pickNext(Collections.<ClanEvent>emptyList(), true));
    }

    @Test
    public void liveIsTheRunningEventNotTheUpcomingOne()
    {
        ClanEvent running = event("Running", -10, 60);
        ClanEvent soon = event("Soon", 30, 60);
        List<ClanEvent> events = Arrays.asList(soon, running);

        assertSame(running, NextEventOverlay.pickLive(events, true));
        assertSame(soon, NextEventOverlay.pickNext(events, true));
        assertNull(NextEventOverlay.pickLive(Collections.singletonList(soon), true));
    }

    @Test
    public void segmentsAreOneLinePerFactWithAFormattedCountdown()
    {
        ClanEvent next = event("Bingo Night", 74, 60);
        List<String> lines = texts(NextEventOverlay.buildSegments(null, next));
        assertEquals(3, lines.size());
        assertEquals("Next Event", lines.get(0));
        assertEquals("Bingo Night", lines.get(1));
        assertTrue(lines.get(2), lines.get(2).matches("1h 1[34]m"));

        ClanEvent live = event("Raids", -5, 60);
        List<String> both = texts(NextEventOverlay.buildSegments(live, next));
        assertEquals(Arrays.asList("Active Event", "Raids", "NOW!", "Next Event", "Bingo Night"), both.subList(0, 5));
        assertTrue(NextEventOverlay.buildSegments(null, null).isEmpty());
    }

    @Test
    public void spotlightsCanBeLeftOut()
    {
        ClanEvent sotw = event("SOTW Fishing", -60, 7 * 24 * 60);
        ClanEvent botw = event("BOTW Zulrah", 20, 60);
        ClanEvent raid = event("Raids", 90, 60);
        List<ClanEvent> events = Arrays.asList(sotw, botw, raid);

        assertSame(sotw, NextEventOverlay.pickLive(events, true));
        assertSame(botw, NextEventOverlay.pickNext(events, true));
        assertNull(NextEventOverlay.pickLive(events, false));
        assertSame(raid, NextEventOverlay.pickNext(events, false));
    }

    @Test
    public void compactIsOneLinePerEvent()
    {
        ClanEvent live = event("Raids", -5, 60);
        ClanEvent next = event("Bingo Night", 74, 60);
        List<String> lines = texts(NextEventOverlay.buildCompactSegments(live, next));
        assertEquals(2, lines.size());
        assertEquals("Raids \u00b7 now", lines.get(0));
        assertTrue(lines.get(1), lines.get(1).matches("Bingo Night \u00b7 in 1h 1[34]m"));
        assertTrue(NextEventOverlay.buildCompactSegments(null, null).isEmpty());
    }
}
