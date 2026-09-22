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

    private static final int RESULT_LIMIT = 10;

    private static final int AUTO_REFRESH_SECONDS = 60;

    private static final long MAX_CASH_STACK =
            2_147_483_647L;

    private final RuneRadarConfig config;

    private final RuneRadarApiClient apiClient =
            new RuneRadarApiClient();

    private final NumberFormat numberFormat =
            NumberFormat.getIntegerInstance();

    // ========================================================
    // CASH STACK
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
            new JButton("Strong Flips");

    private final JButton moreButton =
            new JButton("More Opportunities");

    private String currentResultMode =
            "STRONG";

    // ========================================================
    // FLIP TYPE
    // ========================================================

    private final JButton fastButton =
            new JButton("Fast");

    private final JButton balancedButton =
            new JButton("Balanced");

    private final JButton slowButton =
            new JButton("Slow");

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

    private final JLabel roiLabel =
            new JLabel();

    private final JLabel profitPerItemLabel =
            new JLabel();

    private final JLabel confidenceLabel =
            new JLabel();

    private final JLabel liquidityLabel =
            new JLabel();

    private final JLabel gpNeededLabel =
            new JLabel();

    private final JLabel capitalUsedLabel =
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

    private final ProfitTrackerPanel profitTrackerPanel =
            new ProfitTrackerPanel();

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
        this.config = config;

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
    // TOP SECTION
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
    // CASH STACK
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
    // FAST / BALANCED / SLOW
    // ========================================================

    private JPanel createFlipTypeButtons()
    {
        JPanel panel =
                new JPanel(
                        new GridLayout(
                                1,
                                3,
                                4,
                                0
                        )
                );

        panel.setBackground(
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

        panel.add(
                fastButton
        );

        panel.add(
                balancedButton
        );

        panel.add(
                slowButton
        );

        return panel;
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
    }

    // ========================================================
    // MAIN RESULT CARD
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
                        "EXPECTED RESULT"
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
    // ADVANCED DETAILS
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

        prepareAdvancedLabel(
                roiLabel
        );

        prepareAdvancedLabel(
                profitPerItemLabel
        );

        prepareAdvancedLabel(
                confidenceLabel
        );

        prepareAdvancedLabel(
                liquidityLabel
        );

        prepareAdvancedLabel(
                gpNeededLabel
        );

        prepareAdvancedLabel(
                capitalUsedLabel
        );

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

        advancedPanel.add(
                roiLabel
        );

        advancedPanel.add(
                Box.createVerticalStrut(
                        3
                )
        );

        advancedPanel.add(
                profitPerItemLabel
        );

        advancedPanel.add(
                Box.createVerticalStrut(
                        3
                )
        );

        advancedPanel.add(
                confidenceLabel
        );

        advancedPanel.add(
                Box.createVerticalStrut(
                        3
                )
        );

        advancedPanel.add(
                liquidityLabel
        );

        advancedPanel.add(
                Box.createVerticalStrut(
                        3
                )
        );

        advancedPanel.add(
                gpNeededLabel
        );

        advancedPanel.add(
                Box.createVerticalStrut(
                        3
                )
        );

        advancedPanel.add(
                capitalUsedLabel
        );

        advancedPanel.setVisible(
                false
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
    // BOTTOM SECTION
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

        // ----------------------------------------------------
        // OPTIONAL PROFIT TRACKER
        // ----------------------------------------------------

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
                                                            response
                                                                    .getRecommendations()
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
    // LOADING / ERRORS
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
                "Expected profit: -"
        );

        quantityLabel.setText(
                "You can buy: -"
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
                "Expected profit: -"
        );

        quantityLabel.setText(
                "You can buy: -"
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
                "Start the RuneRadar Python API."
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

    // ========================================================
    // SELECTION PRESERVATION
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

        if (
                currentIndex
                        >= recommendations.size()
        )
        {
            currentIndex =
                    recommendations.size()
                            - 1;
        }

        if (currentIndex < 0)
        {
            currentIndex = 0;
        }
    }

    // ========================================================
    // DISPLAY RESULT
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
                    "Expected profit: -"
            );

            quantityLabel.setText(
                    "You can buy: -"
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
                    currentResultMode.equals(
                            "MORE"
                    )
                            ? "No extra opportunities right now."
                            : "No strong flips right now."
            );

            statusLabel.setForeground(
                    MUTED
            );

            statusLabel.setText(
                    "Try another flip type"
            );

            updateNavigationButtons();

            return;
        }

        if (currentIndex < 0)
        {
            currentIndex =
                    recommendations.size() - 1;
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
                "Expected profit: +"
                        + formatGp(
                        item.getExpectedProfit()
                )
        );

        quantityLabel.setText(
                "You can buy: "
                        + numberFormat.format(
                        item.getQuantity()
                )
        );

        riskLabel.setText(
                "Risk: "
                        + item.getRisk()
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

        roiLabel.setText(
                "ROI: "
                        + String.format(
                        "%.2f%%",
                        item.getRoi()
                )
        );

        profitPerItemLabel.setText(
                "Profit per item: "
                        + formatGp(
                        item.getProfitPerItem()
                )
        );

        confidenceLabel.setText(
                "Confidence: "
                        + item.getConfidence()
        );

        liquidityLabel.setText(
                "Liquidity: "
                        + item.getLiquidity()
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
                        "%.1f%%",
                        item.getCapitalUsedPercent()
                )
        );

        updatedLabel.setText(
                currentResultMode.equals(
                        "MORE"
                )
                        ? "More Opportunities • score 60+ • auto-refresh 60s"
                        : "Strong recommendation • auto-refresh 60s"
        );

        updateLiveStatus();

        updateNavigationButtons();

        revalidate();

        repaint();
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

    private void updateModeLabel()
    {
        if (
                currentResultMode.equals(
                        "MORE"
                )
        )
        {
            modeLabel.setText(
                    "MORE OPPORTUNITIES"
            );

            modeLabel.setForeground(
                    MUTED
            );
        }
        else
        {
            modeLabel.setText(
                    "STRONG FLIP"
            );

            modeLabel.setForeground(
                    GREEN
            );
        }
    }

    private void clearAdvancedDetails()
    {
        roiLabel.setText(
                "ROI: -"
        );

        profitPerItemLabel.setText(
                "Profit per item: -"
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
    }

    // ========================================================
    // PREVIOUS / NEXT
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
                    recommendations.size() - 1;
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
            return "GOOD";
        }

        return "SLOW";
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
