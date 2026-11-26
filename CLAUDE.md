# Ironforged Clan Events (ironforged-clan-events)

RuneLite plugin (`com.ironforgedclanevents`). Hub checklist lives in `AGENTS.md` and `.cursor/rules/plugin-hub.mdc` — read those before editing.

- Java 11. OkHttp + injected Gson. No client-thread IO. No reflection / ProcessBuilder.
- Third-party HTTP (yume-api only, at https://api.ironforged.gg) must stay opt-in with the Hub IP warning.
- Do not automate RuneScape input. Offer `./gradlew.bat runClient`, tell the user what to test, wait.
- Do not tidy `plugin/plugin-hub`. Do not edit ironforged-pages / yume-api in the same sitting.
