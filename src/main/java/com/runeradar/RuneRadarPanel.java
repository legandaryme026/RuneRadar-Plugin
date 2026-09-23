package com.runeradar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.client.ui.PluginPanel;

public class RuneRadarPanel extends PluginPanel
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

    private static final Color GOLD =
            new Color(230, 180, 70);

    private static final Color RED =
            new Color(220, 90, 90);

    private static final int RESULT_LIMIT = 20;

    private static final int AUTO_REFRESH_SECONDS = 60;

    private static final long MAX_CASH_STACK =
            2_147_483_647L;

    private final RuneRadarConfig config;

    private final RuneRadarApiClient apiClient =
            new RuneRadarApiClient();

    private final NumberFormat numberFormat =
            NumberFormat.getIntegerInstance();

    // ========================================================
    // CASH
    // ========================================================

    private final JTextField cashInput =
            new JTextField();

    private final JButton applyCashButton =
            new JButton("Apply");

    private final JLabel cashLabel =
            new JLabel();

    private long currentCashStack;

    // ========================================================
    // RESULT MODE
    // ========================================================

    private final JButton strongButton =
            new JButton("Strong");

    private final JButton moreButton =
            new JButton("More");

    private String currentResultMode =
            "STRONG";

    // ========================================================
    // FLIP TYPES
    // ========================================================

    private final JButton fastButton =
            new JButton("Fast");

    private final JButton balancedButton =
            new JButton("Balanced");

    private final JButton slowButton =
            new JButton("Slow");

    private final JButton highProfitButton =
            new JButton("High Profit");

    private final JButton allButton =
            new JButton("All");

    private String currentFlipType =
            "FAST";

    // ========================================================
    // MAIN RESULT
    // ========================================================

    private final JLabel modeLabel =
            new JLabel();

    private final JLabel itemNameLabel =
            new JLabel();

    private final JLabel buyLabel =
            new JLabel();

    private final JLabel sellLabel =
            new JLabel();

    private final JLabel profitLabel =
            new JLabel();

    private final JLabel quantityLabel =
            new JLabel();

    private final JLabel riskLabel =
            new JLabel();

    private final JLabel speedLabel =
            new JLabel();

    private final JLabel stabilityLabel =
            new JLabel();

    private final JLabel scoreLabel =
            new JLabel();

    private final JLabel updatedLabel =
            new JLabel();

    private final JLabel statusLabel =
            new JLabel();

    // ========================================================
    // ADVANCED DETAILS
    // ========================================================

    private final JButton advancedButton =
            new JButton("Show Advanced Details");

    private final JPanel advancedPanel =
            new JPanel();

    private final JLabel netProfitPerItemLabel =
            new JLabel();

    private final JLabel grossProfitPerItemLabel =
            new JLabel();

    private final JLabel taxPerItemLabel =
            new JLabel();

    private final JLabel totalTaxLabel =
            new JLabel();

    private final JLabel grossExpectedProfitLabel =
            new JLabel();

    private final JLabel breakEvenLabel =
            new JLabel();

    private final JLabel netSellPriceLabel =
            new JLabel();

    private final JLabel roiLabel =
            new JLabel();

    private final JLabel grossRoiLabel =
            new JLabel();

    private final JLabel confidenceLabel =
            new JLabel();

    private final JLabel liquidityLabel =
            new JLabel();

    private final JLabel gpNeededLabel =
            new JLabel();

    private final JLabel capitalUsedLabel =
            new JLabel();

    private final JLabel cashAllowsLabel =
            new JLabel();

    private final JLabel buyLimitLabel =
            new JLabel();

    private final JLabel liquidityQtyLabel =
            new JLabel();

    private boolean advancedVisible =
            false;

    // ========================================================
    // NAVIGATION
    // ========================================================

    private final JButton previousButton =
            new JButton("Previous");

    private final JButton nextButton =
            new JButton("Next");

    private final JButton refreshButton =
            new JButton("Refresh");

    // ========================================================
    // PROFIT TRACKER
    // ========================================================

    private final ProfitTrackerPanel profitTrackerPanel;

    // ========================================================
    // ACTIVE FLIPS
    // ========================================================

    private final ActiveFlipStore activeFlipStore;

    private final ActiveFlipsPanel activeFlipsPanel;

    private final JButton startActiveFlipButton =
            new JButton("Start Active Flip");

    // ========================================================
    // DATA STATE
    // ========================================================

    private List<RuneRadarApiClient.Recommendation>
            recommendations =
            new ArrayList<>();

    private int currentIndex = 0;

    private long updatedAt = 0;

    private boolean blockingLoad = false;

    private boolean requestInProgress = false;

    private boolean lastRefreshFailed = false;

    private int requestNumber = 0;

    // ========================================================
    // TIMERS
    // ========================================================

    private final Timer autoRefreshTimer;

    private final Timer freshnessTimer;

    // ========================================================
    // CONSTRUCTOR
    // ========================================================

    public RuneRadarPanel(
            RuneRadarConfig config
    )
    {
        this(
                config,
                new ActiveFlipStore(),
                null,
                null
        );
    }

    public RuneRadarPanel(
            RuneRadarConfig config,
            ActiveFlipStore activeFlipStore,
            ActiveFlipsPanel activeFlipsPanel
    )
    {
        this(
                config,
                activeFlipStore,
                activeFlipsPanel,
                null
        );
    }

    public RuneRadarPanel(
            RuneRadarConfig config,
            ActiveFlipStore activeFlipStore,
            ActiveFlipsPanel activeFlipsPanel,
            ProfitTrackerPanel profitTrackerPanel
    )
    {
        this.config = config;

        this.activeFlipStore =
                activeFlipStore != null
                        ? activeFlipStore
                        : new ActiveFlipStore();

        this.activeFlipsPanel =
                activeFlipsPanel != null
                        ? activeFlipsPanel
                        : new ActiveFlipsPanel(
                        this.activeFlipStore
                );

        this.profitTrackerPanel =
                profitTrackerPanel != null
                        ? profitTrackerPanel
                        : new ProfitTrackerPanel();

        currentCashStack =
                Math.max(
                        1L,
                        config.cashStack()
                );

        setLayout(
                new BorderLayout(
                        0,
                        10
                )
        );

        setBackground(
                BACKGROUND
        );

        setBorder(
                BorderFactory.createEmptyBorder(
                        10,
                        10,
                        10,
                        10
                )
        );

        add(
                createTopSection(),
                BorderLayout.NORTH
        );

        add(
                createMainSection(),
                BorderLayout.CENTER
        );

        add(
                createBottomSection(),
                BorderLayout.SOUTH
        );

        cashInput.setText(
                friendlyCashInput(
                        currentCashStack
                )
        );

        updateCashLabel();

        updateModeLabel();

        updateResultModeButtons();

        updateFlipTypeButtons();

        autoRefreshTimer =
                new Timer(
                        AUTO_REFRESH_SECONDS * 1000,
                        event ->
                        {
                            if (!requestInProgress)
                            {
                                loadRecommendations(
                                        true,
                                        true
                                );
                            }
                        }
                );

        autoRefreshTimer.setRepeats(
                true
        );

        freshnessTimer =
                new Timer(
                        1000,
                        event ->
                                updateLiveStatus()
                );

        freshnessTimer.setRepeats(
                true
        );

        autoRefreshTimer.start();

        freshnessTimer.start();

        loadRecommendations(
                false,
                false
        );
    }

    // ========================================================
    // PANEL LIFECYCLE
    // ========================================================

    @Override
    public void addNotify()
    {
        super.addNotify();

        if (!autoRefreshTimer.isRunning())
        {
            autoRefreshTimer.start();
        }

        if (!freshnessTimer.isRunning())
        {
            freshnessTimer.start();
        }
    }

    @Override
    public void removeNotify()
    {
        autoRefreshTimer.stop();

        freshnessTimer.stop();

        super.removeNotify();
    }

    // ========================================================
    // TOP
    // ========================================================

    private JPanel createTopSection()
    {
        JPanel wrapper =
                new JPanel();

        wrapper.setLayout(
                new BoxLayout(
                        wrapper,
                        BoxLayout.Y_AXIS
                )
        );

        wrapper.setBackground(
                BACKGROUND
        );

        JLabel title =
                new JLabel(
                        "RuneRadar"
                );

        title.setForeground(
                GOLD
        );

        title.setFont(
                title.getFont()
                        .deriveFont(
                                Font.BOLD,
                                20f
                        )
        );

        title.setAlignmentX(
                CENTER_ALIGNMENT
        );

        JLabel subtitle =
                new JLabel(
                        "Grand Exchange Flip Scanner"
                );

        subtitle.setForeground(
                MUTED
        );

        subtitle.setFont(
                subtitle.getFont()
                        .deriveFont(
                                11f
                        )
        );

        subtitle.setAlignmentX(
                CENTER_ALIGNMENT
        );

        wrapper.add(
                title
        );

        wrapper.add(
                Box.createVerticalStrut(
                        3
                )
        );

        wrapper.add(
                subtitle
        );

        wrapper.add(
                Box.createVerticalStrut(
                        12
                )
        );

        wrapper.add(
                createCashSection()
        );

        wrapper.add(
                Box.createVerticalStrut(
                        10
                )
        );

        wrapper.add(
                createResultModeButtons()
        );

        wrapper.add(
                Box.createVerticalStrut(
                        6
                )
        );

        wrapper.add(
                createFlipTypeButtons()
        );

        return wrapper;
    }

    // ========================================================
    // CASH
    // ========================================================

    private JPanel createCashSection()
    {
        JPanel wrapper =
                new JPanel();

        wrapper.setLayout(
                new BoxLayout(
                        wrapper,
                        BoxLayout.Y_AXIS
                )
        );

        wrapper.setBackground(
                BACKGROUND
        );

        JLabel inputTitle =
                new JLabel(
                        "YOUR CASH STACK"
                );

        inputTitle.setForeground(
                MUTED
        );

        inputTitle.setFont(
                inputTitle.getFont()
                        .deriveFont(
                                Font.BOLD,
                                10f
                        )
        );

        inputTitle.setAlignmentX(
                CENTER_ALIGNMENT
        );

        cashLabel.setForeground(
                TEXT
        );

        cashLabel.setAlignmentX(
                CENTER_ALIGNMENT
        );

        JPanel inputRow =
                new JPanel(
                        new BorderLayout(
                                5,
                                0
                        )
                );

        inputRow.setBackground(
                BACKGROUND
        );

        cashInput.setToolTipText(
                "Examples: 25m, 500k, 2.5m"
        );

        applyCashButton.addActionListener(
                event ->
                        applyCashStack()
        );

        cashInput.addActionListener(
                event ->
                        applyCashStack()
        );

        inputRow.add(
                cashInput,
                BorderLayout.CENTER
        );

        inputRow.add(
                applyCashButton,
                BorderLayout.EAST
        );

        wrapper.add(
                inputTitle
        );

        wrapper.add(
                Box.createVerticalStrut(
                        4
                )
        );

        wrapper.add(
                cashLabel
        );

        wrapper.add(
                Box.createVerticalStrut(
                        6
                )
        );

        wrapper.add(
                inputRow
        );

        return wrapper;
    }

    private void applyCashStack()
    {
        try
        {
            currentCashStack =
                    parseCashStack(
                            cashInput
                                    .getText()
                                    .trim()
                    );

            cashInput.setText(
                    friendlyCashInput(
                            currentCashStack
                    )
            );

            updateCashLabel();

            currentIndex = 0;

            loadRecommendations(
                    false,
                    false
            );

            restartAutoRefreshTimer();
        }
        catch (
                IllegalArgumentException exception
        )
        {
            statusLabel.setForeground(
                    RED
            );

            statusLabel.setText(
                    exception.getMessage()
            );
        }
    }

    private long parseCashStack(
            String value
    )
    {
        if (
                value == null
                        || value.trim().isEmpty()
        )
        {
            throw new IllegalArgumentException(
                    "Enter your cash stack"
            );
        }

        String cleaned =
                value
                        .toLowerCase()
                        .replace(",", "")
                        .replace(" ", "")
                        .replace("_", "");

        BigDecimal multiplier =
                BigDecimal.ONE;

        if (cleaned.endsWith("k"))
        {
            multiplier =
                    BigDecimal.valueOf(
                            1_000L
                    );

            cleaned =
                    cleaned.substring(
                            0,
                            cleaned.length() - 1
                    );
        }
        else if (cleaned.endsWith("m"))
        {
            multiplier =
                    BigDecimal.valueOf(
                            1_000_000L
                    );

            cleaned =
                    cleaned.substring(
                            0,
                            cleaned.length() - 1
                    );
        }
        else if (cleaned.endsWith("b"))
        {
            multiplier =
                    BigDecimal.valueOf(
                            1_000_000_000L
                    );

            cleaned =
                    cleaned.substring(
                            0,
                            cleaned.length() - 1
                    );
        }

        try
        {
            long cash =
                    new BigDecimal(
                            cleaned
                    )
                            .multiply(
                                    multiplier
                            )
                            .longValueExact();

            if (cash < 1)
            {
                throw new IllegalArgumentException(
                        "Cash stack must be above 0"
                );
            }

            if (cash > MAX_CASH_STACK)
            {
                throw new IllegalArgumentException(
                        "Maximum cash stack is 2.147B"
                );
            }

            return cash;
        }
        catch (
                ArithmeticException
                | NumberFormatException exception
        )
        {
            throw new IllegalArgumentException(
                    "Use for example: 25m, 500k or 100m"
            );
        }
    }

    private String friendlyCashInput(
            long cash
    )
    {
        if (
                cash >= 1_000_000_000L
                        && cash % 1_000_000_000L == 0
        )
        {
            return
                    (cash / 1_000_000_000L)
                            + "b";
        }

        if (
                cash >= 1_000_000L
                        && cash % 1_000_000L == 0
        )
        {
            return
                    (cash / 1_000_000L)
                            + "m";
        }

        if (
                cash >= 1_000L
                        && cash % 1_000L == 0
        )
        {
            return
                    (cash / 1_000L)
                            + "k";
        }

        return Long.toString(
                cash
        );
    }

    private void updateCashLabel()
    {
        cashLabel.setText(
                numberFormat.format(
                        currentCashStack
                )
                        + " gp"
        );
    }

    // ========================================================
    // STRONG / MORE
    // ========================================================

    private JPanel createResultModeButtons()
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
                BACKGROUND
        );

        strongButton.addActionListener(
                event ->
                        setResultMode(
                                "STRONG"
                        )
        );

        moreButton.addActionListener(
                event ->
                        setResultMode(
                                "MORE"
                        )
        );

        panel.add(
                strongButton
        );

        panel.add(
                moreButton
        );

        return panel;
    }

    private void setResultMode(
            String mode
    )
    {
        currentResultMode =
                mode;

        currentIndex = 0;

        updateModeLabel();

        updateResultModeButtons();

        loadRecommendations(
                false,
                false
        );

        restartAutoRefreshTimer();
    }

    private void updateResultModeButtons()
    {
        strongButton.setEnabled(
                !blockingLoad
                        && !currentResultMode.equals(
                        "STRONG"
                )
        );

        moreButton.setEnabled(
                !blockingLoad
                        && !currentResultMode.equals(
                        "MORE"
                )
        );
    }

    // ========================================================
    // FLIP TYPES
    // ========================================================

    private JPanel createFlipTypeButtons()
    {
        JPanel wrapper =
                new JPanel();

        wrapper.setLayout(
                new BoxLayout(
                        wrapper,
                        BoxLayout.Y_AXIS
                )
        );

        wrapper.setBackground(
                BACKGROUND
        );

        JPanel topRow =
                new JPanel(
                        new GridLayout(
                                1,
                                3,
                                4,
                                0
                        )
                );

        topRow.setBackground(
                BACKGROUND
        );

        JPanel bottomRow =
                new JPanel(
                        new GridLayout(
                                1,
                                2,
                                4,
                                0
                        )
                );

        bottomRow.setBackground(
                BACKGROUND
        );

        fastButton.addActionListener(
                event ->
                        setFlipType(
                                "FAST"
                        )
        );

        balancedButton.addActionListener(
                event ->
                        setFlipType(
                                "BALANCED"
                        )
        );

        slowButton.addActionListener(
                event ->
                        setFlipType(
                                "SLOW"
                        )
        );

        highProfitButton.addActionListener(
                event ->
                        setFlipType(
                                "HIGH_PROFIT"
                        )
        );

        allButton.addActionListener(
                event ->
                        setFlipType(
                                "ALL"
                        )
        );

        topRow.add(
                fastButton
        );

        topRow.add(
                balancedButton
        );

        topRow.add(
                slowButton
        );

        bottomRow.add(
                highProfitButton
        );

        bottomRow.add(
                allButton
        );

        wrapper.add(
                topRow
        );

        wrapper.add(
                Box.createVerticalStrut(
                        4
                )
        );

        wrapper.add(
                bottomRow
        );

        return wrapper;
    }

    private void setFlipType(
            String flipType
    )
    {
        currentFlipType =
                flipType;

        currentIndex = 0;

        updateFlipTypeButtons();

        loadRecommendations(
                false,
                false
        );

        restartAutoRefreshTimer();
    }

    private void updateFlipTypeButtons()
    {
        fastButton.setEnabled(
                !blockingLoad
                        && !currentFlipType.equals(
                        "FAST"
                )
        );

        balancedButton.setEnabled(
                !blockingLoad
                        && !currentFlipType.equals(
                        "BALANCED"
                )
        );

        slowButton.setEnabled(
                !blockingLoad
                        && !currentFlipType.equals(
                        "SLOW"
                )
        );

        highProfitButton.setEnabled(
                !blockingLoad
                        && !currentFlipType.equals(
                        "HIGH_PROFIT"
                )
        );

        allButton.setEnabled(
                !blockingLoad
                        && !currentFlipType.equals(
                        "ALL"
                )
        );
    }

    // ========================================================
    // MAIN CARD
    // ========================================================

    private JPanel createMainSection()
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
                                new Color(
                                        65,
                                        65,
                                        65
                                )
                        ),
                        BorderFactory.createEmptyBorder(
                                12,
                                12,
                                12,
                                12
                        )
                )
        );

        modeLabel.setForeground(
                MUTED
        );

        modeLabel.setFont(
                modeLabel.getFont()
                        .deriveFont(
                                Font.BOLD,
                                10f
                        )
        );

        card.add(
                modeLabel
        );

        card.add(
                Box.createVerticalStrut(
                        5
                )
        );

        itemNameLabel.setForeground(
                TEXT
        );

        itemNameLabel.setFont(
                itemNameLabel
                        .getFont()
                        .deriveFont(
                                Font.BOLD,
                                16f
                        )
        );

        card.add(
                itemNameLabel
        );

        card.add(
                Box.createVerticalStrut(
                        12
                )
        );

        card.add(
                sectionTitle(
                        "PRICE"
                )
        );

        card.add(
                Box.createVerticalStrut(
                        5
                )
        );

        prepareNormalLabel(
                buyLabel
        );

        prepareNormalLabel(
                sellLabel
        );

        card.add(
                buyLabel
        );

        card.add(
                Box.createVerticalStrut(
                        3
                )
        );

        card.add(
                sellLabel
        );

        card.add(
                Box.createVerticalStrut(
                        14
                )
        );

        card.add(
                sectionTitle(
                        "EXPECTED NET RESULT"
                )
        );

        card.add(
                Box.createVerticalStrut(
                        5
                )
        );

        profitLabel.setForeground(
                GREEN
        );

        profitLabel.setFont(
                profitLabel.getFont()
                        .deriveFont(
                                Font.BOLD,
                                14f
                        )
        );

        quantityLabel.setForeground(
                TEXT
        );

        card.add(
                profitLabel
        );

        card.add(
                Box.createVerticalStrut(
                        4
                )
        );

        card.add(
                quantityLabel
        );

        card.add(
                Box.createVerticalStrut(
                        14
                )
        );

        card.add(
                sectionTitle(
                        "FLIP QUALITY"
                )
        );

        card.add(
                Box.createVerticalStrut(
                        5
                )
        );

        prepareNormalLabel(
                riskLabel
        );

        prepareNormalLabel(
                speedLabel
        );

        prepareNormalLabel(
                stabilityLabel
        );

        card.add(
                riskLabel
        );

        card.add(
                Box.createVerticalStrut(
                        3
                )
        );

        card.add(
                speedLabel
        );

        card.add(
                Box.createVerticalStrut(
                        3
                )
        );

        card.add(
                stabilityLabel
        );

        card.add(
                Box.createVerticalStrut(
                        16
                )
        );

        scoreLabel.setForeground(
                GOLD
        );

        scoreLabel.setFont(
                scoreLabel.getFont()
                        .deriveFont(
                                Font.BOLD,
                                16f
                        )
        );

        card.add(
                scoreLabel
        );

        card.add(
                Box.createVerticalStrut(
                        10
                )
        );

        startActiveFlipButton.setEnabled(
                false
        );

        startActiveFlipButton.addActionListener(
                event ->
                        startCurrentFlip()
        );

        card.add(
                startActiveFlipButton
        );

        card.add(
                Box.createVerticalStrut(
                        8
                )
        );

        createAdvancedPanel();

        card.add(
                advancedButton
        );

        card.add(
                Box.createVerticalStrut(
                        8
                )
        );

        card.add(
                advancedPanel
        );

        card.add(
                Box.createVerticalStrut(
                        8
                )
        );

        updatedLabel.setForeground(
                MUTED
        );

        updatedLabel.setFont(
                updatedLabel.getFont()
                        .deriveFont(
                                10f
                        )
        );

        card.add(
                updatedLabel
        );

        card.add(
                Box.createVerticalStrut(
                        5
                )
        );

        statusLabel.setForeground(
                MUTED
        );

        statusLabel.setFont(
                statusLabel.getFont()
                        .deriveFont(
                                10f
                        )
        );

        card.add(
                statusLabel
        );

        return card;
    }

    // ========================================================
    // ADVANCED
    // ========================================================

    private void createAdvancedPanel()
    {
        advancedPanel.setLayout(
                new BoxLayout(
                        advancedPanel,
                        BoxLayout.Y_AXIS
                )
        );

        advancedPanel.setBackground(
                CARD_BACKGROUND
        );

        advancedButton.addActionListener(
                event ->
                        toggleAdvancedDetails()
        );

        JLabel[] labels = {
                netProfitPerItemLabel,
                grossProfitPerItemLabel,
                taxPerItemLabel,
                totalTaxLabel,
                grossExpectedProfitLabel,
                breakEvenLabel,
                netSellPriceLabel,
                roiLabel,
                grossRoiLabel,
                confidenceLabel,
                liquidityLabel,
                gpNeededLabel,
                capitalUsedLabel,
                cashAllowsLabel,
                buyLimitLabel,
                liquidityQtyLabel
        };

        for (JLabel label : labels)
        {
            prepareAdvancedLabel(
                    label
            );
        }

        advancedPanel.add(
                sectionTitle(
                        "ADVANCED DETAILS"
                )
        );

        advancedPanel.add(
                Box.createVerticalStrut(
                        5
                )
        );

        addAdvancedLabel(
                netProfitPerItemLabel
        );

        addAdvancedLabel(
                grossProfitPerItemLabel
        );

        addAdvancedLabel(
                taxPerItemLabel
        );

        addAdvancedLabel(
                totalTaxLabel
        );

        addAdvancedLabel(
                grossExpectedProfitLabel
        );

        addAdvancedLabel(
                breakEvenLabel
        );

        addAdvancedLabel(
                netSellPriceLabel
        );

        addAdvancedLabel(
                roiLabel
        );

        addAdvancedLabel(
                grossRoiLabel
        );

        addAdvancedLabel(
                confidenceLabel
        );

        addAdvancedLabel(
                liquidityLabel
        );

        addAdvancedLabel(
                gpNeededLabel
        );

        addAdvancedLabel(
                capitalUsedLabel
        );

        addAdvancedLabel(
                cashAllowsLabel
        );

        addAdvancedLabel(
                buyLimitLabel
        );

        addAdvancedLabel(
                liquidityQtyLabel
        );

        advancedPanel.setVisible(
                false
        );
    }

    private void addAdvancedLabel(
            JLabel label
    )
    {
        advancedPanel.add(
                label
        );

        advancedPanel.add(
                Box.createVerticalStrut(
                        3
                )
        );
    }

    private void prepareAdvancedLabel(
            JLabel label
    )
    {
        label.setForeground(
                MUTED
        );

        label.setFont(
                label.getFont()
                        .deriveFont(
                                11f
                        )
        );
    }

    private void toggleAdvancedDetails()
    {
        advancedVisible =
                !advancedVisible;

        advancedPanel.setVisible(
                advancedVisible
        );

        advancedButton.setText(
                advancedVisible
                        ? "Hide Advanced Details"
                        : "Show Advanced Details"
        );

        revalidate();

        repaint();
    }

    private JLabel sectionTitle(
            String text
    )
    {
        JLabel label =
                new JLabel(
                        text
                );

        label.setForeground(
                MUTED
        );

        label.setFont(
                label.getFont()
                        .deriveFont(
                                Font.BOLD,
                                10f
                        )
        );

        return label;
    }

    private void prepareNormalLabel(
            JLabel label
    )
    {
        label.setForeground(
                TEXT
        );
    }

    // ========================================================
    // BOTTOM
    // ========================================================

    private JPanel createBottomSection()
    {
        JPanel wrapper =
                new JPanel();

        wrapper.setLayout(
                new BoxLayout(
                        wrapper,
                        BoxLayout.Y_AXIS
                )
        );

        wrapper.setBackground(
                BACKGROUND
        );

        JPanel navigation =
                new JPanel(
                        new GridLayout(
                                1,
                                2,
                                5,
                                0
                        )
                );

        navigation.setBackground(
                BACKGROUND
        );

        previousButton.addActionListener(
                event ->
                        previousFlip()
        );

        nextButton.addActionListener(
                event ->
                        nextFlip()
        );

        refreshButton.addActionListener(
                event ->
                {
                    loadRecommendations(
                            true,
                            true
                    );

                    restartAutoRefreshTimer();
                }
        );

        navigation.add(
                previousButton
        );

        navigation.add(
                nextButton
        );

        wrapper.add(
                navigation
        );

        wrapper.add(
                Box.createVerticalStrut(
                        6
                )
        );

        wrapper.add(
                refreshButton
        );

        wrapper.add(
                Box.createVerticalStrut(
                        10
                )
        );

        wrapper.add(
                activeFlipsPanel
        );

        wrapper.add(
                Box.createVerticalStrut(
                        10
                )
        );

        wrapper.add(
                profitTrackerPanel
        );

        return wrapper;
    }

    // ========================================================
    // ACTIVE FLIPS
    // ========================================================

    private void startCurrentFlip()
    {
        if (
                recommendations.isEmpty()
                        || currentIndex < 0
                        || currentIndex >= recommendations.size()
        )
        {
            return;
        }

        RuneRadarApiClient.Recommendation item =
                recommendations.get(
                        currentIndex
                );

        if (hasActiveFlipForItem(item.getId()))
        {
            updateStartActiveFlipButton();

            return;
        }

        activeFlipStore.addRecommendation(
                item.getId(),
                item.getName(),
                item.getBuy(),
                item.getSell(),
                Math.toIntExact(
                        item.getRecommendedQty()
                ),
                item.getNetExpectedProfit()
        );

        activeFlipsPanel.refresh();

        updateStartActiveFlipButton();

        updatedLabel.setText(
                "Added to Active Flips • NET target after GE tax"
        );

        revalidate();

        repaint();
    }

    private boolean hasActiveFlipForItem(
            int itemId
    )
    {
        for (
                ActiveFlipStore.ActiveFlip flip
                : activeFlipStore.getAll()
        )
        {
            if (
                    flip != null
                            && !flip.isCompleted()
                            && flip.getItemId() == itemId
            )
            {
                return true;
            }
        }

        return false;
    }

    private void updateStartActiveFlipButton()
    {
        if (
                recommendations.isEmpty()
                        || currentIndex < 0
                        || currentIndex >= recommendations.size()
        )
        {
            startActiveFlipButton.setText(
                    "Start Active Flip"
            );

            startActiveFlipButton.setEnabled(
                    false
            );

            return;
        }

        RuneRadarApiClient.Recommendation item =
                recommendations.get(
                        currentIndex
                );

        boolean alreadyActive =
                hasActiveFlipForItem(
                        item.getId()
                );

        startActiveFlipButton.setText(
                alreadyActive
                        ? "Already Active"
                        : "Start Active Flip"
        );

        startActiveFlipButton.setEnabled(
                !blockingLoad
                        && !alreadyActive
        );
    }

    // ========================================================
    // API LOADING
    // ========================================================

    private void loadRecommendations(
            boolean preserveSelection,
            boolean quietRefresh
    )
    {
        final int thisRequest =
                ++requestNumber;

        final Integer selectedItemId =
                getSelectedItemId();

        final String selectedItemName =
                getSelectedItemName();

        requestInProgress =
                true;

        blockingLoad =
                !quietRefresh;

        lastRefreshFailed =
                false;

        updateResultModeButtons();

        updateFlipTypeButtons();

        updateNavigationButtons();

        applyCashButton.setEnabled(
                !blockingLoad
        );

        refreshButton.setEnabled(
                false
        );

        if (quietRefresh)
        {
            updatedLabel.setText(
                    "Refreshing market data..."
            );
        }
        else
        {
            setLoadingState();
        }

        final long cashStack =
                currentCashStack;

        final String requestedFlipType =
                currentFlipType;

        final String requestedResultMode =
                currentResultMode;

        Thread thread =
                new Thread(
                        () ->
                        {
                            try
                            {
                                RuneRadarApiClient.ApiResponse response;

                                if (
                                        requestedResultMode.equals(
                                                "MORE"
                                        )
                                )
                                {
                                    response =
                                            apiClient.getMoreOpportunities(
                                                    cashStack,
                                                    requestedFlipType,
                                                    RESULT_LIMIT
                                            );
                                }
                                else
                                {
                                    response =
                                            apiClient.getRecommendations(
                                                    cashStack,
                                                    requestedFlipType,
                                                    RESULT_LIMIT
                                            );
                                }

                                SwingUtilities.invokeLater(
                                        () ->
                                        {
                                            if (
                                                    thisRequest
                                                            != requestNumber
                                            )
                                            {
                                                return;
                                            }

                                            recommendations =
                                                    new ArrayList<>(
                                                            response.getRecommendations()
                                                    );

                                            updatedAt =
                                                    response.getUpdatedAt();

                                            requestInProgress =
                                                    false;

                                            blockingLoad =
                                                    false;

                                            lastRefreshFailed =
                                                    false;

                                            applyCashButton.setEnabled(
                                                    true
                                            );

                                            refreshButton.setEnabled(
                                                    true
                                            );

                                            updateResultModeButtons();

                                            updateFlipTypeButtons();

                                            if (preserveSelection)
                                            {
                                                restoreSelection(
                                                        selectedItemId,
                                                        selectedItemName
                                                );
                                            }
                                            else
                                            {
                                                currentIndex = 0;
                                            }

                                            showCurrentFlip();
                                        }
                                );
                            }
                            catch (
                                    Exception exception
                            )
                            {
                                SwingUtilities.invokeLater(
                                        () ->
                                        {
                                            if (
                                                    thisRequest
                                                            != requestNumber
                                            )
                                            {
                                                return;
                                            }

                                            requestInProgress =
                                                    false;

                                            blockingLoad =
                                                    false;

                                            applyCashButton.setEnabled(
                                                    true
                                            );

                                            refreshButton.setEnabled(
                                                    true
                                            );

                                            updateResultModeButtons();

                                            updateFlipTypeButtons();

                                            if (
                                                    recommendations.isEmpty()
                                            )
                                            {
                                                showInitialApiError(
                                                        exception.getMessage()
                                                );
                                            }
                                            else
                                            {
                                                lastRefreshFailed =
                                                        true;

                                                showCurrentFlip();

                                                statusLabel.setForeground(
                                                        RED
                                                );

                                                statusLabel.setText(
                                                        "Refresh failed • showing last valid data"
                                                );
                                            }
                                        }
                                );
                            }
                        }
                );

        thread.setName(
                "RuneRadar-API"
        );

        thread.setDaemon(
                true
        );

        thread.start();
    }

    // ========================================================
    // LOADING
    // ========================================================

    private void setLoadingState()
    {
        updateModeLabel();

        itemNameLabel.setText(
                "Loading..."
        );

        buyLabel.setText(
                "Buy: -"
        );

        sellLabel.setText(
                "Sell: -"
        );

        profitLabel.setText(
                "Expected net profit: -"
        );

        quantityLabel.setText(
                "Recommended buy qty: -"
        );

        riskLabel.setText(
                "Risk: -"
        );

        speedLabel.setText(
                "Trading speed: -"
        );

        stabilityLabel.setText(
                "Price stability: -"
        );

        scoreLabel.setText(
                "RuneRadar: -"
        );

        clearAdvancedDetails();

        updatedLabel.setText(
                "Fetching live market data..."
        );

        statusLabel.setForeground(
                MUTED
        );

        statusLabel.setText(
                ""
        );
    }

    private void showInitialApiError(
            String message
    )
    {
        recommendations.clear();

        itemNameLabel.setText(
                "RuneRadar API offline"
        );

        buyLabel.setText(
                "Buy: -"
        );

        sellLabel.setText(
                "Sell: -"
        );

        profitLabel.setText(
                "Expected net profit: -"
        );

        quantityLabel.setText(
                "Recommended buy qty: -"
        );

        riskLabel.setText(
                "Risk: -"
        );

        speedLabel.setText(
                "Trading speed: -"
        );

        stabilityLabel.setText(
                "Price stability: -"
        );

        scoreLabel.setText(
                "RuneRadar: -"
        );

        clearAdvancedDetails();

        updatedLabel.setText(
                "Could not reach hosted RuneRadar API."
        );

        statusLabel.setForeground(
                RED
        );

        statusLabel.setText(
                message == null
                        ? "Connection failed"
                        : message
        );

        updateNavigationButtons();
    }

    public RuneRadarApiClient.Recommendation findLoadedRecommendationByItemId(
            int itemId
    )
    {
        for (
                RuneRadarApiClient.Recommendation recommendation
                : recommendations
        )
        {
            if (
                    recommendation != null
                            && recommendation.getId() == itemId
            )
            {
                return recommendation;
            }
        }

        return null;
    }

    // ========================================================
    // SELECTION
    // ========================================================

    private Integer getSelectedItemId()
    {
        if (
                recommendations.isEmpty()
                        || currentIndex < 0
                        || currentIndex >= recommendations.size()
        )
        {
            return null;
        }

        return recommendations
                .get(
                        currentIndex
                )
                .getId();
    }

    private String getSelectedItemName()
    {
        if (
                recommendations.isEmpty()
                        || currentIndex < 0
                        || currentIndex >= recommendations.size()
        )
        {
            return null;
        }

        return recommendations
                .get(
                        currentIndex
                )
                .getName();
    }

    private void restoreSelection(
            Integer itemId,
            String itemName
    )
    {
        if (recommendations.isEmpty())
        {
            currentIndex = 0;

            return;
        }

        if (itemId != null)
        {
            for (
                    int index = 0;
                    index < recommendations.size();
                    index++
            )
            {
                if (
                        recommendations
                                .get(index)
                                .getId()
                                == itemId
                )
                {
                    currentIndex =
                            index;

                    return;
                }
            }
        }

        if (itemName != null)
        {
            for (
                    int index = 0;
                    index < recommendations.size();
                    index++
            )
            {
                if (
                        itemName.equalsIgnoreCase(
                                recommendations
                                        .get(index)
                                        .getName()
                        )
                )
                {
                    currentIndex =
                            index;

                    return;
                }
            }
        }

        currentIndex = 0;
    }

    // ========================================================
    // DISPLAY
    // ========================================================

    private void showCurrentFlip()
    {
        updateModeLabel();

        if (recommendations.isEmpty())
        {
            itemNameLabel.setText(
                    currentResultMode.equals(
                            "MORE"
                    )
                            ? "No extra opportunities"
                            : "No strong flips found"
            );

            buyLabel.setText(
                    "Buy: -"
            );

            sellLabel.setText(
                    "Sell: -"
            );

            profitLabel.setText(
                    "Expected net profit: -"
            );

            quantityLabel.setText(
                    "Recommended buy qty: -"
            );

            riskLabel.setText(
                    "Risk: -"
            );

            speedLabel.setText(
                    "Trading speed: -"
            );

            stabilityLabel.setText(
                    "Price stability: -"
            );

            scoreLabel.setText(
                    "RuneRadar: -"
            );

            clearAdvancedDetails();

            updatedLabel.setText(
                    "No matching opportunities right now."
            );

            statusLabel.setForeground(
                    MUTED
            );

            statusLabel.setText(
                    "Try another flip type"
            );

            updateNavigationButtons();

            updateStartActiveFlipButton();

            return;
        }

        if (currentIndex < 0)
        {
            currentIndex =
                    recommendations.size()
                            - 1;
        }

        if (
                currentIndex
                        >= recommendations.size()
        )
        {
            currentIndex = 0;
        }

        RuneRadarApiClient.Recommendation item =
                recommendations.get(
                        currentIndex
                );

        itemNameLabel.setText(
                item.getName()
        );

        buyLabel.setText(
                "Buy: "
                        + formatGp(
                        item.getBuy()
                )
        );

        sellLabel.setText(
                "Sell: "
                        + formatGp(
                        item.getSell()
                )
        );

        profitLabel.setText(
                "Expected net profit: +"
                        + formatGp(
                        item.getNetExpectedProfit()
                )
        );

        quantityLabel.setText(
                "Recommended buy qty: "
                        + numberFormat.format(
                        item.getRecommendedQty()
                )
        );

        riskLabel.setText(
                "Risk: "
                        + safeText(
                        item.getRisk()
                )
        );

        speedLabel.setText(
                "Trading speed: "
                        + friendlyTradingSpeed(
                        item.getTradingSpeed()
                )
        );

        stabilityLabel.setText(
                "Price stability: "
                        + friendlyStability(
                        item.getPriceStability()
                )
        );

        scoreLabel.setText(
                "RuneRadar: "
                        + item.getScore()
                        + "/100"
        );

        netProfitPerItemLabel.setText(
                "Net profit/item: +"
                        + formatGp(
                        item.getNetProfitPerItem()
                )
        );

        grossProfitPerItemLabel.setText(
                "Gross profit/item: +"
                        + formatGp(
                        item.getGrossProfitPerItem()
                )
        );

        taxPerItemLabel.setText(
                "GE tax/item: -"
                        + formatGp(
                        item.getTaxPerItem()
                )
        );

        totalTaxLabel.setText(
                "Total GE tax: -"
                        + formatGp(
                        item.getTotalTax()
                )
        );

        grossExpectedProfitLabel.setText(
                "Gross expected profit: +"
                        + formatGp(
                        item.getGrossExpectedProfit()
                )
        );

        breakEvenLabel.setText(
                "Break-even sell: "
                        + formatGp(
                        item.getBreakEvenSell()
                )
        );

        netSellPriceLabel.setText(
                "Net sell after tax: "
                        + formatGp(
                        item.getNetSellPrice()
                )
        );

        roiLabel.setText(
                "Net ROI: "
                        + String.format(
                        "%.3f%%",
                        item.getRoi()
                )
        );

        grossRoiLabel.setText(
                "Gross ROI: "
                        + String.format(
                        "%.3f%%",
                        item.getGrossRoi()
                )
        );

        confidenceLabel.setText(
                "Confidence: "
                        + safeText(
                        item.getConfidence()
                )
        );

        liquidityLabel.setText(
                "Liquidity: "
                        + safeText(
                        item.getLiquidity()
                )
        );

        gpNeededLabel.setText(
                "GP needed: "
                        + formatGp(
                        item.getGpNeeded()
                )
        );

        capitalUsedLabel.setText(
                "Cash used: "
                        + String.format(
                        "%.2f%%",
                        item.getCapitalUsedPercent()
                )
        );

        cashAllowsLabel.setText(
                "Cash allows: "
                        + numberFormat.format(
                        item.getCashAllows()
                )
        );

        if (item.isBuyLimitKnown())
        {
            buyLimitLabel.setText(
                    "GE buy limit: "
                            + numberFormat.format(
                            item.getBuyLimit()
                    )
            );
        }
        else
        {
            buyLimitLabel.setText(
                    "GE buy limit: Unknown"
            );
        }

        liquidityQtyLabel.setText(
                "Liquidity qty cap: "
                        + numberFormat.format(
                        item.getLiquidityQuantity()
                )
        );

        if (
                currentFlipType.equals(
                        "HIGH_PROFIT"
                )
        )
        {
            updatedLabel.setText(
                    "High Profit • NET after GE tax • auto-refresh 60s"
            );
        }
        else if (
                currentFlipType.equals(
                        "ALL"
                )
        )
        {
            updatedLabel.setText(
                    "All opportunities • NET after GE tax • auto-refresh 60s"
            );
        }
        else if (
                currentResultMode.equals(
                        "MORE"
                )
        )
        {
            updatedLabel.setText(
                    "More Opportunities • NET after GE tax • auto-refresh 60s"
            );
        }
        else
        {
            updatedLabel.setText(
                    "Strong recommendation • NET after GE tax • auto-refresh 60s"
            );
        }

        updateLiveStatus();

        updateNavigationButtons();

        updateStartActiveFlipButton();

        revalidate();

        repaint();
    }

    // ========================================================
    // MODE LABEL
    // ========================================================

    private void updateModeLabel()
    {
        String prefix =
                currentResultMode.equals(
                        "MORE"
                )
                        ? "MORE"
                        : "STRONG";

        String type;

        if (
                currentFlipType.equals(
                        "HIGH_PROFIT"
                )
        )
        {
            type =
                    "HIGH PROFIT";
        }
        else
        {
            type =
                    currentFlipType;
        }

        modeLabel.setText(
                prefix
                        + " • "
                        + type
        );

        if (
                currentResultMode.equals(
                        "MORE"
                )
        )
        {
            modeLabel.setForeground(
                    MUTED
            );
        }
        else
        {
            modeLabel.setForeground(
                    GREEN
            );
        }
    }

    // ========================================================
    // ADVANCED CLEAR
    // ========================================================

    private void clearAdvancedDetails()
    {
        netProfitPerItemLabel.setText(
                "Net profit/item: -"
        );

        grossProfitPerItemLabel.setText(
                "Gross profit/item: -"
        );

        taxPerItemLabel.setText(
                "GE tax/item: -"
        );

        totalTaxLabel.setText(
                "Total GE tax: -"
        );

        grossExpectedProfitLabel.setText(
                "Gross expected profit: -"
        );

        breakEvenLabel.setText(
                "Break-even sell: -"
        );

        netSellPriceLabel.setText(
                "Net sell after tax: -"
        );

        roiLabel.setText(
                "Net ROI: -"
        );

        grossRoiLabel.setText(
                "Gross ROI: -"
        );

        confidenceLabel.setText(
                "Confidence: -"
        );

        liquidityLabel.setText(
                "Liquidity: -"
        );

        gpNeededLabel.setText(
                "GP needed: -"
        );

        capitalUsedLabel.setText(
                "Cash used: -"
        );

        cashAllowsLabel.setText(
                "Cash allows: -"
        );

        buyLimitLabel.setText(
                "GE buy limit: -"
        );

        liquidityQtyLabel.setText(
                "Liquidity qty cap: -"
        );
    }

    // ========================================================
    // LIVE STATUS
    // ========================================================

    private void updateLiveStatus()
    {
        if (
                recommendations.isEmpty()
                        || blockingLoad
        )
        {
            return;
        }

        if (lastRefreshFailed)
        {
            statusLabel.setForeground(
                    RED
            );

            statusLabel.setText(
                    "Last valid data: "
                            + secondsAgo()
                            + " sec ago • refresh failed"
            );

            return;
        }

        statusLabel.setForeground(
                MUTED
        );

        statusLabel.setText(
                "Result "
                        + (currentIndex + 1)
                        + " of "
                        + recommendations.size()
                        + " • updated "
                        + secondsAgo()
                        + " sec ago"
        );
    }

    // ========================================================
    // NAVIGATION
    // ========================================================

    private void updateNavigationButtons()
    {
        boolean multipleResults =
                !blockingLoad
                        && recommendations.size() > 1;

        previousButton.setEnabled(
                multipleResults
        );

        nextButton.setEnabled(
                multipleResults
        );
    }

    private void previousFlip()
    {
        if (
                blockingLoad
                        || recommendations.size() <= 1
        )
        {
            return;
        }

        currentIndex--;

        if (currentIndex < 0)
        {
            currentIndex =
                    recommendations.size()
                            - 1;
        }

        showCurrentFlip();
    }

    private void nextFlip()
    {
        if (
                blockingLoad
                        || recommendations.size() <= 1
        )
        {
            return;
        }

        currentIndex++;

        if (
                currentIndex
                        >= recommendations.size()
        )
        {
            currentIndex = 0;
        }

        showCurrentFlip();
    }

    // ========================================================
    // AUTO REFRESH
    // ========================================================

    private void restartAutoRefreshTimer()
    {
        autoRefreshTimer.restart();
    }

    // ========================================================
    // FORMATTING
    // ========================================================

    private String formatGp(
            long amount
    )
    {
        return numberFormat.format(
                amount
        ) + " gp";
    }

    private String safeText(
            String value
    )
    {
        if (
                value == null
                        || value.trim().isEmpty()
        )
        {
            return "-";
        }

        return value;
    }

    private String friendlyTradingSpeed(
            String value
    )
    {
        if (value == null)
        {
            return "-";
        }

        if (
                value.equalsIgnoreCase(
                        "FAST"
                )
        )
        {
            return "FAST";
        }

        if (
                value.equalsIgnoreCase(
                        "BALANCED"
                )
        )
        {
            return "BALANCED";
        }

        if (
                value.equalsIgnoreCase(
                        "SLOW"
                )
        )
        {
            return "SLOW";
        }

        return value;
    }

    private String friendlyStability(
            String value
    )
    {
        if (value == null)
        {
            return "-";
        }

        if (
                value.equalsIgnoreCase(
                        "STABLE"
                )
        )
        {
            return "GOOD";
        }

        if (
                value.equalsIgnoreCase(
                        "OK"
                )
        )
        {
            return "OK";
        }

        return "POOR";
    }

    private long secondsAgo()
    {
        if (updatedAt <= 0)
        {
            return 0;
        }

        long now =
                Instant.now()
                        .getEpochSecond();

        return Math.max(
                0,
                now - updatedAt
        );
    }
}
