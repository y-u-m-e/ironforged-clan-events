package com.ironforgedclanevents;

import java.awt.Color;
import java.util.regex.Pattern;

/**
 * Event type guessed from the event name, used to colour upcoming events. The API has no
 * category field. Word lists come from the clan's attendance archive (~285 event names,
 * typos included), so stems are loose on purpose.
 *
 * <p>Order matters: a clear social event ("party", "photo") wins first; then categories are
 * tried top to bottom, so "Tempoross mass" is a minigame, "Blast Furnace (Mass)" is skilling,
 * and "Huey/Bankstand" is PvM rather than social.
 */
enum EventCategory
{
    WILDY(new Color(214, 96, 40),
        "wild(y|erness)|pk\\w*|pvp"),
    MINIGAME(new Color(168, 110, 230),
        "temp\\w*|wintert\\w*|\\w*todt|zalc\\w*|zolc\\w*|zacano|barb\\w* ass\\w*|ba|games? (room|night)"
            + "|castle ?wars|soul wars|pest control|lms|last man standing|fight night|minigames?"
            + "|guardians of the rift|ruardians\\w*|go?a?tr|trouble brewing|mahogany|clan wars|fight pit"
            + "|giants?'? ?found\\w*|foundry|m\\w*ology|pyrami\\w*|plunder|sepulchre|barracuda trials"
            + "|gwenith glide"),
    SKILLING(new Color(40, 190, 170),
        "sotw|skill\\w*|xp|fish\\w*|runecraft\\w*|rc|zmi|astral|min(e|es|ing)|mlm|motherlo\\w*|daeyalt"
            + "|essence|calcified|trawl\\w*|woodcut\\w*|wc|chop\\w*|deforest\\w*|forestry|bloodwood"
            + "|agility|rooftops?|parkour|brimhaven|thiev\\w*|steal\\w*|stalls?|wealthy citizens|knights"
            + "|master farmers|rogues'? chests?|crystal chest|hunt\\w*|herbi\\w*|farm\\w*|cook\\w*|kitchen"
            + "|smith\\w*|blast furnace|cannon ?balls?|nail making|craft\\w*|fletch\\w*|vale tot\\w*|sawmill"
            + "|firemak\\w*|herb\\w*|construct\\w*|prayer|bone shards?|karam\\w*|karb\\w*|karmb\\w*"
            + "|kawam\\w*|bwan|sailing|salvag\\w*|salvor\\w*|port roberts"),
    PVM(new Color(220, 72, 72),
        "botw|mass|raids?|cox|tob|tobbin|toa|chambers|theatre|tombs|nex|zulrah|snake|vork\\w*|boss\\w*"
            + "|gwd|god ?wars|corp\\w*|nightmare|inferno|colosseum|pvm|slayer|jad|zuk|hydra|cerberus"
            + "|barrows|kq|dks|hue\\w*|moons of peril|'?do+m\\w*|royal titans|ph?ant\\w* mus\\w*"
            + "|callisto|calisto|be+a+r+\\w*|cg|gauntlet|red prison|scurrius|amoxliatl|yama|maggot king"
            + "|zombie pirates?|chaos druids?|shades of mor'?ton|levi\\w*|gem crab"),
    SOCIAL(new Color(226, 104, 168),
        "bank\\w*|clan hall|chill|vibes|movie|hide (n|and|'n) \\w+|hide and go \\w+"),
    OTHER(new Color(70, 168, 236), null);

    /** Unmistakably social words that beat every activity word in the same name. */
    static final String SOCIAL_FIRST =
        "party|parties|photo|anniversary|summit|jeopardy|skribbl\\w*|trivia|says|halloween"
            + "|friday the 13th|iron forged games";

    private static final Pattern SOCIAL_FIRST_PATTERN = compile(SOCIAL_FIRST);

    final Color color;
    private final Pattern pattern;

    EventCategory(Color color, String words)
    {
        this.color = color;
        this.pattern = words == null ? null : compile(words);
    }

    private static Pattern compile(String words)
    {
        return Pattern.compile("\\b(" + words + ")\\b", Pattern.CASE_INSENSITIVE);
    }

    static EventCategory of(String name)
    {
        if (name == null)
        {
            return OTHER;
        }
        if (SOCIAL_FIRST_PATTERN.matcher(name).find())
        {
            return SOCIAL;
        }
        for (EventCategory c : values())
        {
            if (c.pattern != null && c.pattern.matcher(name).find())
            {
                return c;
            }
        }
        return OTHER;
    }
}
