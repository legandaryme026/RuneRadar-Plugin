package com.runeradar;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@Slf4j
@PluginDescriptor(
		name = "RuneRadar",
		description = "Finds and ranks Grand Exchange flipping opportunities.",
		tags = {
				"grand exchange",
				"ge",
				"flipping",
				"prices",
				"trading",
				"market"
		}
)
public class RuneRadarPlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private RuneRadarConfig config;

	private RuneRadarPanel panel;
	private NavigationButton navigationButton;

	@Override
	protected void startUp()
	{
		panel = new RuneRadarPanel(
				config
		);

		BufferedImage icon =
				createTemporaryIcon();

		navigationButton =
				NavigationButton.builder()
						.tooltip("RuneRadar")
						.icon(icon)
						.priority(5)
						.panel(panel)
						.build();

		clientToolbar.addNavigation(
				navigationButton
		);

		log.info(
				"RuneRadar started using hosted API"
		);
	}

	@Override
	protected void shutDown()
	{
		if (
				navigationButton
						!= null
		)
		{
			clientToolbar.removeNavigation(
					navigationButton
			);
		}

		panel = null;
		navigationButton = null;

		log.info(
				"RuneRadar stopped"
		);
	}

	private BufferedImage createTemporaryIcon()
	{
		int size = 16;

		BufferedImage image =
				new BufferedImage(
						size,
						size,
						BufferedImage.TYPE_INT_ARGB
				);

		Graphics2D graphics =
				image.createGraphics();

		graphics.setRenderingHint(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON
		);

		graphics.setColor(
				new Color(
						32,
						120,
						110
				)
		);

		graphics.fillOval(
				1,
				1,
				14,
				14
		);

		graphics.setColor(
				new Color(
						240,
						190,
						60
				)
		);

		graphics.drawOval(
				4,
				4,
				8,
				8
		);

		graphics.drawLine(
				8,
				8,
				13,
				4
		);

		graphics.dispose();

		return image;
	}

	@Provides
	RuneRadarConfig provideConfig(
			ConfigManager configManager
	)
	{
		return configManager.getConfig(
				RuneRadarConfig.class
		);
	}
}
