package com.ironforgedclanevents;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

import java.awt.Color;

/**
 * Plugin settings. Every option that contacts the API sits in the Iron Forged API section.
 */
@ConfigGroup(IronforgedClanEventsConfig.CONFIG_GROUP)
public interface IronforgedClanEventsConfig extends Config
{
    String CONFIG_GROUP = "ironforgedclanevents";

    @ConfigSection(
        name = "Iron Forged API",
        description = "Everything that contacts api.ironforged.gg. All off by default",
        position = 0
    )
    String apiSection = "apiSection";

    @ConfigSection(
        name = "Panel",
        description = "Configure panel settings",
        position = 1
    )
    String panelSection = "panelSection";

    @ConfigSection(
        name = "Overlays",
        description = "Boxes drawn on the game screen",
        position = 2
    )
    String overlaySection = "overlaySection";

    @ConfigSection(
        name = "Teams",
        description = "Colored team badges in chat, and friends/clan lists",
        position = 3
    )
    String teamsSection = "teamsSection";

    // ========== Iron Forged API ==========

    @ConfigItem(
        keyName = "enableEventsFeed",
        name = "Load clan events",
        description = "Fetch the Iron Forged events calendar from api.ironforged.gg over the internet",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        section = apiSection,
        position = 0
    )
    default boolean enableEventsFeed()
    {
        return false;
    }

    @ConfigItem(
        keyName = "refreshInterval",
        name = "Refresh Interval (min)",
        description = "How often to refresh event data (in minutes)",
        section = apiSection,
        position = 1
    )
    @Range(min = 5, max = 60)
    default int refreshInterval()
    {
        return 15;
    }

    @ConfigItem(
        keyName = "enableTeamRosterSync",
        name = "Sync team roster",
        description = "Fetch team names, colors and members from api.ironforged.gg. Needs the Seasonal Event ID. Overwrites local edits in the Seasonal tab when it runs",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        section = apiSection,
        position = 2
    )
    default boolean enableTeamRosterSync()
    {
        return false;
    }

    @ConfigItem(
        keyName = "seasonalEventId",
        name = "Seasonal Event ID",
        description = "The event id staff announce for this season. Team roster sync only returns teams for the current id",
        section = apiSection,
        position = 3
    )
    default String seasonalEventId()
    {
        return "";
    }

    @ConfigItem(
        keyName = "seasonalCatalogRefreshInterval",
        name = "Roster refresh (min)",
        description = "How often to re-fetch the team roster",
        section = apiSection,
        position = 4
    )
    @Range(min = 15, max = 60)
    default int seasonalCatalogRefreshInterval()
    {
        return 15;
    }

    // ========== Overlays ==========

    @ConfigItem(
        keyName = "showSeasonalEventPassphrase",
        name = "Show event passphrase",
        description = "Draw the Seasonal Event Passphrase you enter below on the game screen (drag to reposition)",
        section = overlaySection,
        position = 1
    )
    default boolean showSeasonalEventPassphrase()
    {
        return false;
    }

    @ConfigItem(
        keyName = "seasonalEventPassphrase",
        name = "Seasonal Event Passphrase",
        description = "The passphrase staff announce for this season, shown on the passphrase overlay",
        section = overlaySection,
        position = 2
    )
    default String seasonalEventPassphrase()
    {
        return "";
    }

    @ConfigItem(
        keyName = "showSeasonalEventDateTime",
        name = "Show Date & Time",
        description = "Show the current local date and time beside the Seasonal Event Passphrase",
        section = overlaySection,
        position = 3
    )
    default boolean showSeasonalEventDateTime()
    {
        return false;
    }

    @ConfigItem(
        keyName = "seasonalEventPassphraseColor",
        name = "Passphrase Color",
        description = "Color of the Seasonal Event Passphrase text",
        section = overlaySection,
        position = 4
    )
    default Color seasonalEventPassphraseColor()
    {
        return new Color(232, 140, 36);
    }

    @ConfigItem(
        keyName = "seasonalEventDateTimeColor",
        name = "Date & Time Color",
        description = "Color of the date and time text",
        section = overlaySection,
        position = 5
    )
    default Color seasonalEventDateTimeColor()
    {
        return Color.WHITE;
    }

    @ConfigItem(
        keyName = "seasonalEventPassphraseBackgroundColor",
        name = "Background Color",
        description = "Background color behind the Seasonal Event Passphrase",
        section = overlaySection,
        position = 6
    )
    default Color seasonalEventPassphraseBackgroundColor()
    {
        return new Color(30, 30, 30, 180);
    }

    // ========== Panel Settings ==========

    @ConfigItem(
        keyName = "showSidebarButton",
        name = "Show sidebar button",
        description = "Show the Ironforged icon in RuneLite's sidebar to open the side panel",
        section = panelSection,
        position = 0
    )
    default boolean showSidebarButton()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showOverlay",
        name = "Show next event overlay",
        description = "Display the next event (and the live one) on the game screen. Needs Load clan events; drag to reposition",
        section = overlaySection,
        position = 0
    )
    default boolean showOverlay()
    {
        return true;
    }

    @ConfigItem(
        keyName = "maxEventsShown",
        name = "Max Events Shown",
        description = "Maximum number of events to show in the panel (scroll for more)",
        section = panelSection,
        position = 1
    )
    @Range(min = 1, max = 50)
    default int maxEventsInPanel()
    {
        return 10;
    }

    @ConfigItem(
        keyName = "showPastEvents",
        name = "Show Past Events",
        description = "Include events that have already passed",
        section = panelSection,
        position = 2
    )
    default boolean showPastEvents()
    {
        return false;
    }

    // ========== Teams ==========

    @ConfigItem(
        keyName = "enableTeamIcons",
        name = "Enable team icons",
        description = "Show colored team badges for rostered players (edit rosters in the Teams side-panel tab)",
        section = teamsSection,
        position = 0
    )
    default boolean enableTeamIcons()
    {
        return true;
    }

    @ConfigItem(
        keyName = "teamCount",
        name = "Number of teams",
        description = "How many teams to show in the Teams tab (1–10). Prefer editing from the Teams tab.",
        section = teamsSection,
        position = 2,
        hidden = true
    )
    @Range(min = 1, max = 10)
    default int teamCount()
    {
        return 2;
    }

    @ConfigItem(
        keyName = "friendsListTeamIcons",
        name = "Friends list icons",
        description = "Show team icons next to names on the friends list",
        section = teamsSection,
        position = 4
    )
    default boolean friendsListTeamIcons()
    {
        return true;
    }

    @ConfigItem(
        keyName = "clanListTeamIcons",
        name = "Clan member list icons",
        description = "Show team icons next to names in the clan and guest clan member lists",
        section = teamsSection,
        position = 5
    )
    default boolean clanListTeamIcons()
    {
        return true;
    }

    @ConfigItem(
        keyName = "broadcastTeamIcons",
        name = "Broadcast icons",
        description = "Also show team icons on collection log and drop broadcasts in clan chat",
        section = teamsSection,
        position = 6
    )
    default boolean broadcastTeamIcons()
    {
        return true;
    }
}
