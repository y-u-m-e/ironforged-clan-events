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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Movable overlay that draws the Seasonal Event Passphrase typed in settings. It exists so the passphrase shows in
 * screenshots posted to the website, as proof they were taken during the event.
 */
public class SeasonalEventPassphraseOverlay extends OverlayPanel
{
    private static final DateTimeFormatter DATE_TIME_FORMAT =
        DateTimeFormatter.ofPattern("M/d/yyyy h:mm a");

    // Matches WrappedTextComponent: gap between the passphrase and the date on one line.
    private static final int SEGMENT_GAP_SPACES = 4;

    private final IronforgedClanEventsConfig config;
    // One instance: OverlayPanel clears its children every frame.
    private final WrappedTextComponent text = new WrappedTextComponent();
    private List<WrappedText.Segment> segments = new ArrayList<>();
    private String builtPassphrase = "";
    private Color builtColor;
    private Color builtDateColor;
    private boolean builtShowDate;
    private long builtAt;
    private int oneLineWidth = -1;

    public SeasonalEventPassphraseOverlay(IronforgedClanEventsConfig config)
    {
        this.config = config;
        setPosition(OverlayPosition.TOP_CENTER);
        setMovable(true);
        setSnappable(true);
        setResizable(true);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showSeasonalEventPassphrase())
        {
            return null;
        }

        String passphrase = config.seasonalEventPassphrase() == null ? "" : config.seasonalEventPassphrase().trim();
        if (passphrase.isEmpty())
        {
            return null;
        }

        // Rebuild only when a setting changed, or once a second for the minute-granular clock.
        boolean showDate = config.showSeasonalEventDateTime();
        Color color = config.seasonalEventPassphraseColor();
        Color dateColor = config.seasonalEventDateTimeColor();
        long now = System.currentTimeMillis();
        if (!passphrase.equals(builtPassphrase) || !color.equals(builtColor) || !dateColor.equals(builtDateColor)
            || showDate != builtShowDate || (showDate && now - builtAt >= 1000))
        {
            builtPassphrase = passphrase;
            builtColor = color;
            builtDateColor = dateColor;
            builtShowDate = showDate;
            builtAt = now;
            segments = new ArrayList<>();
            segments.add(new WrappedText.Segment(passphrase, color));
            if (showDate)
            {
                segments.add(new WrappedText.Segment(LocalDateTime.now().format(DATE_TIME_FORMAT), dateColor));
            }
            oneLineWidth = -1;
        }
        text.setSegments(segments);

        panelComponent.getChildren().clear();
        panelComponent.setBackgroundColor(config.seasonalEventPassphraseBackgroundColor());
        panelComponent.getChildren().add(text);

        int minimumWidth = text.minimumWidth(graphics);
        setMinimumSize(minimumWidth);

        // The overlay's preferredSize is only set when the user drags to resize (and cleared
        // by "Reset"); OverlayPanel then applies it. Otherwise show everything on one line.
        if (getPreferredSize() == null)
        {
            if (oneLineWidth < 0)
            {
                FontMetrics metrics = graphics.getFontMetrics();
                oneLineWidth = WrappedText.layout(segments, metrics::stringWidth, metrics.getHeight(),
                    Integer.MAX_VALUE, SEGMENT_GAP_SPACES * metrics.stringWidth(" ")).contentWidth();
            }
            panelComponent.setPreferredSize(new Dimension(
                oneLineWidth + 2 * ComponentConstants.STANDARD_BORDER, 0));
        }

        return super.render(graphics);
    }
}
