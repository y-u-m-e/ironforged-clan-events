package com.ironforgedclanevents;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class IronforgedClanEventsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(IronforgedClanEventsPlugin.class);
		RuneLite.main(args);
	}
}

