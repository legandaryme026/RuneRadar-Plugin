package com.runeradar;

import java.awt.Dimension;
import java.awt.Graphics2D;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class RuneRadarGrandExchangeOverlay extends OverlayPanel
{
    private final Client client;

    private final RuneRadarPanel panel;

    public RuneRadarGrandExchangeOverlay(
            Client client,
            RuneRadarPanel panel
    )
    {
        this.client =
                client;

        this.panel =
                panel;

        setPosition(
                OverlayPosition.BOTTOM_LEFT
        );

        setLayer(
                OverlayLayer.ABOVE_WIDGETS
        );
    }

    @Override
    public Dimension render(
            Graphics2D graphics
    )
    {
        if (
                client == null
                        || panel == null
        )
        {
            return null;
        }

        Widget grandExchangeOffer =
                client.getWidget(
                        WidgetInfo.GRAND_EXCHANGE_OFFER_CONTAINER
                );

        if (
                grandExchangeOffer == null
                        || grandExchangeOffer.isHidden()
        )
        {
            return null;
        }

        RuneRadarApiClient.Recommendation recommendation =
                panel.getCurrentRecommendationForOverlay();

        if (recommendation == null)
        {
            return null;
        }

        long buyPrice =
                recommendation.getBuy();

        long quantity =
                recommendation.getRecommendedQty();

        if (
                buyPrice <= 0
                        || quantity <= 0
        )
        {
            return null;
        }

        panelComponent.getChildren().clear();

        panelComponent.getChildren().add(
                TitleComponent.builder()
                        .text(
                                "RuneRadar GE Hint"
                        )
                        .build()
        );

        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left(
                                "Buy price"
                        )
                        .right(
                                String.format(
                                        "%,d gp",
                                        buyPrice
                                )
                        )
                        .build()
        );

        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left(
                                "Quantity"
                        )
                        .right(
                                String.format(
                                        "%,d",
                                        quantity
                                )
                        )
                        .build()
        );

        return super.render(
                graphics
        );
    }
}
