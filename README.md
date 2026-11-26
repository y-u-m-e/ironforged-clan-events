# Ironforged Clan Events

A RuneLite plugin for **Iron Forged** clan events: live calendar from `api.ironforged.gg`, side panel timeline, your seasonal team, optional Seasonal Event Passphrase overlay, and event team badges.

---

## Features

- **Events API** — Fetches from `https://api.ironforged.gg/plugins/events` (sesh-backed, opt-in with RuneLite’s third-party warning)
- **Side panel timeline** — Day-grouped events with live / today / future / past color tones
- **Seasonal Event Passphrase** — Optional on-screen passphrase (and optional local date/time), movable and resizable. Prefers the API value; config is fallback.
- **Your team** — The Seasonal tab shows the event, your team and its badge, and which teammates are online
- **Event teams** — Paste RSNs per team in the Seasonal tab, or opt in to **Sync team roster from API** (`GET /plugins/seasonal/teams`). Rostered players get their team's clan icon in chat, PMs, broadcasts, friends list and clan list
- **Local times** — API timestamps are UTC and shown in your local timezone

---

## Setup

1. Open RuneLite settings → **Ironforged Clan Events**
2. Enable **Load clan events** (accept the third-party warning)
3. Optional: set **Seasonal Event ID** and **Seasonal Event Passphrase** under **Seasonal Event**
4. Optional: under **Teams**, enable **Sync team roster from API**

Three settings contact `https://api.ironforged.gg`, and each sends your IP address to it: **Load clan events**, **Show Seasonal Event Passphrase** and **Sync team roster from API**. All three are off by default.

---

## Seasonal Event

Under **Seasonal Event** in settings:

- Enter **Seasonal Event ID** (which event team roster sync asks for)
- Enter **Seasonal Event Passphrase** (overlay text)
- Toggle **Show Seasonal Event Passphrase** on the game screen
- Optionally show local date & time
- Drag to reposition; resize freely but not smaller than the text

Config values are used when set; otherwise the plugin falls back to values from the API.

---

## Development

See [AGENTS.md](./AGENTS.md) for RuneLite Plugin Hub rules this project follows.

Cross-repo team roster contract for the webpage/API team:  
[`docs/plans/PLUGIN_TEAM_ROSTER_SYNC.md`](../../docs/plans/PLUGIN_TEAM_ROSTER_SYNC.md) (workspace).

```bash
./gradlew.bat runClient
# or
./gradlew.bat test
```

Use a Jagex account login for the dev client:  
https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts

---

## Credits

- **[Bingo Team Icons](https://github.com/gwigl/bingo-team-icons)** by gwig (BSD-2-Clause): the
  idea and approach for the event team badges, coloured clan icons next to rostered players in
  chat, on the friends and clan lists, and above their heads.
- **[Friend Notes](https://github.com/runelite/runelite/tree/master/runelite-client/src/main/java/net/runelite/client/plugins/friendnotes)**,
  part of RuneLite (BSD-2-Clause): the friends and clan list icon layout, a 14×12 icon after the
  name with the row shifted to make room.
- **[Clan Events](https://github.com/cmsu224/clan-events)** by Maz, the Elysium clan's plugin
  (BSD-2-Clause): the on-screen event passphrase box.
- Clan icon sprites are Old School RuneScape assets, taken from the
  [Old School RuneScape Wiki](https://oldschool.runescape.wiki/).

---

## License

BSD 2-Clause. See [LICENSE](./LICENSE).
