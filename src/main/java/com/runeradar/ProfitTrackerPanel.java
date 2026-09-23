package com.runeradar;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ProfitTrackerPanel extends JPanel
{
    private static final Color BACKGROUND =
            new Color(35, 35, 35);

    private static final Color CARD_BACKGROUND =
            new Color(45, 45, 45);

    private static final Color TEXT =
            new Color(220, 220, 220);

    private static final Color MUTED =
            new Color(165, 165, 165);

    private static final Color GREEN =
            new Color(90, 200, 120);

    private static final Color RED =
            new Color(220, 90, 90);

    private static final Color GOLD =
            new Color(230, 180, 70);

    private final ProfitTrackerStore store;

    private final NumberFormat numberFormat =
            NumberFormat.getIntegerInstance();

    private final long sessionStartedAt =
            Instant.now().getEpochSecond();

    private final JButton toggleButton =
            new JButton("Profit Tracker ▸");

    private final JPanel contentPanel =
            new JPanel();

    private final JLabel sessionProfitLabel =
            new JLabel();

    private final JLabel todayProfitLabel =
            new JLabel();

    private final JLabel totalProfitLabel =
            new JLabel();

    private final JLabel completedFlipsLabel =
            new JLabel();

    private final JTextField itemNameInput =
            new JTextField();

    private final JTextField buyPriceInput =
            new JTextField();

    private final JTextField sellPriceInput =
            new JTextField();

    private final JTextField quantityInput =
            new JTextField();

    private final JButton saveFlipButton =
            new JButton("Add completed flip");

    private final JLabel calculatedProfitLabel =
            new JLabel();

    private final JLabel messageLabel =
            new JLabel();

    private final JPanel historyPanel =
            new JPanel();

    private boolean expanded =
            false;

    public ProfitTrackerPanel()
    {
        this(
                new ProfitTrackerStore()
        );
    }

    public ProfitTrackerPanel(
            ProfitTrackerStore store
    )
    {
        this.store =
                store != null
                        ? store
                        : new ProfitTrackerStore();

        setLayout(
                new BoxLayout(
                        this,
                        BoxLayout.Y_AXIS
                )
        );

        setBackground(
                BACKGROUND
        );

        setBorder(
                BorderFactory.createEmptyBorder(
                        0,
                        0,
                        0,
                        0
                )
        );

        createContent();

        add(
                toggleButton
        );

        add(
                Box.createVerticalStrut(
                        5
                )
        );

        add(
                contentPanel
        );

        toggleButton.addActionListener(
                event ->
                        toggleTracker()
        );

        contentPanel.setVisible(
                false
        );

        refreshStats();
    }

    // ========================================================
    // CONTENT
    // ========================================================

    private void createContent()
    {
        contentPanel.setLayout(
                new BoxLayout(
                        contentPanel,
                        BoxLayout.Y_AXIS
                )
        );

        contentPanel.setBackground(
                CARD_BACKGROUND
        );

        contentPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(
                                        65,
                                        65,
                                        65
                                )
                        ),
                        BorderFactory.createEmptyBorder(
                                10,
                                10,
                                10,
                                10
                        )
                )
        );

        JLabel title =
                new JLabel(
                        "PROFIT TRACKER"
                );

        title.setForeground(
                GOLD
        );

        title.setFont(
                title.getFont()
                        .deriveFont(
                                Font.BOLD,
                                11f
                        )
        );

        contentPanel.add(
                title
        );

        contentPanel.add(
                Box.createVerticalStrut(
                        8
                )
        );

        prepareStatLabel(
                sessionProfitLabel
        );

        prepareStatLabel(
                todayProfitLabel
        );

        prepareStatLabel(
                totalProfitLabel
        );

        completedFlipsLabel.setForeground(
                MUTED
        );

        contentPanel.add(
                sessionProfitLabel
        );

        contentPanel.add(
                Box.createVerticalStrut(
                        3
                )
        );

        contentPanel.add(
                todayProfitLabel
        );

        contentPanel.add(
                Box.createVerticalStrut(
                        3
                )
        );

        contentPanel.add(
                totalProfitLabel
        );

        contentPanel.add(
                Box.createVerticalStrut(
                        3
                )
        );

        contentPanel.add(
                completedFlipsLabel
        );

        contentPanel.add(
                Box.createVerticalStrut(
                        14
                )
        );

        JLabel addTitle =
                new JLabel(
                        "ADD COMPLETED FLIP"
                );

        addTitle.setForeground(
                MUTED
        );

        addTitle.setFont(
                addTitle.getFont()
                        .deriveFont(
                                Font.BOLD,
                                10f
                        )
        );

        contentPanel.add(
                addTitle
        );

        contentPanel.add(
                Box.createVerticalStrut(
                        6
                )
        );

        itemNameInput.setToolTipText(
                "Item name"
        );

        buyPriceInput.setToolTipText(
                "Actual buy price per item"
        );

        sellPriceInput.setToolTipText(
                "Actual sell price per item"
        );

        quantityInput.setToolTipText(
                "Quantity sold"
        );

        contentPanel.add(
                createInputRow(
                        "Item",
                        itemNameInput
                )
        );

        contentPanel.add(
                Box.createVerticalStrut(
                        4
                )
        );

        contentPanel.add(
                createInputRow(
                        "Buy",
                        buyPriceInput
                )
        );

        contentPanel.add(
                Box.createVerticalStrut(
                        4
                )
        );

        contentPanel.add(
                createInputRow(
                        "Sell",
                        sellPriceInput
                )
        );

        contentPanel.add(
                Box.createVerticalStrut(
                        4
                )
        );

        contentPanel.add(
                createInputRow(
                        "Qty",
                        quantityInput
                )
        );

        contentPanel.add(
                Box.createVerticalStrut(
                        7
                )
        );

        calculatedProfitLabel.setForeground(
                MUTED
        );

        calculatedProfitLabel.setText(
                "Net profit: -"
        );

        contentPanel.add(
                calculatedProfitLabel
        );

        contentPanel.add(
                Box.createVerticalStrut(
                        6
                )
        );

        saveFlipButton.addActionListener(
                event ->
                        saveCompletedFlip()
        );

        contentPanel.add(
                saveFlipButton
        );

        contentPanel.add(
                Box.createVerticalStrut(
                        6
                )
        );

        messageLabel.setForeground(
                MUTED
        );

        messageLabel.setFont(
                messageLabel.getFont()
                        .deriveFont(
                                10f
                        )
        );

        contentPanel.add(
                messageLabel
        );

        contentPanel.add(
                Box.createVerticalStrut(
                        14
                )
        );

        JLabel historyTitle =
                new JLabel(
                        "RECENT FLIPS"
                );

        historyTitle.setForeground(
                MUTED
        );

        historyTitle.setFont(
                historyTitle.getFont()
                        .deriveFont(
                                Font.BOLD,
                                10f
                        )
        );

        contentPanel.add(
                historyTitle
        );

        contentPanel.add(
                Box.createVerticalStrut(
                        6
                )
        );

        historyPanel.setLayout(
                new BoxLayout(
                        historyPanel,
                        BoxLayout.Y_AXIS
                )
        );

        historyPanel.setBackground(
                CARD_BACKGROUND
        );

        contentPanel.add(
                historyPanel
        );
    }

    private JPanel createInputRow(
            String labelText,
            JTextField input
    )
    {
        JPanel panel =
                new JPanel(
                        new GridLayout(
                                1,
                                2,
                                5,
                                0
                        )
                );

        panel.setBackground(
                CARD_BACKGROUND
        );

        JLabel label =
                new JLabel(
                        labelText
                );

        label.setForeground(
                MUTED
        );

        panel.add(
                label
        );

        panel.add(
                input
        );

        return panel;
    }

    private void prepareStatLabel(
            JLabel label
    )
    {
        label.setForeground(
                TEXT
        );

        label.setFont(
                label.getFont()
                        .deriveFont(
                                Font.BOLD,
                                12f
                        )
        );
    }

    // ========================================================
    // TOGGLE
    // ========================================================

    private void toggleTracker()
    {
        expanded =
                !expanded;

        contentPanel.setVisible(
                expanded
        );

        toggleButton.setText(
                expanded
                        ? "Profit Tracker ▾"
                        : "Profit Tracker ▸"
        );

        if (expanded)
        {
            refreshStats();
        }

        revalidate();
        repaint();
    }

    // ========================================================
    // SAVE FLIP
    // ========================================================

    private void saveCompletedFlip()
    {
        try
        {
            String itemName =
                    itemNameInput
                            .getText()
                            .trim();

            long buyPrice =
                    parseGp(
                            buyPriceInput
                                    .getText()
                    );

            long sellPrice =
                    parseGp(
                            sellPriceInput
                                    .getText()
                    );

            long quantity =
                    parseGp(
                            quantityInput
                                    .getText()
                    );

            ProfitTrackerStore.FlipRecord record =
                    store.addCompletedFlip(
                            itemName,
                            buyPrice,
                            sellPrice,
                            quantity
                    );

            long profit =
                    record.getNetProfit();

            calculatedProfitLabel.setText(
                    "Net profit: "
                            + formatSignedGp(
                            profit
                    )
            );

            calculatedProfitLabel.setForeground(
                    profit >= 0
                            ? GREEN
                            : RED
            );

            messageLabel.setForeground(
                    GREEN
            );

            messageLabel.setText(
                    "Flip saved"
            );

            itemNameInput.setText(
                    ""
            );

            buyPriceInput.setText(
                    ""
            );

            sellPriceInput.setText(
                    ""
            );

            quantityInput.setText(
                    ""
            );

            refreshStats();
        }
        catch (
                IllegalArgumentException exception
        )
        {
            messageLabel.setForeground(
                    RED
            );

            messageLabel.setText(
                    exception.getMessage()
            );
        }
    }

    private long parseGp(
            String value
    )
    {
        if (
                value == null
                        || value.trim().isEmpty()
        )
        {
            throw new IllegalArgumentException(
                    "Fill in all fields"
            );
        }

        String cleaned =
                value
                        .trim()
                        .toLowerCase()
                        .replace(",", "")
                        .replace(" ", "")
                        .replace("_", "");

        double multiplier =
                1.0;

        if (
                cleaned.endsWith(
                        "k"
                )
        )
        {
            multiplier =
                    1_000.0;

            cleaned =
                    cleaned.substring(
                            0,
                            cleaned.length() - 1
                    );
        }
        else if (
                cleaned.endsWith(
                        "m"
                )
        )
        {
            multiplier =
                    1_000_000.0;

            cleaned =
                    cleaned.substring(
                            0,
                            cleaned.length() - 1
                    );
        }
        else if (
                cleaned.endsWith(
                        "b"
                )
        )
        {
            multiplier =
                    1_000_000_000.0;

            cleaned =
                    cleaned.substring(
                            0,
                            cleaned.length() - 1
                    );
        }

        try
        {
            double parsed =
                    Double.parseDouble(
                            cleaned
                    );

            long result =
                    Math.round(
                            parsed * multiplier
                    );

            if (result <= 0)
            {
                throw new IllegalArgumentException(
                        "Values must be above 0"
                );
            }

            return result;
        }
        catch (
                NumberFormatException exception
        )
        {
            throw new IllegalArgumentException(
                    "Use numbers like 150k or 2.5m"
            );
        }
    }

    // ========================================================
    // STATS
    // ========================================================

    public void refreshStats()
    {
        long sessionProfit =
                store.getSessionProfit(
                        sessionStartedAt
                );

        long todayProfit =
                store.getTodayProfit();

        long totalProfit =
                store.getTotalProfit();

        sessionProfitLabel.setText(
                "Session: "
                        + formatSignedGp(
                        sessionProfit
                )
        );

        todayProfitLabel.setText(
                "Today: "
                        + formatSignedGp(
                        todayProfit
                )
        );

        totalProfitLabel.setText(
                "All-time: "
                        + formatSignedGp(
                        totalProfit
                )
        );

        sessionProfitLabel.setForeground(
                getProfitColor(
                        sessionProfit
                )
        );

        todayProfitLabel.setForeground(
                getProfitColor(
                        todayProfit
                )
        );

        totalProfitLabel.setForeground(
                getProfitColor(
                        totalProfit
                )
        );

        completedFlipsLabel.setText(
                "Completed flips: "
                        + store.getCompletedFlipCount()
        );

        refreshHistory();
    }

    // ========================================================
    // HISTORY
    // ========================================================

    private void refreshHistory()
    {
        historyPanel.removeAll();

        List<ProfitTrackerStore.FlipRecord>
                history =
                store.getRecentHistory(
                        5
                );

        if (
                history.isEmpty()
        )
        {
            JLabel empty =
                    new JLabel(
                            "No completed flips yet"
                    );

            empty.setForeground(
                    MUTED
            );

            historyPanel.add(
                    empty
            );
        }
        else
        {
            for (
                    ProfitTrackerStore.FlipRecord record
                    : history
            )
            {
                historyPanel.add(
                        createHistoryRow(
                                record
                        )
                );

                historyPanel.add(
                        Box.createVerticalStrut(
                                5
                        )
                );
            }
        }

        historyPanel.revalidate();
        historyPanel.repaint();
    }

    private JPanel createHistoryRow(
            ProfitTrackerStore.FlipRecord record
    )
    {
        JPanel row =
                new JPanel();

        row.setLayout(
                new BoxLayout(
                        row,
                        BoxLayout.Y_AXIS
                )
        );

        row.setBackground(
                new Color(
                        40,
                        40,
                        40
                )
        );

        row.setBorder(
                BorderFactory.createEmptyBorder(
                        5,
                        6,
                        5,
                        6
                )
        );

        JLabel item =
                new JLabel(
                        record.getItemName()
                );

        item.setForeground(
                TEXT
        );

        item.setFont(
                item.getFont()
                        .deriveFont(
                                Font.BOLD,
                                11f
                        )
        );

        JLabel profit =
                new JLabel(
                        formatSignedGp(
                                record.getNetProfit()
                        )
                );

        profit.setForeground(
                getProfitColor(
                        record.getNetProfit()
                )
        );

        JLabel info =
                new JLabel(
                        numberFormat.format(
                                record.getQuantity()
                        )
                                + " items"
                                + " • "
                                + formatTime(
                                record.getTimestamp()
                        )
                );

        info.setForeground(
                MUTED
        );

        info.setFont(
                info.getFont()
                        .deriveFont(
                                9f
                        )
        );

        row.add(
                item
        );

        row.add(
                profit
        );

        row.add(
                info
        );

        return row;
    }

    // ========================================================
    // FORMATTING
    // ========================================================

    private Color getProfitColor(
            long profit
    )
    {
        if (profit > 0)
        {
            return GREEN;
        }

        if (profit < 0)
        {
            return RED;
        }

        return TEXT;
    }

    private String formatSignedGp(
            long value
    )
    {
        if (value > 0)
        {
            return "+"
                    + numberFormat.format(
                    value
            )
                    + " gp";
        }

        if (value < 0)
        {
            return "-"
                    + numberFormat.format(
                    Math.abs(value)
            )
                    + " gp";
        }

        return "0 gp";
    }

    private String formatTime(
            long timestamp
    )
    {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "HH:mm"
                );

        return Instant
                .ofEpochSecond(
                        timestamp
                )
                .atZone(
                        ZoneId.systemDefault()
                )
                .format(
                        formatter
                );
    }
}
