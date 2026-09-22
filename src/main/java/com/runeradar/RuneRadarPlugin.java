package com.runeradar;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
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
	private static final String BACKEND_PATH =
			"C:\\Users\\danny steinmann\\Desktop\\RuneRadar\\dist\\RuneRadarBackend.exe";

	private static final String HEALTH_URL =
			"http://127.0.0.1:8765/health";

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private RuneRadarConfig config;

	private RuneRadarPanel panel;
	private NavigationButton navigationButton;

	private Process backendProcess;

	@Override
	protected void startUp()
	{
		startBackendIfNeeded();

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
				"RuneRadar started"
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

	private void startBackendIfNeeded()
	{
		if (isBackendRunning())
		{
			log.info(
					"RuneRadar backend already running"
			);

			return;
		}

		try
		{
			log.info(
					"Starting RuneRadar backend"
			);

			ProcessBuilder processBuilder =
					new ProcessBuilder(
							BACKEND_PATH
					);

			processBuilder.redirectErrorStream(
					true
			);

			backendProcess =
					processBuilder.start();

			waitForBackend();

			if (isBackendRunning())
			{
				log.info(
						"RuneRadar backend started successfully"
				);
			}
			else
			{
				log.warn(
						"RuneRadar backend did not respond in time"
				);
			}
		}
		catch (IOException exception)
		{
			log.error(
					"Could not start RuneRadar backend",
					exception
			);
		}
	}

	private boolean isBackendRunning()
	{
		HttpURLConnection connection =
				null;

		try
		{
			URL url =
					new URL(
							HEALTH_URL
					);

			connection =
					(HttpURLConnection)
							url.openConnection();

			connection.setRequestMethod(
					"GET"
			);

			connection.setConnectTimeout(
					500
			);

			connection.setReadTimeout(
					500
			);

			int responseCode =
					connection.getResponseCode();

			return responseCode >= 200
					&& responseCode < 300;
		}
		catch (Exception exception)
		{
			return false;
		}
		finally
		{
			if (connection != null)
			{
				connection.disconnect();
			}
		}
	}

	private void waitForBackend()
	{
		for (
				int attempt = 0;
				attempt < 20;
				attempt++
		)
		{
			if (isBackendRunning())
			{
				return;
			}

			try
			{
				Thread.sleep(
						250
				);
			}
			catch (InterruptedException exception)
			{
				Thread.currentThread()
						.interrupt();

				return;
			}
		}
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
