package com.ironforgedclanevents;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Event colour comes from the name. Names from the live feed on 2026-11-26.
 */
public class EventCategoryTest
{
    @Test
    public void classifiesTheLiveFeed()
    {
        assertEquals(EventCategory.SKILLING, EventCategory.of("SOTW Fishing"));
        assertEquals(EventCategory.SKILLING, EventCategory.of("Zeah Runecrafting"));
        assertEquals(EventCategory.SKILLING, EventCategory.of("Marlin Trawling"));
        assertEquals(EventCategory.SKILLING, EventCategory.of("Motherlode Mine"));
        assertEquals(EventCategory.SKILLING, EventCategory.of("SOTW - Herblore"));
        assertEquals(EventCategory.WILDY, EventCategory.of("Wildy Wednesday"));
        assertEquals(EventCategory.MINIGAME, EventCategory.of("Tempoross mass"));
        assertEquals(EventCategory.MINIGAME, EventCategory.of("Barbarian Assault"));
        assertEquals(EventCategory.MINIGAME, EventCategory.of("Burthrope Games Room"));
        assertEquals(EventCategory.PVM, EventCategory.of("Nex Mass"));
        assertEquals(EventCategory.PVM, EventCategory.of("BOTW Zulrah"));
        assertEquals(EventCategory.PVM, EventCategory.of("Zulrah Powerhour"));
    }

    @Test
    public void unknownAndMissingNamesAreOther()
    {
        assertEquals(EventCategory.OTHER, EventCategory.of("Clan meeting"));
        assertEquals(EventCategory.OTHER, EventCategory.of(""));
        assertEquals(EventCategory.OTHER, EventCategory.of(null));
    }
}
