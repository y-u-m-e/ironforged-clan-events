package com.ironforgedclanevents;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import sh.yumekui.toolkit.overlay.WrappedText;
import sh.yumekui.toolkit.overlay.WrappedTextComponent;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Movable overlay that draws the Seasonal Event Passphrase
 * (API value when published, else config). It exists so the passphrase shows in
 * screenshots posted to the website, as proof they were taken during the event.
 */
public class SeasonalEventPassphraseOverlay extends OverlayPanel
{
    private static final DateTimeFormatter DATE_TIME_FORMAT =
        DateTimeFormatter.ofPattern("M/d/yyyy h:mm a");

    // Matches WrappedTextComponent: gap between the passphrase and the date on one line.
    private static final int SEGMENT_GAP_SPACES = 4;

    private final IronforgedClanEventsConfig config;
    private final SeasonalEventService seasonalEventService;
    // One instance: OverlayPanel clears its children every frame.
    private final WrappedTextComponent text = new WrappedTextComponent();

    public SeasonalEventPassphraseOverlay(IronforgedClanEventsConfig config, SeasonalEventService seasonalEventService)
    {
        this.config = config;
        this.seasonalEventService = seasonalEventService;
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

        String passphrase = seasonalEventService.resolveEventPassphrase(config);
        if (passphrase == null || passphrase.isEmpty())
        {
            return null;
        }

        List<WrappedText.Segment> segments = new ArrayList<>();
        segments.add(new WrappedText.Segment(passphrase, config.seasonalEventPassphraseColor()));
        if (config.showSeasonalEventDateTime())
        {
            segments.add(new WrappedText.Segment(
                LocalDateTime.now().format(DATE_TIME_FORMAT), config.seasonalEventDateTimeColor()));
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
            FontMetrics metrics = graphics.getFontMetrics();
            int oneLine = WrappedText.layout(segments, metrics::stringWidth, metrics.getHeight(),
                Integer.MAX_VALUE, SEGMENT_GAP_SPACES * metrics.stringWidth(" ")).contentWidth();
            panelComponent.setPreferredSize(new Dimension(
                oneLine + 2 * ComponentConstants.STANDARD_BORDER, 0));
        }

        return super.render(graphics);
    }
}
