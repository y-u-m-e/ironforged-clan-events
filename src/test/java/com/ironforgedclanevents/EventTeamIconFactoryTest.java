package com.ironforgedclanevents;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import org.junit.Test;

/**
 * Friends-list badge: the chat-size badge inside the 14×12 slot, with a gap after the name and a
 * clear bottom row so it sits on the text the way the chat badge does.
 */
public class EventTeamIconFactoryTest
{
    private static boolean columnClear(BufferedImage img, int x)
    {
        for (int y = 0; y < img.getHeight(); y++)
        {
            if ((img.getRGB(x, y) >>> 24) != 0)
            {
                return false;
            }
        }
        return true;
    }

    private static boolean rowClear(BufferedImage img, int y)
    {
        for (int x = 0; x < img.getWidth(); x++)
        {
            if ((img.getRGB(x, y) >>> 24) != 0)
            {
                return false;
            }
        }
        return true;
    }

    @Test
    public void friendsBadgeKeepsTheSlotSize()
    {
        BufferedImage img = EventTeamIconFactory.loadFriendsListBadge(EventTeamIconFactory.TeamPalette.RED);
        assertEquals(EventTeamIconFactory.FRIENDS_ICON_WIDTH, img.getWidth());
        assertEquals(EventTeamIconFactory.FRIENDS_ICON_HEIGHT, img.getHeight());
    }

    @Test
    public void friendsBadgeLeavesAGapAfterTheName()
    {
        BufferedImage img = EventTeamIconFactory.loadFriendsListBadge(EventTeamIconFactory.TeamPalette.RED);
        assertEquals(true, columnClear(img, 0));
        assertEquals(true, columnClear(img, 1));
    }

    @Test
    public void friendsBadgeSitsOneRowAboveTheBaseline()
    {
        BufferedImage img = EventTeamIconFactory.loadFriendsListBadge(EventTeamIconFactory.TeamPalette.RED);
        assertEquals(true, rowClear(img, img.getHeight() - 1));
    }

    @Test
    public void friendsBadgeIsNoTallerThanTheChatBadge()
    {
        BufferedImage img = EventTeamIconFactory.loadFriendsListBadge(EventTeamIconFactory.TeamPalette.RED);
        int top = 0;
        while (top < img.getHeight() && rowClear(img, top))
        {
            top++;
        }
        int bottom = img.getHeight() - 1;
        while (bottom > top && rowClear(img, bottom))
        {
            bottom--;
        }
        assertEquals(true, bottom - top + 1 <= EventTeamIconFactory.BADGE_SIZE);
    }
}
