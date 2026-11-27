package com.ironforgedclanevents;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.ClanMemberJoined;
import net.runelite.api.events.ClanMemberLeft;
import net.runelite.api.events.FriendsChatSync;
import net.runelite.api.events.FriendsChatMemberJoined;
import net.runelite.api.events.FriendsChatMemberLeft;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ironforged Clan Events: events side panel, your seasonal team, passphrase overlay and event team badges.
 */
@Slf4j
@PluginDescriptor(
    name = "Ironforged Clan Events",
    description = "Display Iron Forged clan events from api.ironforged.gg in a side panel, with your seasonal team, an optional Seasonal Event Passphrase overlay, and event team icons. Enable Load clan events in settings.",
    tags = {"clan", "events", "calendar", "schedule", "seasonal", "ironforged", "teams"}
)
public class IronforgedClanEventsPlugin extends Plugin
{
    private static final String ICON_PATH = "ironforged_icon.png";
    private static final int NAV_ICON_SIZE = 16;

    @Inject
    private IronforgedClanEventsConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private EventsApiService eventsApiService;

    @Inject
    private SeasonalEventService seasonalEventService;

    @Inject
    private EventTeamService eventTeamService;

    @Inject
    private EventTeamSyncService eventTeamSyncService;

    @Inject
    private ScheduledExecutorService executorService;

    @Inject
    private Client client;

    /** Config group of the Plugin Hub build before the rename (8d48b19). */
    static final String LEGACY_CONFIG_GROUP = "claneventlist";
    static final int MIN_EVENTS_INTERVAL = 5;
    static final int MIN_ROSTER_INTERVAL = 15;

    private SeasonalEventPassphraseOverlay passphraseOverlay;
    private NextEventOverlay nextEventOverlay;
    /** Set and cleared on the EDT, read from executor threads: copy to a local before use. */
    private volatile IronforgedClanEventsPanel panel;
    private NavigationButton navButton;
    private volatile boolean initialized = false;
    private final AtomicBoolean eventsFetchInFlight = new AtomicBoolean();
    /** Client thread only. True from the login screen / hop / reconnect until the next LOGGED_IN. */
    private boolean loginPending;
    /** Client thread only. Clan / friends chat membership changed; recount once on the next tick. */
    private boolean onlineCountsDirty;

    @Override
    protected void startUp() throws Exception
    {
        log.info("Ironforged Clan Events plugin started");

        migrateConfigKey(configManager, "maxEventsShown");
        migrateConfigKey(configManager, "showPastEvents");
        migrateConfigKey(configManager, "refreshInterval");
        migrateConfigKey(configManager, "showOverlay");

        passphraseOverlay = new SeasonalEventPassphraseOverlay(config, seasonalEventService);
        overlayManager.add(passphraseOverlay);
        nextEventOverlay = new NextEventOverlay(config, eventsApiService);
        overlayManager.add(nextEventOverlay);

        panel = new IronforgedClanEventsPanel(
            config,
            configManager,
            eventsApiService,
            eventTeamService,
            eventTeamSyncService,
            () -> refreshEvents(true),
            this::syncTeams
        );

        BufferedImage icon = loadPluginIcon();

        navButton = NavigationButton.builder()
            .tooltip("Ironforged Clan Events")
            .icon(icon)
            .priority(5)
            .panel(panel)
            .build();

        if (config.showSidebarButton())
        {
            clientToolbar.addNavigation(navButton);
        }

        loginPending = client.getGameState() != GameState.LOGGED_IN;
        initialized = true;
        eventTeamService.start();
        refreshEvents();
        refreshSeasonalEvent();
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.info("Ironforged Clan Events plugin stopped");

        overlayManager.remove(passphraseOverlay);
        overlayManager.remove(nextEventOverlay);
        clientToolbar.removeNavigation(navButton);
        if (panel != null)
        {
            panel.closeEventDialog();
        }
        seasonalEventService.clear();
        eventsApiService.clearEvents();
        eventTeamSyncService.reset();
        eventTeamService.stop();

        initialized = false;
        passphraseOverlay = null;
        nextEventOverlay = null;
        panel = null;
        navButton = null;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        switch (event.getGameState())
        {
            case LOGIN_SCREEN:
            case LOGGING_IN:
            case HOPPING:
            case CONNECTION_LOST:
                loginPending = true;
                return;
            case LOGGED_IN:
                // LOGGED_IN also follows every LOADING (each region change); act only after a real login.
                if (!loginPending || !initialized)
                {
                    return;
                }
                loginPending = false;
                break;
            default:
                return;
        }

        eventTeamService.refreshSurfaces();
        IronforgedClanEventsPanel p = panel;
        if (p != null)
        {
            p.refreshTeamsOnlineCounts();
        }
        if (eventsApiService.needsRefresh(eventsInterval()))
        {
            refreshEvents();
        }
        if (seasonalEventNeeded() && seasonalEventService.needsRefresh(rosterInterval()))
        {
            refreshSeasonalEvent();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (onlineCountsDirty)
        {
            onlineCountsDirty = false;
            IronforgedClanEventsPanel p = panel;
            if (p != null)
            {
                p.refreshTeamsOnlineCounts();
            }
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        eventTeamService.onChatMessage(event);
    }

    @Subscribe
    public void onScriptCallbackEvent(ScriptCallbackEvent event)
    {
        eventTeamService.onScriptCallbackEvent(event);
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event)
    {
        eventTeamService.onScriptPostFired(event);
    }

    @Subscribe
    public void onClanChannelChanged(ClanChannelChanged event)
    {
        onlineCountsDirty = true;
    }

    @Subscribe
    public void onClanMemberJoined(ClanMemberJoined event)
    {
        onlineCountsDirty = true;
    }

    @Subscribe
    public void onClanMemberLeft(ClanMemberLeft event)
    {
        onlineCountsDirty = true;
    }

    @Subscribe
    public void onFriendsChatSync(FriendsChatSync event)
    {
        onlineCountsDirty = true;
    }

    @Subscribe
    public void onFriendsChatMemberJoined(FriendsChatMemberJoined event)
    {
        onlineCountsDirty = true;
    }

    @Subscribe
    public void onFriendsChatMemberLeft(FriendsChatMemberLeft event)
    {
        onlineCountsDirty = true;
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!event.getGroup().equals(IronforgedClanEventsConfig.CONFIG_GROUP))
        {
            return;
        }

        String key = event.getKey();
        if (key == null)
        {
            return;
        }
        if (key.startsWith("team") && eventTeamSyncService.isApplying())
        {
            // Roster sync writes up to ~31 team* keys at once; syncTeams() rebuilds once afterwards.
            return;
        }

        if (key.startsWith("team")
            || key.equals("enableTeamIcons")
            || key.equals("enableTeamRosterSync")
            || key.equals("friendsListTeamIcons")
            || key.equals("clanListTeamIcons")
            || key.equals("broadcastTeamIcons"))
        {
            eventTeamService.onConfigChanged(key);
            IronforgedClanEventsPanel p = panel;
            if (p != null)
            {
                if ("teamCount".equals(key) || (key.startsWith("team") && key.endsWith("Label")))
                {
                    p.rebuildTeamsUi();
                }
                else if (key.startsWith("team") && key.endsWith("Names"))
                {
                    // Usually typed in the panel itself: rebuilding would destroy the focused text area.
                    p.refreshTeamNames();
                }
                else
                {
                    p.refreshTeamsOnlineCounts();
                    p.updateTeamsSyncLabel();
                }
            }
            if ("enableTeamRosterSync".equals(key))
            {
                if (config.enableTeamRosterSync())
                {
                    // Queued first on the same executor, so the sync sees the event id and passphrase.
                    refreshSeasonalEvent();
                    syncTeams();
                }
                else if (!config.showSeasonalEventPassphrase())
                {
                    seasonalEventService.clear();
                }
            }
        }

        if (key.equals("enableEventsFeed"))
        {
            refreshEvents();
        }

        if (key.equals("showSeasonalEventPassphrase"))
        {
            if (config.showSeasonalEventPassphrase())
            {
                refreshSeasonalEvent();
            }
            else if (!config.enableTeamRosterSync())
            {
                seasonalEventService.clear();
            }
        }

        if (key.equals("showSidebarButton") && navButton != null)
        {
            // Removing a button that was never added is a no-op, so no need to track the state.
            if (config.showSidebarButton())
            {
                clientToolbar.addNavigation(navButton);
            }
            else
            {
                clientToolbar.removeNavigation(navButton);
            }
        }

        if (key.equals("showPastEvents") || key.equals("maxEventsShown"))
        {
            IronforgedClanEventsPanel p = panel;
            if (p != null)
            {
                p.updateEvents();
            }
        }
    }

    @Schedule(period = 1, unit = ChronoUnit.MINUTES)
    public void scheduledRefresh()
    {
        if (!initialized)
        {
            return;
        }

        IronforgedClanEventsPanel p = panel;
        // Nobody sees the panel or overlays from the login screen; the login handler catches up.
        boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;

        if (loggedIn && eventsApiService.needsRefresh(eventsInterval()))
        {
            log.debug("Scheduled refresh triggered");
            refreshEvents();
        }
        else if (p != null)
        {
            p.updateEvents();
        }

        if (!loggedIn)
        {
            return;
        }

        if (seasonalEventNeeded() && seasonalEventService.needsRefresh(rosterInterval()))
        {
            refreshSeasonalEvent();
        }

        if (config.enableTeamRosterSync() && eventTeamSyncService.needsRefresh(rosterInterval()))
        {
            syncTeams();
        }

        if (p != null && config.enableTeamIcons())
        {
            p.refreshTeamsOnlineCounts();
        }
    }

    public void refreshEvents()
    {
        refreshEvents(false);
    }

    /** @param manual the panel's refresh button: also re-checks API health. */
    private void refreshEvents(boolean manual)
    {
        if (!initialized)
        {
            return;
        }

        IronforgedClanEventsPanel p = panel;
        if (!config.enableEventsFeed())
        {
            log.debug("Events feed disabled");
            eventsApiService.clearEvents();
            if (p != null)
            {
                p.updateEvents();
            }
            return;
        }

        // One fetch at a time: the refresh button and the schedule can otherwise overlap.
        if (!eventsFetchInFlight.compareAndSet(false, true))
        {
            return;
        }

        if (p != null)
        {
            p.showLoading();
        }

        executorService.submit(() ->
        {
            try
            {
                boolean ok = eventsApiService.fetchEvents(manual);
                IronforgedClanEventsPanel current = panel;
                if (current == null)
                {
                    return;
                }
                if (!ok && eventsApiService.getCachedEvents().isEmpty())
                {
                    current.showError("Failed to fetch events.<br>Try again later.");
                }
                else
                {
                    current.updateEvents();
                }
            }
            finally
            {
                eventsFetchInFlight.set(false);
            }
        });
    }

    /** The overlay shows the passphrase, and roster sync sends the event id and passphrase. */
    private boolean seasonalEventNeeded()
    {
        return config.showSeasonalEventPassphrase() || config.enableTeamRosterSync();
    }

    private int eventsInterval()
    {
        return Math.max(MIN_EVENTS_INTERVAL, config.refreshInterval());
    }

    private int rosterInterval()
    {
        return Math.max(MIN_ROSTER_INTERVAL, config.seasonalCatalogRefreshInterval());
    }

    /**
     * Fetches GET /plugins/seasonal: the passphrase for the overlay and the event id for roster sync.
     */
    public void refreshSeasonalEvent()
    {
        if (!initialized || !seasonalEventNeeded())
        {
            return;
        }

        executorService.submit(seasonalEventService::refresh);
    }

    public void syncTeams()
    {
        if (!initialized)
        {
            return;
        }

        executorService.submit(() ->
        {
            boolean updated = eventTeamSyncService.syncNow();
            if (updated)
            {
                // Per-key handling was skipped while applying; rebuild the roster once.
                eventTeamService.refreshSurfaces();
            }
            IronforgedClanEventsPanel p = panel;
            if (p != null)
            {
                if (updated)
                {
                    p.rebuildTeamsUi();
                }
                p.updateTeamsSyncLabel();
            }
        });
    }

    private BufferedImage loadPluginIcon()
    {
        try
        {
            BufferedImage image = ImageUtil.loadImageResource(getClass(), ICON_PATH);
            return ImageUtil.resizeImage(image, NAV_ICON_SIZE, NAV_ICON_SIZE);
        }
        catch (IllegalArgumentException e)
        {
            log.debug("Iron Forged icon resource missing, using drawn fallback");
        }

        BufferedImage icon = new BufferedImage(NAV_ICON_SIZE, NAV_ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        int gold = 0xFFFFC800;
        int goldDark = 0xFFB48200;
        int body = 0xFFE6E6E6;
        int ink = 0xFF373737;
        int ring = 0xFF787878;
        int live = 0xFF64DC64;

        for (int x : new int[]{3, 4, 10, 11})
        {
            icon.setRGB(x, 1, ring);
            icon.setRGB(x, 2, ring);
        }

        for (int x = 1; x <= 14; x++)
        {
            icon.setRGB(x, 3, gold);
            icon.setRGB(x, 4, gold);
            icon.setRGB(x, 5, goldDark);
            for (int y = 6; y <= 14; y++)
            {
                icon.setRGB(x, y, body);
            }
        }

        for (int x = 1; x <= 14; x++)
        {
            icon.setRGB(x, 3, ink);
            icon.setRGB(x, 14, ink);
        }
        for (int y = 3; y <= 14; y++)
        {
            icon.setRGB(1, y, ink);
            icon.setRGB(14, y, ink);
        }

        int[][] dots = {
            {3, 7}, {6, 7}, {9, 7}, {12, 7},
            {3, 10}, {6, 10}, {9, 10},
            {3, 13}, {6, 13}
        };
        for (int[] p : dots)
        {
            icon.setRGB(p[0], p[1], ink);
            icon.setRGB(p[0] + 1, p[1], ink);
        }

        for (int x = 9; x <= 12; x++)
        {
            icon.setRGB(x, 10, gold);
            icon.setRGB(x, 11, gold);
        }
        icon.setRGB(10, 11, live);
        icon.setRGB(11, 11, live);

        return icon;
    }

    /**
     * Copies a key from the pre-rename config group when the current group has no value, then unsets the old one.
     */
    static void migrateConfigKey(ConfigManager configManager, String key)
    {
        String legacy = configManager.getConfiguration(LEGACY_CONFIG_GROUP, key);
        if (legacy == null)
        {
            return;
        }
        if (configManager.getConfiguration(IronforgedClanEventsConfig.CONFIG_GROUP, key) == null)
        {
            configManager.setConfiguration(IronforgedClanEventsConfig.CONFIG_GROUP, key, legacy);
            log.debug("Migrated config {}.{}", LEGACY_CONFIG_GROUP, key);
        }
        configManager.unsetConfiguration(LEGACY_CONFIG_GROUP, key);
    }

    @Provides
    IronforgedClanEventsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(IronforgedClanEventsConfig.class);
    }
}
