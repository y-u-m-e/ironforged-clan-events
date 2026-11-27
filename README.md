# Ironforged Clan Events

A RuneLite plugin for **Iron Forged** clan events: the clan calendar in a side panel, a next-event box on the game screen, your seasonal team, an optional event passphrase box, and team badges for the seasonal event.

Formerly **Clan Event List**. If you had that installed, it updates in place; your settings carry over.

---

## Features

- **Events panel**: the clan calendar grouped by day, in your local time. Upcoming events are coloured by type (PvM, skilling, minigame, wildy, social). Click an event for a details window with a countdown, host, location, description and link.
- **Next event box**: the next event and the one running now, on the game screen. **Verbose** shows title, name and countdown on separate lines; **Compact** fits each event on one line. Skill and Boss of the Week can be left out. Drag to move, resize to wrap.
- **Your team**: the Seasonal tab shows the event, your team and its badge, and which teammates are online.
- **Team badges**: rostered players get their team's coloured clan icon in chat, private messages, broadcasts, and the friends and clan lists. Paste names per team in the Seasonal tab, or turn on **Sync team roster** and press **Sync teams** to fetch the roster staff publish.
- **Event passphrase box**: shows the passphrase you type in settings, with an optional date and time, so it appears in screenshots for event submissions.

---

## Setup

1. Open RuneLite settings, then **Ironforged Clan Events**.
2. Under **Iron Forged API**, turn on **Load clan events** and accept the warning.
3. Optional: in the same section, turn on **Sync team roster** and enter the **Seasonal Event ID** staff announce.

Only the **Iron Forged API** section contacts `https://api.ironforged.gg`. Its two switches, **Load clan events** and **Sync team roster**, send your IP address to it, and both are off by default. Nothing else in the plugin uses the network.

---

## Overlays

Under **Overlays** in settings:

- **Show next event overlay**, **Next event style** (Compact or Verbose) and **Show SOTW/BOTW**. Needs **Load clan events**.
- **Show event passphrase** and the **Seasonal Event Passphrase** text. Nothing is fetched. Optional local date and time, and colours for each.

Drag either box to move it; resize it and the text wraps.

---

## Development

See [AGENTS.md](./AGENTS.md) for the RuneLite Plugin Hub rules this project follows.

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
  chat and on the friends and clan lists.
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
