package com.ironforgedclanevents;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import ws.yume.toolkit.overlay.WrappedText;
import ws.yume.toolkit.overlay.WrappedTextComponent;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Movable "Next Event" box (title, name, countdown; plus the live event when one is running),
 * built from the events the panel already fetched. Returns on the wrapping text component, so
 * narrowing the box wraps lines instead of clipping.
 */
public class NextEventOverlay extends OverlayPanel
{
    private static final Color BACKGROUND = new Color(30, 30, 30, 200);
    private static final Color TEXT = Color.WHITE;
    private static final Color ACCENT = new Color(255, 200, 0);
    private static final Color IMMINENT = new Color(255, 100, 100);
    private static final Color ACTIVE = new Color(100, 255, 100);
    private static final long REBUILD_MS = 1000;

    private final IronforgedClanEventsConfig config;
    private final EventsApiService eventsApiService;
    // One instance: OverlayPanel clears its children every frame.
    private final WrappedTextComponent text = new WrappedTextComponent();
    private List<WrappedText.Segment> segments = new ArrayList<>();
    private long builtAt;
    private boolean builtSpotlights;
    private IronforgedClanEventsConfig.NextEventStyle builtStyle;

    public NextEventOverlay(IronforgedClanEventsConfig config, EventsApiService eventsApiService)
    {
        this.config = config;
        this.eventsApiService = eventsApiService;
        setPosition(OverlayPosition.TOP_LEFT);
        setMovable(true);
        setSnappable(true);
        setResizable(true);
    }

    /** The upcoming event that starts soonest, or null. */
    static ClanEvent pickNext(List<ClanEvent> events, boolean spotlights)
    {
        ClanEvent best = null;
        for (ClanEvent e : events)
        {
            if (e.isUpcoming() && (spotlights || !e.isWeeklySpotlight()) && (best == null || e.compareTo(best) < 0))
            {
                best = e;
            }
        }
        return best;
    }

    /** The running event that started first, or null. */
    static ClanEvent pickLive(List<ClanEvent> events, boolean spotlights)
    {
        ClanEvent best = null;
        for (ClanEvent e : events)
        {
            if (e.isHappeningNow() && (spotlights || !e.isWeeklySpotlight()) && (best == null || e.compareTo(best) < 0))
            {
                best = e;
            }
        }
        return best;
    }

    /** Compact: one line per event, "Name - now" and "Name - in 1h 5m". */
    static List<WrappedText.Segment> buildCompactSegments(ClanEvent live, ClanEvent next)
    {
        List<WrappedText.Segment> out = new ArrayList<>();
        if (live != null)
        {
            out.add(new WrappedText.Segment(displayName(live) + " - now", ACTIVE));
        }
        if (next != null)
        {
            Color color = next.isImminent() ? IMMINENT : ACCENT;
            out.add(new WrappedText.Segment(displayName(next) + " - in " + next.getTimeUntil(), color));
        }
        return out;
    }

    private static String displayName(ClanEvent e)
    {
        return e.getName() == null || e.getName().isEmpty() ? "Event" : e.getName();
    }

    /** Verbose (the old box): live block (Active Event, name, NOW!) then next block (title, name, countdown). */
    static List<WrappedText.Segment> buildSegments(ClanEvent live, ClanEvent next)
    {
        List<WrappedText.Segment> out = new ArrayList<>();
        if (live != null)
        {
            out.add(new WrappedText.Segment("Active Event", ACTIVE));
            addName(out, live);
            out.add(new WrappedText.Segment("NOW!", ACTIVE));
        }
        if (next != null)
        {
            boolean imminent = next.isImminent();
            out.add(new WrappedText.Segment("Next Event", imminent ? IMMINENT : ACCENT));
            addName(out, next);
            out.add(new WrappedText.Segment(next.getTimeUntil(), imminent ? IMMINENT : TEXT));
        }
        return out;
    }

    private static void addName(List<WrappedText.Segment> out, ClanEvent e)
    {
        if (e.getName() != null && !e.getName().isEmpty())
        {
            out.add(new WrappedText.Segment(e.getName(), TEXT));
        }
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showOverlay() || !config.enableEventsFeed())
        {
            return null;
        }

        // The countdown is minute-granular; rebuilding once a second keeps the frame cheap.
        // A settings change rebuilds at once so switching style or SOTW/BOTW feels instant.
        long now = System.currentTimeMillis();
        boolean spotlights = config.nextEventShowSpotlights();
        IronforgedClanEventsConfig.NextEventStyle style = config.nextEventStyle();
        if (now - builtAt >= REBUILD_MS || spotlights != builtSpotlights || style != builtStyle)
        {
            builtAt = now;
            builtSpotlights = spotlights;
            builtStyle = style;
            List<ClanEvent> events = eventsApiService.getCachedEvents();
            ClanEvent live = pickLive(events, spotlights);
            ClanEvent next = pickNext(events, spotlights);
            segments = style == IronforgedClanEventsConfig.NextEventStyle.COMPACT
                ? buildCompactSegments(live, next)
                : buildSegments(live, next);
        }
        if (segments.isEmpty())
        {
            return null;
        }
        text.setSegments(segments);

        panelComponent.getChildren().clear();
        panelComponent.setBackgroundColor(BACKGROUND);
        panelComponent.getChildren().add(text);
        setMinimumSize(text.minimumWidth(graphics));

        // Default width: the widest line, so the lines stack (segments only share a line when all fit).
        if (getPreferredSize() == null)
        {
            FontMetrics metrics = graphics.getFontMetrics();
            int widest = 0;
            for (WrappedText.Segment s : segments)
            {
                widest = Math.max(widest, metrics.stringWidth(s.text()));
            }
            panelComponent.setPreferredSize(new Dimension(widest + 2 * ComponentConstants.STANDARD_BORDER, 0));
        }

        return super.render(graphics);
    }
}
