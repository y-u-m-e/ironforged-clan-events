package com.ironforgedclanevents;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Opt-in sync of event team rosters from {@code GET /plugins/seasonal/teams}.
 * Overwrites local team count / names / colors when a payload is accepted.
 */
@Slf4j
@Singleton
public class EventTeamSyncService
{
    /** Contract caps the payload at 16 teams; slots past MAX_TEAMS are dropped when applied. */
    private static final int MAX_API_TEAMS = 16;

    private final IronforgedClanEventsConfig config;
    private final ConfigManager configManager;
    private final OkHttpClient httpClient;
    private final Gson gson;

    private final RefreshBackoff backoff = new RefreshBackoff();
    private volatile String lastStatus = "Team sync idle";
    /** From the last successful sync; empty until one succeeds. */
    private volatile String eventName = "";
    private volatile String rosterUpdatedAt = "";
    /** True while applyTeams writes config; the plugin skips per-key team handling meanwhile. */
    private volatile boolean applying;

    @Inject
    EventTeamSyncService(
        IronforgedClanEventsConfig config,
        ConfigManager configManager,
        OkHttpClient httpClient,
        Gson gson
    )
    {
        this.config = config;
        this.configManager = configManager;
        this.httpClient = httpClient;
        this.gson = gson;
    }

    public String getLastStatus()
    {
        return lastStatus;
    }

    public String getEventName()
    {
        return eventName;
    }

    /** ISO-8601 instant the roster was last published, or empty. */
    public String getRosterUpdatedAt()
    {
        return rosterUpdatedAt;
    }

    public boolean isApplying()
    {
        return applying;
    }

    /** Forget the last fetch so a restarted plugin syncs immediately. */
    public void reset()
    {
        backoff.reset();
        lastStatus = "Team sync idle";
        eventName = "";
        rosterUpdatedAt = "";
    }

    public boolean needsRefresh(int intervalMinutes)
    {
        if (!config.enableTeamRosterSync())
        {
            return false;
        }
        return backoff.isDue(intervalMinutes, System.currentTimeMillis());
    }

    /**
     * Fetch and apply the team roster. The request runs on OkHttp's own threads, never the client
     * thread or RuneLite's shared executor. {@code done} gets true if local config was updated.
     */
    public void syncNow(Consumer<Boolean> done)
    {
        Request request = buildRequest();
        if (request == null)
        {
            done.accept(false);
            return;
        }
        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                fail(e);
                done.accept(false);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                boolean updated;
                try (Response r = response)
                {
                    updated = handleResponse(r);
                }
                catch (Exception e)
                {
                    fail(e);
                    updated = false;
                }
                done.accept(updated);
            }
        });
    }

    private void fail(Exception e)
    {
        lastStatus = "Team sync failed: " + (e.getMessage() == null ? "error" : e.getMessage());
        logFailure();
    }

    /** The roster request, or null when sync is off or not set up (lastStatus says why). */
    private Request buildRequest()
    {
        if (!config.enableTeamRosterSync())
        {
            lastStatus = "Team sync disabled";
            return null;
        }

        // The typed event id is the key: the API returns an empty roster without it, so don't ask.
        String eventId = config.seasonalEventId() == null ? "" : config.seasonalEventId().trim();
        if (eventId.isEmpty())
        {
            lastStatus = "Enter the Seasonal Event ID to sync teams";
            return null;
        }
        String base = EventsApiService.API_BASE + "/plugins/seasonal/teams";
        HttpUrl url = HttpUrl.parse(base);
        if (url == null)
        {
            lastStatus = "Team sync failed: bad url";
            return null;
        }
        HttpUrl.Builder urlBuilder = url.newBuilder().addQueryParameter("event_id", eventId);

        return new Request.Builder()
            .url(urlBuilder.build())
            .header("User-Agent", "RuneLite-IronforgedClanEvents")
            .header("Accept", "application/json")
            .get()
            .build();
    }

    /** Parses and applies one roster response. Synchronized so two responses never apply at once. */
    private synchronized boolean handleResponse(Response response) throws IOException
    {
        if (!response.isSuccessful() || response.body() == null)
        {
            lastStatus = "Team sync failed: HTTP " + response.code();
            logFailure();
            return false;
        }
        // A response arrived; payload problems below are not retried faster than the interval.
        backoff.succeeded(System.currentTimeMillis());

        JsonObject root = gson.fromJson(response.body().string(), JsonObject.class);
        if (root == null)
        {
            lastStatus = "Team sync failed: empty payload";
            return false;
        }

        JsonArray teams = root.has("teams") && root.get("teams").isJsonArray()
            ? root.getAsJsonArray("teams")
            : null;
        if (teams == null || teams.size() == 0)
        {
            lastStatus = "Team sync: API returned no teams";
            return false;
        }

        List<SyncedTeam> syncedTeams = new ArrayList<>();
        for (JsonElement el : teams)
        {
            if (!el.isJsonObject())
            {
                continue;
            }
            SyncedTeam team = parseTeam(el.getAsJsonObject(), syncedTeams.size() + 1);
            if (team != null)
            {
                syncedTeams.add(team);
            }
            if (syncedTeams.size() >= MAX_API_TEAMS)
            {
                break;
            }
        }

        if (syncedTeams.isEmpty())
        {
            lastStatus = "Team sync failed: could not parse teams";
            return false;
        }

        int applied;
        applying = true;
        try
        {
            applied = applyTeams(syncedTeams);
        }
        finally
        {
            applying = false;
        }
        if (applied == 0)
        {
            lastStatus = "Team sync failed: no teams in slots 1-" + EventTeamService.MAX_TEAMS;
            log.warn("Team roster sync returned {} team(s), none in range", syncedTeams.size());
            return false;
        }

        eventName = text(root, "event_name");
        rosterUpdatedAt = text(root, "updated_at");
        String apiEvent = text(root, "event_id");
        lastStatus = syncedLabel(applied, syncedTeams)
            + (apiEvent.isEmpty() ? "" : " · " + apiEvent);
        log.debug("Team roster sync applied {} teams", applied);
        return true;
    }

    private void logFailure()
    {
        if (backoff.failed(System.currentTimeMillis()))
        {
            log.warn("{}", lastStatus);
        }
        else
        {
            log.debug("{}", lastStatus);
        }
    }

    /** Says so when the API sent teams in slots the plugin cannot show. */
    static String syncedLabel(int applied, List<SyncedTeam> teams)
    {
        long aboveMax = teams.stream().filter(t -> t.slot > EventTeamService.MAX_TEAMS).count();
        if (aboveMax == 0)
        {
            return "Synced " + applied + " team(s)";
        }
        return "Synced " + applied + " of " + teams.size() + " teams (slots above "
            + EventTeamService.MAX_TEAMS + " ignored)";
    }

    /**
     * Writes each team into the slot the API assigned it, so badge color and label
     * follow the roster rather than payload ordering. parseTeam already falls back
     * to array order when the API omits {@code slot}.
     *
     * @return how many teams were placed
     */
    private int applyTeams(List<SyncedTeam> teams)
    {
        Map<Integer, SyncedTeam> bySlot = mapBySlot(teams);

        if (bySlot.isEmpty())
        {
            // Nothing usable — leave the local roster alone rather than wiping it.
            return 0;
        }

        // Highest occupied slot, not team count: slots may be sparse.
        configManager.setConfiguration(
            IronforgedClanEventsConfig.CONFIG_GROUP, "teamCount", Collections.max(bySlot.keySet()));

        for (int slot = 1; slot <= EventTeamService.MAX_TEAMS; slot++)
        {
            SyncedTeam team = bySlot.get(slot);
            if (team == null)
            {
                // Clear unoccupied slots so stale RSNs do not linger.
                configManager.unsetConfiguration(IronforgedClanEventsConfig.CONFIG_GROUP, EventTeamService.teamNamesKey(slot));
                configManager.unsetConfiguration(IronforgedClanEventsConfig.CONFIG_GROUP, EventTeamService.teamLabelKey(slot));
                continue;
            }

            configManager.setConfiguration(
                IronforgedClanEventsConfig.CONFIG_GROUP,
                EventTeamService.teamNamesKey(slot),
                String.join("\n", team.members));
            if (team.colorKey != null && !team.colorKey.isEmpty())
            {
                configManager.setConfiguration(
                    IronforgedClanEventsConfig.CONFIG_GROUP,
                    EventTeamService.teamColorKey(slot),
                    team.colorKey);
            }
            if (team.name != null && !team.name.isEmpty())
            {
                configManager.setConfiguration(
                    IronforgedClanEventsConfig.CONFIG_GROUP,
                    EventTeamService.teamLabelKey(slot),
                    team.name);
            }
        }

        return bySlot.size();
    }

    /**
     * Keys teams by their assigned slot. Slots outside 1..MAX_TEAMS are dropped,
     * and on a duplicate slot the first team wins (same rule as duplicate RSNs).
     */
    static Map<Integer, SyncedTeam> mapBySlot(List<SyncedTeam> teams)
    {
        Map<Integer, SyncedTeam> bySlot = new LinkedHashMap<>();
        for (SyncedTeam team : teams)
        {
            if (team.slot < 1 || team.slot > EventTeamService.MAX_TEAMS)
            {
                log.debug("Dropping team '{}': slot {} outside 1-{}",
                    team.name, team.slot, EventTeamService.MAX_TEAMS);
                continue;
            }
            SyncedTeam existing = bySlot.putIfAbsent(team.slot, team);
            if (existing != null)
            {
                log.debug("Dropping team '{}': slot {} already taken by '{}'",
                    team.name, team.slot, existing.name);
            }
        }
        return bySlot;
    }

    private static SyncedTeam parseTeam(JsonObject obj, int fallbackSlot)
    {
        SyncedTeam team = new SyncedTeam();
        team.slot = slotOrFallback(obj, fallbackSlot);
        team.name = text(obj, "name");
        if (team.name.isEmpty())
        {
            team.name = text(obj, "label");
        }
        String color = text(obj, "color");
        EventTeamIconFactory.TeamPalette palette = EventTeamIconFactory.TeamPalette.fromKey(color);
        if (palette == null && color.matches("(?i)^#[0-9A-F]{6}$"))
        {
            try
            {
                palette = EventTeamIconFactory.TeamPalette.nearest(java.awt.Color.decode(color));
            }
            catch (NumberFormatException ignored)
            {
                palette = null;
            }
        }
        if (palette != null)
        {
            team.colorKey = palette.getKey();
        }

        JsonArray members = null;
        if (obj.has("members") && obj.get("members").isJsonArray())
        {
            members = obj.getAsJsonArray("members");
        }
        else if (obj.has("players") && obj.get("players").isJsonArray())
        {
            members = obj.getAsJsonArray("players");
        }
        if (members == null)
        {
            return null;
        }

        for (JsonElement m : members)
        {
            String name;
            if (m.isJsonPrimitive())
            {
                name = m.getAsString();
            }
            else if (m.isJsonObject())
            {
                name = text(m.getAsJsonObject(), "rsn");
                if (name.isEmpty())
                {
                    name = text(m.getAsJsonObject(), "name");
                }
            }
            else
            {
                continue;
            }
            name = name == null ? "" : name.trim();
            if (!name.isEmpty())
            {
                team.members.add(name);
            }
        }
        return team;
    }

    /**
     * Contract slot is 1..16. Anything missing, non-numeric or out of range falls
     * back to array order rather than failing the whole sync.
     */
    private static int slotOrFallback(JsonObject obj, int fallbackSlot)
    {
        if (obj == null || !obj.has("slot") || !obj.get("slot").isJsonPrimitive())
        {
            return fallbackSlot;
        }
        try
        {
            int slot = obj.get("slot").getAsInt();
            return slot >= 1 && slot <= MAX_API_TEAMS ? slot : fallbackSlot;
        }
        catch (NumberFormatException e)
        {
            return fallbackSlot;
        }
    }

    private static String text(JsonObject obj, String key)
    {
        if (obj == null || !obj.has(key) || !obj.get(key).isJsonPrimitive())
        {
            return "";
        }
        return obj.get(key).getAsString().trim();
    }

    static final class SyncedTeam
    {
        int slot;
        String name = "";
        String colorKey = "";
        final List<String> members = new ArrayList<>();
    }
}
