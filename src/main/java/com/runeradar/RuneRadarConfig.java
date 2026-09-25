package com.runeradar;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("runeradar")
public interface RuneRadarConfig extends Config
{
    @ConfigItem(
            keyName = "cashStack",
            name = "Cash stack",
            description = "How much GP RuneRadar may use when finding flips. Set to 0 to show all opportunities.",
            position = 0
    )
    @Range(
            min = 0,
            max = 2147483647
    )
    default int cashStack()
    {
        return 25_000_000;
    }
}
