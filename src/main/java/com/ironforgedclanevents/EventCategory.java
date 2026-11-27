package com.ironforgedclanevents;

import java.awt.Color;
import java.util.regex.Pattern;

/**
 * Event type guessed from the event name, used to colour upcoming events.
 * The API has no category field; checked in order, first match wins
 * (so "Tempoross mass" is a minigame, not PvM).
 */
enum EventCategory
{
    WILDY(new Color(214, 96, 40), "wild(y|erness)|pk\\w*|pvp"),
    MINIGAME(new Color(168, 110, 230),
        "tempoross|wintertodt|barbarian assault|ba|games room|castle wars|cw|soul wars|pest control|pc"
            + "|lms|last man standing|minigames?|guardians of the rift|gotr|trouble brewing|mahogany homes"
            + "|clan wars|fight pit|trivia|hide and seek|drop party"),
    PVM(new Color(220, 72, 72),
        "botw|mass|raids?|cox|tob|toa|chambers|theatre|tombs|nex|zulrah|vorkath|boss\\w*|gwd|god ?wars"
            + "|corp\\w*|nightmare|inferno|colosseum|pvm|slayer|jad|zuk|hydra|cerberus|barrows|kq|dks"),
    SKILLING(new Color(40, 190, 170),
        "sotw|skill\\w*|xp|fish\\w*|runecraft\\w*|rc|min(e|es|ing)|motherlode|trawl\\w*|woodcut\\w*|wc"
            + "|agility|thiev\\w*|hunt\\w*|farm\\w*|cook\\w*|smith\\w*|craft\\w*|fletch\\w*|firemak\\w*"
            + "|herb\\w*|construct\\w*"),
    OTHER(new Color(70, 168, 236), null);

    final Color color;
    private final Pattern pattern;

    EventCategory(Color color, String words)
    {
        this.color = color;
        this.pattern = words == null ? null : Pattern.compile("\\b(" + words + ")\\b", Pattern.CASE_INSENSITIVE);
    }

    static EventCategory of(String name)
    {
        if (name != null)
        {
            for (EventCategory c : values())
            {
                if (c.pattern != null && c.pattern.matcher(name).find())
                {
                    return c;
                }
            }
        }
        return OTHER;
    }
}
