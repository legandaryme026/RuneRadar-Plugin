package com.runeradar;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import okhttp3.OkHttpClient;

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
	private static final double GE_TAX_RATE =
			0.02;

	private static final long GE_TAX_CAP_PER_ITEM =
			5_000_000L;

	private static final long GE_TAX_MIN_PRICE =
			50L;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private RuneRadarConfig config;

	@Inject
	private Client client;

	@Inject
	private Gson gson;

	@Inject
	private OkHttpClient httpClient;

	private RuneRadarPanel panel;

	private ActiveFlipStore activeFlipStore;

	private ActiveFlipsPanel activeFlipsPanel;

	private ProfitTrackerStore profitTrackerStore;

	private ProfitTrackerPanel profitTrackerPanel;

	private NavigationButton navigationButton;

	private final Map<Integer, OfferSnapshot> offerSnapshots =
			new HashMap<>();

	@Override
	protected void startUp()
	{
		RuneRadarApiClient.setGson(
				gson
		);

		RuneRadarApiClient.setHttpClient(
				httpClient
		);

		activeFlipStore =
				new ActiveFlipStore();

		activeFlipsPanel =
				new ActiveFlipsPanel(
						activeFlipStore
				);

		profitTrackerStore =
				new ProfitTrackerStore();

		profitTrackerPanel =
				new ProfitTrackerPanel(
						profitTrackerStore
				);

		panel =
				new RuneRadarPanel(
						config,
						activeFlipStore,
						activeFlipsPanel,
						profitTrackerPanel
				);

		recoverCompletedFlips();

		primeGrandExchangeSnapshots();

		BufferedImage icon =
				createTemporaryIcon();

		navigationButton =
				NavigationButton.builder()
						.tooltip(
								"RuneRadar"
						)
						.icon(
								icon
						)
						.priority(
								5
						)
						.panel(
								panel
						)
						.build();

		clientToolbar.addNavigation(
				navigationButton
		);

		log.info(
				"RuneRadar started with {} active flips",
				activeFlipStore.size()
		);
	}

	@Override
	protected void shutDown()
	{
		offerSnapshots.clear();

		if (
				navigationButton
						!= null
		)
		{
			clientToolbar.removeNavigation(
					navigationButton
			);
		}

		panel =
				null;

		activeFlipsPanel =
				null;

		activeFlipStore =
				null;

		profitTrackerPanel =
				null;

		profitTrackerStore =
				null;

		navigationButton =
				null;

		log.info(
				"RuneRadar stopped"
		);
	}

	// ========================================================
	// GRAND EXCHANGE TRACKING
	// ========================================================

	@Subscribe
	public void onGrandExchangeOfferChanged(
			GrandExchangeOfferChanged event
	)
	{
		if (
				event == null
						|| activeFlipStore == null
		)
		{
			return;
		}

		GrandExchangeOffer offer =
				event.getOffer();

		if (offer == null)
		{
			return;
		}

		int slot =
				event.getSlot();

		GrandExchangeOfferState state =
				offer.getState();

		if (
				state == null
						|| state == GrandExchangeOfferState.EMPTY
		)
		{
			offerSnapshots.remove(
					slot
			);

			return;
		}

		OfferSnapshot current =
				OfferSnapshot.from(
						offer
				);

		OfferSnapshot previous =
				offerSnapshots.get(
						slot
				);

		boolean newOffer =
				previous == null
						|| !previous.matchesSameOffer(
						current
				);

		int quantityDelta;

		long spentDelta;

		if (newOffer)
		{
			quantityDelta =
					current.quantitySold;

			spentDelta =
					current.spent;
		}
		else
		{
			quantityDelta =
					current.quantitySold
							- previous.quantitySold;

			spentDelta =
					current.spent
							- previous.spent;
		}

		offerSnapshots.put(
				slot,
				current
		);

		ActiveFlipStore.ActiveFlip activeFlip =
				activeFlipStore.findOpenByItemId(
						current.itemId
				);

		if (
				activeFlip == null
						&& isBuyState(
						current.state
				)
						&& quantityDelta > 0
						&& spentDelta > 0
						&& panel != null
		)
		{
			RuneRadarApiClient.Recommendation recommendation =
					panel.findLoadedRecommendationByItemId(
							current.itemId
					);

			if (recommendation != null)
			{
				activeFlip =
						activeFlipStore.addRecommendation(
								recommendation.getId(),
								recommendation.getName(),
								recommendation.getBuy(),
								recommendation.getSell(),
								Math.toIntExact(
										recommendation.getRecommendedQty()
								),
								recommendation.getNetExpectedProfit()
						);

				log.info(
						"RuneRadar automatically started Active Flip for recommended item {}",
						recommendation.getName()
				);
			}
		}

		if (
				activeFlip != null
						&& quantityDelta > 0
						&& spentDelta > 0
		)
		{
			if (
					isBuyState(
							current.state
					)
			)
			{
				activeFlipStore.recordBuyTotal(
						activeFlip.getId(),
						quantityDelta,
						spentDelta
				);

				log.info(
						"RuneRadar detected GE buy: {} x{} for {} gp",
						activeFlip.getItemName(),
						quantityDelta,
						spentDelta
				);
			}
			else if (
					isSellState(
							current.state
					)
			)
			{
				long estimatedTax =
						calculateEstimatedTaxForExecution(
								spentDelta,
								quantityDelta
						);

				activeFlipStore.recordSaleTotal(
						activeFlip.getId(),
						quantityDelta,
						spentDelta,
						estimatedTax
				);

				log.info(
						"RuneRadar detected GE sale: {} x{} for {} gp, estimated tax {} gp",
						activeFlip.getItemName(),
						quantityDelta,
						spentDelta,
						estimatedTax
				);

				recordCompletedFlipIfNeeded(
						activeFlip
				);
			}
		}

		if (activeFlip != null)
		{
			applyOfferStateToActiveFlip(
					activeFlip,
					current.state
			);
		}

		refreshActiveFlipsPanel();
	}

	private void applyOfferStateToActiveFlip(
			ActiveFlipStore.ActiveFlip activeFlip,
			GrandExchangeOfferState state
	)
	{
		if (
				activeFlip == null
						|| state == null
						|| activeFlip.isCompleted()
		)
		{
			return;
		}

		if (
				state == GrandExchangeOfferState.BOUGHT
		)
		{
			if (activeFlip.getOpenQuantity() > 0)
			{
				activeFlipStore.setStatus(
						activeFlip.getId(),
						ActiveFlipStore.ActiveFlip.STATUS_READY_TO_SELL
				);
			}

			return;
		}

		if (
				state == GrandExchangeOfferState.CANCELLED_BUY
		)
		{
			if (activeFlip.getOpenQuantity() > 0)
			{
				activeFlipStore.setStatus(
						activeFlip.getId(),
						ActiveFlipStore.ActiveFlip.STATUS_READY_TO_SELL
				);
			}
			else
			{
				activeFlipStore.setStatus(
						activeFlip.getId(),
						ActiveFlipStore.ActiveFlip.STATUS_PLANNED
				);
			}

			return;
		}

		if (
				state == GrandExchangeOfferState.SOLD
						|| state == GrandExchangeOfferState.CANCELLED_SELL
		)
		{
			if (activeFlip.getOpenQuantity() > 0)
			{
				activeFlipStore.setStatus(
						activeFlip.getId(),
						ActiveFlipStore.ActiveFlip.STATUS_READY_TO_SELL
				);
			}
		}
	}

	private void recordCompletedFlipIfNeeded(
			ActiveFlipStore.ActiveFlip activeFlip
	)
	{
		if (
				activeFlip == null
						|| !activeFlip.isCompleted()
						|| activeFlip.isProfitTrackerRecorded()
						|| profitTrackerStore == null
		)
		{
			return;
		}

		long completedQuantity =
				Math.min(
						activeFlip.getBoughtQuantity(),
						activeFlip.getSoldQuantity()
				);

		if (completedQuantity <= 0)
		{
			return;
		}

		try
		{
			profitTrackerStore.addCompletedFlipExact(
					activeFlip.getItemName(),
					activeFlip.getAverageBuyPrice(),
					activeFlip.getAverageSellPrice(),
					completedQuantity,
					activeFlip.getTotalTax(),
					activeFlip.getRealizedNetProfit()
			);

			activeFlipStore.markProfitTrackerRecorded(
					activeFlip.getId()
			);

			refreshProfitTrackerPanel();

			log.info(
					"RuneRadar completed flip: {} x{} net {} gp",
					activeFlip.getItemName(),
					completedQuantity,
					activeFlip.getRealizedNetProfit()
			);
		}
		catch (Exception exception)
		{
			log.error(
					"RuneRadar could not write completed flip {} to Profit Tracker",
					activeFlip.getItemName(),
					exception
			);
		}
	}

	private void recoverCompletedFlips()
	{
		if (
				activeFlipStore == null
						|| profitTrackerStore == null
		)
		{
			return;
		}

		for (
				ActiveFlipStore.ActiveFlip activeFlip
				: activeFlipStore.getCompletedUnrecorded()
		)
		{
			recordCompletedFlipIfNeeded(
					activeFlip
			);
		}
	}

	private void primeGrandExchangeSnapshots()
	{
		offerSnapshots.clear();

		if (client == null)
		{
			return;
		}

		GrandExchangeOffer[] offers =
				client.getGrandExchangeOffers();

		if (offers == null)
		{
			return;
		}

		for (
				int slot = 0;
				slot < offers.length;
				slot++
		)
		{
			GrandExchangeOffer offer =
					offers[slot];

			if (
					offer == null
							|| offer.getState() == null
							|| offer.getState()
							== GrandExchangeOfferState.EMPTY
			)
			{
				continue;
			}

			offerSnapshots.put(
					slot,
					OfferSnapshot.from(
							offer
					)
			);
		}
	}

	private boolean isBuyState(
			GrandExchangeOfferState state
	)
	{
		return state == GrandExchangeOfferState.BUYING
				|| state == GrandExchangeOfferState.BOUGHT
				|| state == GrandExchangeOfferState.CANCELLED_BUY;
	}

	private boolean isSellState(
			GrandExchangeOfferState state
	)
	{
		return state == GrandExchangeOfferState.SELLING
				|| state == GrandExchangeOfferState.SOLD
				|| state == GrandExchangeOfferState.CANCELLED_SELL;
	}

	private long calculateEstimatedTaxForExecution(
			long grossSale,
			int quantity
	)
	{
		if (
				grossSale <= 0
						|| quantity <= 0
		)
		{
			return 0;
		}

		long averageSellPrice =
				grossSale
						/ quantity;

		if (
				averageSellPrice
						< GE_TAX_MIN_PRICE
		)
		{
			return 0;
		}

		long taxPerItem =
				(long) Math.floor(
						averageSellPrice
								* GE_TAX_RATE
				);

		taxPerItem =
				Math.min(
						taxPerItem,
						GE_TAX_CAP_PER_ITEM
				);

		return taxPerItem
				* (long) quantity;
	}

	private void refreshActiveFlipsPanel()
	{
		if (activeFlipsPanel == null)
		{
			return;
		}

		SwingUtilities.invokeLater(
				activeFlipsPanel::refresh
		);
	}

	private void refreshProfitTrackerPanel()
	{
		if (profitTrackerPanel == null)
		{
			return;
		}

		SwingUtilities.invokeLater(
				profitTrackerPanel::refreshStats
		);
	}

	// ========================================================
	// ACTIVE FLIPS
	// ========================================================

	public ActiveFlipStore getActiveFlipStore()
	{
		return activeFlipStore;
	}

	public ActiveFlipsPanel getActiveFlipsPanel()
	{
		return activeFlipsPanel;
	}

	// ========================================================
	// ICON
	// ========================================================

	private BufferedImage createTemporaryIcon()
	{
		int size =
				16;

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

	// ========================================================
	// CONFIG
	// ========================================================

	@Provides
	RuneRadarConfig provideConfig(
			ConfigManager configManager
	)
	{
		return configManager.getConfig(
				RuneRadarConfig.class
		);
	}

	// ========================================================
	// GE SNAPSHOT
	// ========================================================

	private static class OfferSnapshot
	{
		private final int itemId;

		private final int totalQuantity;

		private final int offerPrice;

		private final int quantitySold;

		private final long spent;

		private final GrandExchangeOfferState state;

		private OfferSnapshot(
				int itemId,
				int totalQuantity,
				int offerPrice,
				int quantitySold,
				long spent,
				GrandExchangeOfferState state
		)
		{
			this.itemId =
					itemId;

			this.totalQuantity =
					totalQuantity;

			this.offerPrice =
					offerPrice;

			this.quantitySold =
					quantitySold;

			this.spent =
					spent;

			this.state =
					state;
		}

		private static OfferSnapshot from(
				GrandExchangeOffer offer
		)
		{
			return new OfferSnapshot(
					offer.getItemId(),
					offer.getTotalQuantity(),
					offer.getPrice(),
					offer.getQuantitySold(),
					offer.getSpent(),
					offer.getState()
			);
		}

		private boolean matchesSameOffer(
				OfferSnapshot other
		)
		{
			if (other == null)
			{
				return false;
			}

			return itemId == other.itemId
					&& totalQuantity == other.totalQuantity
					&& offerPrice == other.offerPrice
					&& sameSide(
					state,
					other.state
			);
		}

		private static boolean sameSide(
				GrandExchangeOfferState first,
				GrandExchangeOfferState second
		)
		{
			boolean firstBuy =
					first == GrandExchangeOfferState.BUYING
							|| first == GrandExchangeOfferState.BOUGHT
							|| first == GrandExchangeOfferState.CANCELLED_BUY;

			boolean secondBuy =
					second == GrandExchangeOfferState.BUYING
							|| second == GrandExchangeOfferState.BOUGHT
							|| second == GrandExchangeOfferState.CANCELLED_BUY;

			boolean firstSell =
					first == GrandExchangeOfferState.SELLING
							|| first == GrandExchangeOfferState.SOLD
							|| first == GrandExchangeOfferState.CANCELLED_SELL;

			boolean secondSell =
					second == GrandExchangeOfferState.SELLING
							|| second == GrandExchangeOfferState.SOLD
							|| second == GrandExchangeOfferState.CANCELLED_SELL;

			return (
					firstBuy
							&& secondBuy
			)
					|| (
					firstSell
							&& secondSell
			);
		}
	}
}
