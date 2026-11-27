# Ironforged Clan Events

A RuneLite plugin for **Iron Forged** clan events: live calendar from `api.ironforged.gg`, side panel timeline, your seasonal team, optional Seasonal Event Passphrase overlay, and event team badges.

---

## Features

- **Events API** — Fetches from `https://api.ironforged.gg/plugins/events` (sesh-backed, opt-in with RuneLite’s third-party warning)
- **Side panel timeline** — Day-grouped events with live / today / future / past color tones
- **Seasonal Event Passphrase** — Optional on-screen passphrase you type in settings (and optional local date/time), movable and resizable.
- **Your team** — The Seasonal tab shows the event, your team and its badge, and which teammates are online
- **Event teams** — Paste RSNs per team in the Seasonal tab, or opt in to **Sync team roster** (`GET /plugins/seasonal/teams`, needs the Seasonal Event ID). Rostered players get their team's clan icon in chat, PMs, broadcasts, friends list and clan list
- **Local times** — API timestamps are UTC and shown in your local timezone

---

## Setup

1. Open RuneLite settings → **Ironforged Clan Events**
2. Enable **Load clan events** (accept the third-party warning)
3. Optional: in the same **Iron Forged API** section, enable **Sync team roster** and enter the **Seasonal Event ID**

Only the **Iron Forged API** section contacts `https://api.ironforged.gg`. Two settings there send your IP address to it, **Load clan events** and **Sync team roster**, and both are off by default.

---

## Seasonal Event

Under **Overlays** in settings:

- Toggle **Show event passphrase** on the game screen
- Enter **Seasonal Event Passphrase** (the overlay text; nothing is fetched)
- Optionally show local date & time
- Drag to reposition; resize freely but not smaller than the text

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
