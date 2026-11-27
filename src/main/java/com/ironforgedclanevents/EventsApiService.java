package com.ironforgedclanevents;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.Callback;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.io.IOException;

/**
 * Fetches clan events from the public Iron Forged plugins API
 * ({@code GET https://api.ironforged.gg/plugins/events}).
 */
@Slf4j
@Singleton
public class EventsApiService
{
    public static final String API_BASE = "https://api.ironforged.gg";

    private final OkHttpClient httpClient;
    private final Gson gson;
    /** Replaced wholesale (never mutated) so the EDT and executor can read it without locking. */
    private volatile List<ClanEvent> cachedEvents = Collections.emptyList();
    private volatile long lastFetchTime = 0;
    /** Last /health answer; reused between polls. Written by one fetch chain at a time. */
    private volatile HealthPayload lastHealth;
    private final RefreshBackoff backoff = new RefreshBackoff();
    private volatile ApiStatus apiStatus = ApiStatus.unknown("Enable Load clan events to check API health");

    @Inject
    public EventsApiService(OkHttpClient httpClient, Gson gson)
    {
        this.httpClient = httpClient;
        this.gson = gson;
    }

    public ApiStatus getApiStatus()
    {
        return apiStatus;
    }

    /**
     * Fetches events from the Iron Forged events API on OkHttp's own threads (never the client thread
     * or RuneLite's shared executor). {@code /health} only colours the status dot, so it is fetched on
     * the first fetch, on a manual refresh, and after a feed failure rather than on every poll.
     * {@code done} gets true if the feed was fetched; on failure the previous events are kept.
     */
    public void fetchEvents(boolean manual, Consumer<Boolean> done)
    {
        if (manual || lastHealth == null)
        {
            fetchHealth(health ->
            {
                lastHealth = health;
                fetchFeed(done);
            });
        }
        else
        {
            fetchFeed(done);
        }
    }

    private void fetchFeed(Consumer<Boolean> done)
    {
        String eventsUrl = API_BASE + "/plugins/events";
        log.debug("Fetching events from: {}", eventsUrl);

        Request request = new Request.Builder()
            .url(eventsUrl)
            .header("User-Agent", "RuneLite-IronforgedClanEvents")
            .header("Accept", "application/json")
            .build();

        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                feedFailed(0, e.getMessage(), done);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                int httpCode = response.code();
                String error = "HTTP " + httpCode;
                try (Response r = response)
                {
                    ResponseBody responseBody = r.body();
                    if (r.isSuccessful() && responseBody != null)
                    {
                        String body = responseBody.string();
                        String feedCache = parseFeedCache(body);
                        List<ClanEvent> events = parseEventsJson(body);
                        cachedEvents = Collections.unmodifiableList(events);
                        long now = System.currentTimeMillis();
                        lastFetchTime = now;
                        backoff.succeeded(now);
                        apiStatus = buildStatus(lastHealth, true, httpCode, feedCache, events.size());
                        log.debug("Fetched {} events", events.size());
                        error = null;
                    }
                }
                catch (Exception e)
                {
                    error = e.getMessage();
                }

                if (error == null)
                {
                    done.accept(true);
                }
                else
                {
                    feedFailed(httpCode, error, done);
                }
            }
        });
    }

    private void feedFailed(int httpCode, String error, Consumer<Boolean> done)
    {
        fetchHealth(health ->
        {
            lastHealth = health;
            apiStatus = buildStatus(health, false, httpCode, null, cachedEvents.size());
            if (backoff.failed(System.currentTimeMillis()))
            {
                log.warn("Failed to fetch events API: {}", error);
            }
            else
            {
                log.debug("Failed to fetch events API again: {}", error);
            }
            done.accept(false);
        });
    }

    private void fetchHealth(Consumer<HealthPayload> done)
    {
        Request request = new Request.Builder()
            .url(API_BASE + "/health")
            .header("User-Agent", "RuneLite-IronforgedClanEvents")
            .header("Accept", "application/json")
            .build();

        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Health check failed: {}", e.getMessage());
                done.accept(HealthPayload.failed(0));
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                HealthPayload health;
                try (Response r = response)
                {
                    JsonObject root = r.isSuccessful() && r.body() != null
                        ? gson.fromJson(r.body().string(), JsonObject.class)
                        : null;
                    health = root == null
                        ? HealthPayload.failed(r.code())
                        : new HealthPayload(true, r.code(), text(root, "status"), text(root, "version"));
                }
                catch (Exception e)
                {
                    log.debug("Health check failed: {}", e.getMessage());
                    health = HealthPayload.failed(0);
                }
                done.accept(health);
            }
        });
    }

    /** The feed's cache marker ("hit" / "miss"), shown in the health tooltip. */
    private String parseFeedCache(String body)
    {
        try
        {
            JsonObject root = gson.fromJson(body, JsonObject.class);
            return root == null ? null : text(root, "cache");
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private ApiStatus buildStatus(
        HealthPayload health,
        boolean eventsOk,
        int eventsHttp,
        String feedCache,
        int eventCount
    )
    {
        String healthBit;
        if (health != null && health.reachable)
        {
            healthBit = health.status.isEmpty() ? "ok" : health.status;
            if (!health.version.isEmpty())
            {
                healthBit += " · v" + health.version;
            }
        }
        else if (health != null && health.httpCode > 0)
        {
            healthBit = "down · HTTP " + health.httpCode;
        }
        else
        {
            healthBit = "unreachable";
        }

        String eventsBit;
        if (eventsOk)
        {
            eventsBit = eventCount + " event" + (eventCount != 1 ? "s" : "");
            if (feedCache != null && !feedCache.isEmpty())
            {
                eventsBit += " · " + feedCache;
            }
        }
        else if (eventsHttp > 0)
        {
            eventsBit = "feed error · HTTP " + eventsHttp;
            if (eventCount > 0)
            {
                eventsBit += " · cached " + eventCount;
            }
        }
        else
        {
            eventsBit = eventCount > 0 ? "feed error · cached " + eventCount : "feed error";
        }

        String tip = "<html>" + escape(healthBit) + "<br>" + escape(eventsBit) + "</html>";

        ApiStatus.Level level;
        if (health != null && health.reachable && eventsOk
            && (health.status.isEmpty() || "healthy".equalsIgnoreCase(health.status)))
        {
            level = ApiStatus.Level.HEALTHY;
        }
        else if ((health != null && health.reachable) || eventsOk || eventCount > 0)
        {
            level = ApiStatus.Level.DEGRADED;
        }
        else
        {
            level = ApiStatus.Level.DOWN;
        }
        return new ApiStatus(level, tip);
    }

    private static String escape(String value)
    {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private static final class HealthPayload
    {
        final boolean reachable;
        final int httpCode;
        final String status;
        final String version;

        HealthPayload(boolean reachable, int httpCode, String status, String version)
        {
            this.reachable = reachable;
            this.httpCode = httpCode;
            this.status = status == null ? "" : status;
            this.version = version == null ? "" : version;
        }

        static HealthPayload failed(int httpCode)
        {
            return new HealthPayload(false, httpCode, "", "");
        }
    }

    List<ClanEvent> parseEventsJson(String body)
    {
        List<ClanEvent> events = new ArrayList<>();
        if (body == null || body.trim().isEmpty())
        {
            return events;
        }

        JsonObject root = gson.fromJson(body, JsonObject.class);
        if (root == null)
        {
            return events;
        }

        JsonArray list = root.has("events") && root.get("events").isJsonArray()
            ? root.getAsJsonArray("events")
            : null;
        if (list == null)
        {
            log.warn("Events API payload missing events array");
            return events;
        }

        for (JsonElement element : list)
        {
            if (!element.isJsonObject())
            {
                continue;
            }
            ClanEvent event = parseEventObject(element.getAsJsonObject());
            if (event != null)
            {
                events.add(event);
            }
        }

        Collections.sort(events);
        return events;
    }

    private ClanEvent parseEventObject(JsonObject row)
    {
        String name = text(row, "name");
        if (name.isEmpty())
        {
            name = text(row, "title");
        }
        if (name.isEmpty())
        {
            return null;
        }

        LocalDateTime start = parseIsoUtc(text(row, "startAt"));
        if (start == null)
        {
            start = parseIsoUtc(text(row, "startTime"));
        }
        if (start == null)
        {
            log.debug("Skipping event without start time: {}", name);
            return null;
        }

        LocalDateTime end = parseIsoUtc(text(row, "endAt"));
        if (end == null)
        {
            end = parseIsoUtc(text(row, "endTime"));
        }

        LocalTime endTime = end != null ? end.toLocalTime() : null;
        String host = text(row, "host");
        if (host.isEmpty())
        {
            host = text(row, "postedBy");
        }

        ClanEvent event = new ClanEvent(
            name,
            text(row, "description"),
            start,
            endTime,
            host,
            text(row, "location"),
            firstNonEmpty(text(row, "url"), text(row, "link")),
            end
        );
        return event;
    }

    private static String text(JsonObject row, String key)
    {
        if (row == null || !row.has(key) || !row.get(key).isJsonPrimitive())
        {
            return "";
        }
        return row.get(key).getAsString().trim();
    }

    private static String firstNonEmpty(String a, String b)
    {
        return a != null && !a.isEmpty() ? a : (b == null ? "" : b);
    }

    private static LocalDateTime parseIsoUtc(String raw)
    {
        if (raw == null || raw.isEmpty())
        {
            return null;
        }
        try
        {
            Instant instant = Instant.parse(raw);
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        }
        catch (DateTimeParseException ignored)
        {
            // fall through
        }
        try
        {
            return LocalDateTime.parse(raw);
        }
        catch (DateTimeParseException e)
        {
            log.debug("Could not parse timestamp: {}", raw);
            return null;
        }
    }

    public List<ClanEvent> getCachedEvents()
    {
        return new ArrayList<>(cachedEvents);
    }

    public void clearEvents()
    {
        cachedEvents = Collections.emptyList();
        lastFetchTime = 0;
        lastHealth = null;
        backoff.reset();
        apiStatus = ApiStatus.unknown("Enable Load clan events to check API health");
        log.debug("Cleared all cached events");
    }

    public ClanEvent getNextEvent()
    {
        return cachedEvents.stream()
            .filter(ClanEvent::isUpcoming)
            .sorted()
            .findFirst()
            .orElse(null);
    }

    public ClanEvent getActiveEvent()
    {
        return cachedEvents.stream()
            .filter(ClanEvent::isHappeningNow)
            .sorted()
            .findFirst()
            .orElse(null);
    }

    public ClanEvent getHighlightedEvent()
    {
        ClanEvent active = getActiveEvent();
        return active != null ? active : getNextEvent();
    }

    public List<ClanEvent> getUpcomingEvents()
    {
        List<ClanEvent> upcoming = new ArrayList<>();
        for (ClanEvent event : cachedEvents)
        {
            if (event.isUpcoming() || event.isHappeningNow() || (event.isWeeklySpotlight() && !event.isPast()))
            {
                upcoming.add(event);
            }
        }
        Collections.sort(upcoming);
        return upcoming;
    }

    public boolean needsRefresh(int refreshIntervalMinutes)
    {
        return backoff.isDue(refreshIntervalMinutes, System.currentTimeMillis());
    }

    public long getLastFetchTime()
    {
        return lastFetchTime;
    }
}
