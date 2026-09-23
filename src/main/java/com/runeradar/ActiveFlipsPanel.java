package com.runeradar;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ActiveFlipsPanel extends JPanel
{
    private static final Color BACKGROUND =
            new Color(
                    30,
                    30,
                    30
            );

    private static final Color CARD_BACKGROUND =
            new Color(
                    40,
                    40,
                    40
            );

    private static final Color BORDER =
            new Color(
                    65,
                    65,
                    65
            );

    private static final Color TEXT =
            new Color(
                    230,
                    230,
                    230
            );

    private static final Color MUTED =
            new Color(
                    165,
                    165,
                    165
            );

    private static final Color GREEN =
            new Color(
                    95,
                    200,
                    120
            );

    private static final Color YELLOW =
            new Color(
                    230,
                    190,
                    80
            );

    private final ActiveFlipStore store;

    private final NumberFormat numberFormat =
            NumberFormat.getIntegerInstance(
                    Locale.US
            );

    private final JPanel listPanel =
            new JPanel();

    private final JLabel countLabel =
            new JLabel(
                    "0 active flips"
            );

    public ActiveFlipsPanel(
            ActiveFlipStore store
    )
    {
        this.store =
                store;

        setLayout(
                new BorderLayout(
                        0,
                        8
                )
        );

        setBackground(
                BACKGROUND
        );

        setBorder(
                BorderFactory.createEmptyBorder(
                        8,
                        8,
                        8,
                        8
                )
        );

        add(
                createHeader(),
                BorderLayout.NORTH
        );

        add(
                createScrollArea(),
                BorderLayout.CENTER
        );

        refresh();
    }

    // ========================================================
    // HEADER
    // ========================================================

    private JPanel createHeader()
    {
        JPanel panel =
                new JPanel();

        panel.setLayout(
                new BoxLayout(
                        panel,
                        BoxLayout.Y_AXIS
                )
        );

        panel.setBackground(
                BACKGROUND
        );

        JLabel title =
                new JLabel(
                        "ACTIVE FLIPS"
                );

        title.setForeground(
                TEXT
        );

        title.setFont(
                title.getFont()
                        .deriveFont(
                                Font.BOLD,
                                15f
                        )
        );

        title.setAlignmentX(
                LEFT_ALIGNMENT
        );

        countLabel.setForeground(
                MUTED
        );

        countLabel.setAlignmentX(
                LEFT_ALIGNMENT
        );

        JButton refreshButton =
                new JButton(
                        "Refresh active flips"
                );

        refreshButton.setAlignmentX(
                LEFT_ALIGNMENT
        );

        refreshButton.addActionListener(
                event ->
                        refresh()
        );

        panel.add(
                title
        );

        panel.add(
                Box.createVerticalStrut(
                        4
                )
        );

        panel.add(
                countLabel
        );

        panel.add(
                Box.createVerticalStrut(
                        8
                )
        );

        panel.add(
                refreshButton
        );

        panel.add(
                Box.createVerticalStrut(
                        4
                )
        );

        return panel;
    }

    // ========================================================
    // SCROLL AREA
    // ========================================================

    private JScrollPane createScrollArea()
    {
        listPanel.setLayout(
                new BoxLayout(
                        listPanel,
                        BoxLayout.Y_AXIS
                )
        );

        listPanel.setBackground(
                BACKGROUND
        );

        JScrollPane scrollPane =
                new JScrollPane(
                        listPanel
                );

        scrollPane.setBorder(
                BorderFactory.createEmptyBorder()
        );

        scrollPane.getViewport()
                .setBackground(
                        BACKGROUND
                );

        scrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        scrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        return scrollPane;
    }

    // ========================================================
    // REFRESH
    // ========================================================

    public void refresh()
    {
        listPanel.removeAll();

        List<ActiveFlipStore.ActiveFlip> flips =
                store.getAll();

        int activeCount =
                0;

        for (
                ActiveFlipStore.ActiveFlip flip
                : flips
        )
        {
            if (
                    flip == null
                            || flip.isCompleted()
            )
            {
                continue;
            }

            activeCount++;

            listPanel.add(
                    createFlipCard(
                            flip
                    )
            );

            listPanel.add(
                    Box.createVerticalStrut(
                            8
                    )
            );
        }

        countLabel.setText(
                activeCount
                        + (
                        activeCount == 1
                                ? " active flip"
                                : " active flips"
                )
        );

        if (activeCount == 0)
        {
            listPanel.add(
                    createEmptyState()
            );
        }

        listPanel.revalidate();
        listPanel.repaint();

        revalidate();
        repaint();
    }

    // ========================================================
    // EMPTY STATE
    // ========================================================

    private JPanel createEmptyState()
    {
        JPanel panel =
                new JPanel(
                        new BorderLayout()
                );

        panel.setBackground(
                CARD_BACKGROUND
        );

        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER
                        ),
                        BorderFactory.createEmptyBorder(
                                14,
                                10,
                                14,
                                10
                        )
                )
        );

        JLabel label =
                new JLabel(
                        "<html><div style='text-align:center;'>"
                                + "No active flips yet.<br>"
                                + "<span style='color:#aaaaaa;'>"
                                + "Start a flip from a RuneRadar recommendation."
                                + "</span>"
                                + "</div></html>",
                        SwingConstants.CENTER
                );

        label.setForeground(
                TEXT
        );

        panel.add(
                label,
                BorderLayout.CENTER
        );

        panel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        90
                )
        );

        return panel;
    }

    // ========================================================
    // FLIP CARD
    // ========================================================

    private JPanel createFlipCard(
            ActiveFlipStore.ActiveFlip flip
    )
    {
        JPanel card =
                new JPanel();

        card.setLayout(
                new BoxLayout(
                        card,
                        BoxLayout.Y_AXIS
                )
        );

        card.setBackground(
                CARD_BACKGROUND
        );

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER
                        ),
                        BorderFactory.createEmptyBorder(
                                10,
                                10,
                                10,
                                10
                        )
                )
        );

        card.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        260
                )
        );

        JLabel nameLabel =
                createLabel(
                        flip.getItemName(),
                        TEXT,
                        true
                );

        JLabel statusLabel =
                createLabel(
                        "Status: "
                                + friendlyStatus(
                                flip.getStatus()
                        ),
                        statusColor(
                                flip.getStatus()
                        ),
                        true
                );

        JLabel planLabel =
                createLabel(
                        "Plan: buy "
                                + numberFormat.format(
                                flip.getRecommendedQuantity()
                        )
                                + " @ "
                                + formatGp(
                                flip.getRecommendedBuyPrice()
                        ),
                        MUTED,
                        false
                );

        JLabel targetLabel =
                createLabel(
                        "Target sell: "
                                + formatGp(
                                flip.getTargetSellPrice()
                        ),
                        MUTED,
                        false
                );

        JLabel expectedLabel =
                createLabel(
                        "Expected net: +"
                                + formatGp(
                                flip.getExpectedNetProfit()
                        ),
                        GREEN,
                        true
                );

        JLabel boughtLabel =
                createLabel(
                        "Bought: "
                                + numberFormat.format(
                                flip.getBoughtQuantity()
                        )
                                + " / "
                                + numberFormat.format(
                                flip.getRecommendedQuantity()
                        ),
                        TEXT,
                        false
                );

        JLabel soldLabel =
                createLabel(
                        "Sold: "
                                + numberFormat.format(
                                flip.getSoldQuantity()
                        )
                                + " / "
                                + numberFormat.format(
                                flip.getBoughtQuantity()
                        ),
                        TEXT,
                        false
                );

        JLabel averageBuyLabel =
                createLabel(
                        "Avg buy: "
                                + (
                                flip.getAverageBuyPrice() > 0
                                        ? formatGp(
                                        flip.getAverageBuyPrice()
                                )
                                        : "-"
                        ),
                        MUTED,
                        false
                );

        JLabel averageSellLabel =
                createLabel(
                        "Avg sell: "
                                + (
                                flip.getAverageSellPrice() > 0
                                        ? formatGp(
                                        flip.getAverageSellPrice()
                                )
                                        : "-"
                        ),
                        MUTED,
                        false
                );

        JLabel taxLabel =
                createLabel(
                        "Tax paid: "
                                + formatGp(
                                flip.getTotalTax()
                        ),
                        MUTED,
                        false
                );

        long realizedNet =
                flip.getRealizedNetProfit();

        JLabel realizedLabel =
                createLabel(
                        "Realized net: "
                                + signedGp(
                                realizedNet
                        ),
                        realizedNet >= 0
                                ? GREEN
                                : Color.RED,
                        true
                );

        JButton removeButton =
                new JButton(
                        "Remove"
                );

        removeButton.setAlignmentX(
                LEFT_ALIGNMENT
        );

        removeButton.addActionListener(
                event ->
                {
                    store.remove(
                            flip.getId()
                    );

                    refresh();
                }
        );

        card.add(
                nameLabel
        );

        card.add(
                Box.createVerticalStrut(
                        4
                )
        );

        card.add(
                statusLabel
        );

        card.add(
                Box.createVerticalStrut(
                        7
                )
        );

        card.add(
                planLabel
        );

        card.add(
                targetLabel
        );

        card.add(
                expectedLabel
        );

        card.add(
                Box.createVerticalStrut(
                        7
                )
        );

        card.add(
                boughtLabel
        );

        card.add(
                soldLabel
        );

        card.add(
                averageBuyLabel
        );

        card.add(
                averageSellLabel
        );

        card.add(
                taxLabel
        );

        card.add(
                realizedLabel
        );

        card.add(
                Box.createVerticalStrut(
                        8
                )
        );

        card.add(
                removeButton
        );

        return card;
    }

    // ========================================================
    // LABEL HELPERS
    // ========================================================

    private JLabel createLabel(
            String text,
            Color color,
            boolean bold
    )
    {
        JLabel label =
                new JLabel(
                        text
                );

        label.setForeground(
                color
        );

        label.setAlignmentX(
                LEFT_ALIGNMENT
        );

        if (bold)
        {
            label.setFont(
                    label.getFont()
                            .deriveFont(
                                    Font.BOLD
                            )
            );
        }

        return label;
    }

    // ========================================================
    // STATUS
    // ========================================================

    private String friendlyStatus(
            String status
    )
    {
        if (
                ActiveFlipStore.ActiveFlip.STATUS_BUYING
                        .equals(
                                status
                        )
        )
        {
            return "Buying";
        }

        if (
                ActiveFlipStore.ActiveFlip.STATUS_READY_TO_SELL
                        .equals(
                                status
                        )
        )
        {
            return "Ready to sell";
        }

        if (
                ActiveFlipStore.ActiveFlip.STATUS_SELLING
                        .equals(
                                status
                        )
        )
        {
            return "Selling";
        }

        if (
                ActiveFlipStore.ActiveFlip.STATUS_COMPLETED
                        .equals(
                                status
                        )
        )
        {
            return "Completed";
        }

        return "Planned";
    }

    private Color statusColor(
            String status
    )
    {
        if (
                ActiveFlipStore.ActiveFlip.STATUS_READY_TO_SELL
                        .equals(
                                status
                        )
        )
        {
            return GREEN;
        }

        if (
                ActiveFlipStore.ActiveFlip.STATUS_COMPLETED
                        .equals(
                                status
                        )
        )
        {
            return GREEN;
        }

        return YELLOW;
    }

    // ========================================================
    // FORMATTING
    // ========================================================

    private String formatGp(
            long value
    )
    {
        return numberFormat.format(
                value
        )
                + " gp";
    }

    private String signedGp(
            long value
    )
    {
        if (value > 0)
        {
            return "+"
                    + formatGp(
                    value
            );
        }

        return formatGp(
                value
        );
    }
}
