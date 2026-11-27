package com.ironforgedclanevents;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Event colour comes from the name. Cases are real event names from the clan's attendance
 * archive and the live feed (2026-11-26), typos and mixed events included.
 */
public class EventCategoryTest
{
    private static void check(EventCategory expected, String... names)
    {
        for (String name : names)
        {
            assertEquals(name, expected, EventCategory.of(name));
        }
    }

    @Test
    public void skilling()
    {
        check(EventCategory.SKILLING,
            "SOTW Fishing", "SOTW - Herblore", "Zeah Runecrafting", "Marlin Trawling", "Motherlode Mine",
            "MLM / Bank stand", "Karbambams", "Kawambam fishing", "Vale Totes", "Blast Furnace (Mass)",
            "Mass Astral Projection!", "Rellekka Rooftops", "Stealing valuables", "ZMI Altar",
            "Hunter rumours/bank standing", "Sailing Prep: Seer's Forestry/Bankstanding",
            "Blessed bone shard prayer", "Mercenary Salvaging (Salvor's Paint) Event");
    }

    @Test
    public void pvm()
    {
        check(EventCategory.PVM,
            "Nex Mass", "BOTW Zulrah", "Zulrah Powerhour", "ToA Thrusday", "Throbbin Tobbin", "Huey/Bankstand",
            "Hueycoatl", "Dooooooooooom", "'Doom'sday Wednesday", "Phanto Muspha", "CG Power Hour2",
            "Red Prison Visitation Hours (CG)", "Calisto Mass", "Did Someone Say, Beeaarr?", "Moons of Peril",
            "Royal Titans", "Maggot king", "Levi Ethan");
    }

    @Test
    public void minigames()
    {
        check(EventCategory.MINIGAME,
            "Tempoross mass", "Tempeross Mass", "Temp Sunday", "Zalcano Mass", "Zacano mass", "Wintertoad",
            "Tatertodt", "Barbarian Assault", "Barb assult", "BA", "Burthrope Games Room", "Burthorpe Game Night",
            "GOATR 2026", "Ruardians of the Rift", "Mastering Mxiology", "Giants Foundy", "Pyrami Plunder",
            "Hallowed Sepulchre/Bankstanding", "SUNDAY NIGHT FIGHT NIGHT!", "Gwenith Glide");
    }

    @Test
    public void wildy()
    {
        check(EventCategory.WILDY, "Wildy Wednesday", "Wildy Wednesday (Mass Agility)", "Wildy Alt Saturday");
    }

    @Test
    public void social()
    {
        check(EventCategory.SOCIAL,
            "Clan Hall Bankstanding", "Seers' Bankin!", "Seers' Village Vibes", "PRE SAILING CLAN PHOTO",
            "Akicha 99 Sailing / Re-Maxing Party!", "Jeopardy #4", "OSRS Skribblio", "Beetleback says (EU)",
            "Friday the 13th Hide N Seek", "Hide and Go Beach", "2/3 3rd Clan Anniversary Event",
            "Iron Forged Games - The Race");
    }

    @Test
    public void unknownAndMissingNamesAreOther()
    {
        check(EventCategory.OTHER, "Unknown Event", "Easy Clue Event", "Daily Chores", "", null);
    }
}
