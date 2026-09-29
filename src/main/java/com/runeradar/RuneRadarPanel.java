package com.runeradar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

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

    private static final int MAX_RESULT_LIMIT = 50;

    private static final String CONFIG_GROUP = "runeradar";

    private static final String RESULT_MODE_KEY = "recommendationResultMode";

    private static final String FLIP_TYPE_KEY = "recommendationFlipType";

    private static final String SORT_MODE_KEY = "recommendationSortMode";

    private static final String ADVANCED_VISIBLE_KEY = "advancedDetailsVisible";

    private static final String AUTH_DEVICE_ID_KEY = "authDeviceId";

    private static final String AUTH_SESSION_TOKEN_KEY = "authSessionToken";

    private static final int AUTO_REFRESH_SECONDS = 60;

    private static final long MAX_CASH_STACK =
            2_147_483_647L;

    private final RuneRadarConfig config;

    private final ConfigManager configManager;

    private final RuneRadarApiClient apiClient =
            new RuneRadarApiClient();

    private final NumberFormat numberFormat =
            NumberFormat.getIntegerInstance();

    // ========================================================
    // ACCOUNT
    // ========================================================

    private final JPanel accountContent =
            new JPanel();

    private final JLabel accountStatusLabel =
            new JLabel();

    private final JTextField emailInput =
            new JTextField();

    private final JTextField verificationCodeInput =
            new JTextField();

    private final JButton requestCodeButton =
            new JButton("Send code");

    private final JButton verifyCodeButton =
            new JButton("Verify & sign in");

    private String deviceId;

    private String sessionToken;

    private String accountEmail = "";

    private String accountPlan = "FREE";

    private List<RuneRadarApiClient.Device> accountDevices =
            new ArrayList<>();

    private int maximumDevices = 3;

    private boolean devicesVisible = false;

    private boolean accountBusy = false;

    // ========================================================
    // ACCOUNT
    // ========================================================

    private JPanel createAccountSection()
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
                CARD_BACKGROUND
        );

        wrapper.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(70, 70, 70)
                        ),
                        BorderFactory.createEmptyBorder(
                                8,
                                8,
                                8,
                                8
                        )
                )
        );

        JLabel title =
                new JLabel(
                        "ACCOUNT"
                );

        title.setForeground(GOLD);
        title.setFont(
                title.getFont().deriveFont(
                        Font.BOLD,
                        12f
                )
        );

        accountContent.setLayout(
                new BoxLayout(
                        accountContent,
                        BoxLayout.Y_AXIS
                )
        );
        accountContent.setBackground(CARD_BACKGROUND);

        accountStatusLabel.setForeground(MUTED);
        accountStatusLabel.setFont(
                accountStatusLabel.getFont().deriveFont(
                        Font.BOLD,
                        13f
                )
        );

        emailInput.setToolTipText(
                "Email address for your RuneRadar account"
        );
        verificationCodeInput.setToolTipText(
                "Enter the 6-digit code from your email"
        );

        requestCodeButton.addActionListener(
                event -> requestLoginCode()
        );
        verifyCodeButton.addActionListener(
                event -> verifyLoginCode()
        );
        emailInput.addActionListener(
                event -> requestLoginCode()
        );
        verificationCodeInput.addActionListener(
                event -> verifyLoginCode()
        );

        wrapper.add(title);
        wrapper.add(Box.createVerticalStrut(7));
        wrapper.add(accountContent);
        wrapper.add(Box.createVerticalStrut(5));
        wrapper.add(accountStatusLabel);

        return wrapper;
    }

    private void renderAccountContent()
    {
        accountContent.removeAll();

        if (sessionToken.isEmpty() || accountEmail.isEmpty())
        {
            renderSignedOutAccount();
        }
        else
        {
            renderSignedInAccount();
        }

        accountContent.revalidate();
        accountContent.repaint();
        updateResultModeButtons();
        updateFlipTypeButtons();
    }

    private boolean hasProAccess()
    {
        return "PRO".equalsIgnoreCase(
                accountPlan
        );
    }

    private boolean resetFreeOnlySelections()
    {
        if (hasProAccess())
        {
            return false;
        }

        boolean changed = false;

        if ("MORE".equals(currentResultMode))
        {
            currentResultMode = "STRONG";
            persistSetting(
                    RESULT_MODE_KEY,
                    currentResultMode
            );
            changed = true;
        }

        if ("HIGH_PROFIT".equals(currentFlipType))
        {
            currentFlipType = "FAST";
            persistSetting(
                    FLIP_TYPE_KEY,
                    currentFlipType
            );
            changed = true;
        }

        if (changed)
        {
            currentIndex = 0;
            updateModeLabel();
            updateResultModeButtons();
            updateFlipTypeButtons();
        }

        return changed;
    }

    private void renderSignedOutAccount()
    {
        JLabel description =
                createAccountLabel(
                        "Sign in with a one-time email code."
                );

        emailInput.setEnabled(!accountBusy);
        verificationCodeInput.setEnabled(!accountBusy);
        requestCodeButton.setEnabled(!accountBusy);
        verifyCodeButton.setEnabled(!accountBusy);

        accountContent.add(description);
        accountContent.add(Box.createVerticalStrut(6));
        accountContent.add(emailInput);
        accountContent.add(Box.createVerticalStrut(5));
        accountContent.add(requestCodeButton);
        accountContent.add(Box.createVerticalStrut(7));
        accountContent.add(verificationCodeInput);
        accountContent.add(Box.createVerticalStrut(5));
        accountContent.add(verifyCodeButton);
    }

    private void renderSignedInAccount()
    {
        JLabel signedInLabel =
                createAccountLabel(
                        "Signed in"
                );

        signedInLabel.setForeground(MUTED);

        int atIndex =
                accountEmail.indexOf('@');

        String emailFirstLine =
                atIndex > 0
                        ? accountEmail.substring(0, atIndex)
                        : accountEmail;

        String emailSecondLine =
                atIndex > 0
                        ? accountEmail.substring(atIndex)
                        : "";

        JLabel emailLabel =
                createAccountLabel(
                        emailFirstLine
                );

        emailLabel.setToolTipText(accountEmail);

        JLabel emailDomainLabel =
                createAccountLabel(
                        emailSecondLine
                );

        emailDomainLabel.setToolTipText(accountEmail);

        boolean pro =
                "PRO".equalsIgnoreCase(
                        accountPlan
                );

        JLabel planLabel =
                createAccountLabel(
                        pro
                                ? "Plan: Pro active"
                                : "Plan: Free"
                );

        planLabel.setForeground(
                pro ? GREEN : TEXT
        );
        planLabel.setFont(
                planLabel.getFont().deriveFont(
                        Font.BOLD,
                        13f
                )
        );

        JButton planButton =
                new JButton(
                        pro
                                ? "Pro active"
                                : "Get Pro"
                );

        planButton.setEnabled(
                !pro && !accountBusy
        );

        if (!pro)
        {
            planButton.addActionListener(
                    event -> openProCheckout()
            );
        }

        JButton devicesButton =
                new JButton(
                        devicesVisible
                                ? "Hide"
                                : "Devices"
                );

        JLabel deviceCountLabel =
                createAccountLabel(
                        accountDevices.size()
                                + " of " + maximumDevices
                );

        deviceCountLabel.setForeground(MUTED);

        devicesButton.setEnabled(!accountBusy);
        devicesButton.addActionListener(
                event ->
                {
                    devicesVisible = !devicesVisible;
                    renderAccountContent();
                }
        );

        JButton logoutButton =
                new JButton("Log out");

        logoutButton.setEnabled(!accountBusy);
        logoutButton.addActionListener(
                event -> logoutAccount()
        );

        accountContent.add(signedInLabel);
        accountContent.add(Box.createVerticalStrut(2));
        accountContent.add(emailLabel);

        if (!emailSecondLine.isEmpty())
        {
            accountContent.add(emailDomainLabel);
        }
        accountContent.add(Box.createVerticalStrut(3));
        accountContent.add(planLabel);
        accountContent.add(Box.createVerticalStrut(6));
        accountContent.add(planButton);
        accountContent.add(Box.createVerticalStrut(5));
        accountContent.add(deviceCountLabel);
        accountContent.add(Box.createVerticalStrut(2));
        accountContent.add(devicesButton);

        if (devicesVisible)
        {
            accountContent.add(Box.createVerticalStrut(5));
            renderDevices();
        }

        accountContent.add(Box.createVerticalStrut(5));
        accountContent.add(logoutButton);
    }

    private void renderDevices()
    {
        if (accountDevices.isEmpty())
        {
            accountContent.add(
                    createAccountLabel(
                            "No device details available."
                    )
            );
            return;
        }

        for (RuneRadarApiClient.Device device : accountDevices)
        {
            JPanel row =
                    new JPanel();

            row.setLayout(
                    new BoxLayout(
                            row,
                            BoxLayout.Y_AXIS
                    )
            );
            row.setBackground(CARD_BACKGROUND);
            row.setBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(
                                    new Color(70, 70, 70)
                            ),
                            BorderFactory.createEmptyBorder(
                                    5,
                                    5,
                                    5,
                                    5
                            )
                    )
            );

            String deviceName =
                    device.getName() == null
                            || device.getName().trim().isEmpty()
                            ? "RuneLite device"
                            : device.getName().trim();

            JLabel label =
                    createAccountLabel(
                            deviceName
                    );

            label.setToolTipText(deviceName);

            JButton revokeButton =
                    new JButton("Revoke");

            revokeButton.setEnabled(!accountBusy);
            revokeButton.addActionListener(
                    event -> revokeDevice(device)
            );

            row.add(label);

            if (device.isCurrent())
            {
                JLabel currentDeviceLabel =
                        createAccountLabel(
                                "This device"
                        );

                currentDeviceLabel.setForeground(GREEN);
                row.add(Box.createVerticalStrut(2));
                row.add(currentDeviceLabel);
            }

            row.add(Box.createVerticalStrut(5));
            row.add(revokeButton);
            accountContent.add(row);
            accountContent.add(Box.createVerticalStrut(5));
        }
    }

    private JLabel createAccountLabel(
            String text
    )
    {
        JLabel label =
                new JLabel(text);

        label.setForeground(TEXT);
        label.setFont(
                label.getFont().deriveFont(
                        13f
                )
        );

        return label;
    }

    private void requestLoginCode()
    {
        final String email =
                emailInput.getText().trim();

        if (
                email.isEmpty()
                        || !email.contains("@")
        )
        {
            setAccountStatus(
                    "Enter a valid email address.",
                    RED
            );
            return;
        }

        setAccountBusy(
                true,
                "Sending login code..."
        );

        startAccountThread(
                "RuneRadar-Auth-Request",
                () ->
                {
                    try
                    {
                        RuneRadarApiClient.RequestCodeResponse response =
                                apiClient.requestLoginCode(email);

                        SwingUtilities.invokeLater(
                                () ->
                                {
                                    setAccountBusy(false, "");
                                    setAccountStatus(
                                            response.getTestCode() == null
                                                    || response.getTestCode().trim().isEmpty()
                                                    ? (response.getMessage() == null
                                                    ? "Code sent. Check your email."
                                                    : response.getMessage())
                                                    : "Local test code: "
                                                    + response.getTestCode().trim(),
                                            GREEN
                                    );
                                    verificationCodeInput.requestFocusInWindow();
                                }
                        );
                    }
                    catch (Exception exception)
                    {
                        SwingUtilities.invokeLater(
                                () ->
                                {
                                    setAccountBusy(false, "");
                                    setAccountStatus(
                                            friendlyAccountError(exception),
                                            RED
                                    );
                                }
                        );
                    }
                }
        );
    }

    private void verifyLoginCode()
    {
        final String email =
                emailInput.getText().trim();

        final String code =
                verificationCodeInput.getText().trim();

        if (
                email.isEmpty()
                        || !email.contains("@")
        )
        {
            setAccountStatus(
                    "Enter the email address used for the code.",
                    RED
            );
            return;
        }

        if (!code.matches("[0-9]{6}"))
        {
            setAccountStatus(
                    "Enter the 6-digit verification code.",
                    RED
            );
            return;
        }

        setAccountBusy(
                true,
                "Verifying code..."
        );

        startAccountThread(
                "RuneRadar-Auth-Verify",
                () ->
                {
                    try
                    {
                        RuneRadarApiClient.VerifyCodeResponse response =
                                apiClient.verifyLoginCode(
                                        email,
                                        code,
                                        deviceId,
                                        "RuneLite on "
                                                + System.getProperty(
                                                "os.name",
                                                "this computer"
                                        )
                                );

                        SwingUtilities.invokeLater(
                                () ->
                                {
                                    verificationCodeInput.setText("");
                                    saveSessionToken(
                                            response.getSessionToken()
                                    );

                                    if (response.getAccount() != null)
                                    {
                                        accountEmail =
                                                response.getAccount().getEmail();
                                        accountPlan =
                                                response.getAccount().getPlan();
                                        resetFreeOnlySelections();
                                    }

                                    setAccountBusy(false, "");
                                    setAccountStatus(
                                            "Signed in",
                                            GREEN
                                    );
                                    renderAccountContent();
                                    refreshAccount();
                                    loadRecommendations(true, false);
                                }
                        );
                    }
                    catch (Exception exception)
                    {
                        SwingUtilities.invokeLater(
                                () ->
                                {
                                    setAccountBusy(false, "");
                                    setAccountStatus(
                                            friendlyAccountError(exception),
                                            RED
                                    );
                                }
                        );
                    }
                }
        );
    }

    private void refreshAccount()
    {
        refreshAccount(false);
    }

    private void refreshAccount(
            boolean quiet
    )
    {
        if (sessionToken.isEmpty())
        {
            return;
        }

        final String requestedSessionToken =
                sessionToken;

        if (!quiet)
        {
            setAccountBusy(
                    true,
                    "Loading account..."
            );
        }

        startAccountThread(
                "RuneRadar-Auth-Account",
                () ->
                {
                    try
                    {
                        RuneRadarApiClient.AccountResponse response =
                                apiClient.getAccount();

                        SwingUtilities.invokeLater(
                                () ->
                                {
                                    if (!requestedSessionToken.equals(sessionToken))
                                    {
                                        return;
                                    }

                                    if (response.getAccount() != null)
                                    {
                                        accountEmail =
                                                response.getAccount().getEmail();
                                        accountPlan =
                                                response.getAccount().getPlan();
                                    }

                                    boolean selectionReset =
                                            resetFreeOnlySelections();

                                    accountDevices =
                                            new ArrayList<>(
                                                    response.getDevices()
                                            );
                                    maximumDevices =
                                            response.getMaximumDevices();
                                    if (!quiet)
                                    {
                                        setAccountBusy(false, "");
                                    setAccountStatus(
                                            "Connected",
                                            GREEN
                                    );
                                    }
                                    renderAccountContent();

                                    if (selectionReset)
                                    {
                                        loadRecommendations(false, false);
                                    }
                                }
                        );
                    }
                    catch (Exception exception)
                    {
                        SwingUtilities.invokeLater(
                                () ->
                                {
                                    if (!requestedSessionToken.equals(sessionToken))
                                    {
                                        return;
                                    }

                                    if (
                                            !quiet
                                                    || isAuthenticationFailure(exception)
                                    )
                                    {
                                        handleAccountRefreshFailure(exception);
                                    }
                                }
                        );
                    }
                }
        );
    }

    private void revokeDevice(
            RuneRadarApiClient.Device device
    )
    {
        if (
                device == null
                        || device.getId() == null
                        || device.getId().trim().isEmpty()
        )
        {
            return;
        }

        int choice =
                JOptionPane.showConfirmDialog(
                        this,
                        device.isCurrent()
                                ? "Revoke this device and sign out?"
                                : "Revoke this RuneRadar device?",
                        "Revoke device",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

        if (choice != JOptionPane.YES_OPTION)
        {
            return;
        }

        setAccountBusy(
                true,
                "Revoking device..."
        );

        startAccountThread(
                "RuneRadar-Auth-Revoke",
                () ->
                {
                    try
                    {
                        apiClient.revokeDevice(
                                device.getId()
                        );

                        SwingUtilities.invokeLater(
                                () ->
                                {
                                    if (device.isCurrent())
                                    {
                                        clearAccountSession(
                                                "This device was revoked."
                                        );
                                    }
                                    else
                                    {
                                        setAccountBusy(false, "");
                                        setAccountStatus(
                                                "Device revoked.",
                                                GREEN
                                        );
                                        refreshAccount();
                                    }
                                }
                        );
                    }
                    catch (Exception exception)
                    {
                        SwingUtilities.invokeLater(
                                () ->
                                {
                                    if (isAuthenticationFailure(exception))
                                    {
                                        clearAccountSession(
                                                "Your session is no longer valid."
                                        );
                                    }
                                    else
                                    {
                                        setAccountBusy(false, "");
                                        setAccountStatus(
                                                friendlyAccountError(exception),
                                                RED
                                        );
                                    }
                                }
                        );
                    }
                }
        );
    }

    private void logoutAccount()
    {
        setAccountBusy(
                true,
                "Signing out..."
        );

        startAccountThread(
                "RuneRadar-Auth-Logout",
                () ->
                {
                    String status =
                            "Signed out.";

                    try
                    {
                        apiClient.logout();
                    }
                    catch (Exception exception)
                    {
                        status =
                                "Signed out locally; server unavailable.";
                    }

                    final String finalStatus = status;

                    SwingUtilities.invokeLater(
                            () -> clearAccountSession(finalStatus)
                    );
                }
        );
    }

    private void openProCheckout()
    {
        if (sessionToken.isEmpty() || accountBusy)
        {
            return;
        }

        final String requestedSessionToken =
                sessionToken;

        setAccountBusy(
                true,
                "Preparing secure checkout..."
        );

        startAccountThread(
                "RuneRadar-Billing-Checkout",
                () ->
                {
                    try
                    {
                        RuneRadarApiClient.CheckoutSessionResponse response =
                                apiClient.createCheckoutSession();

                        String checkoutUrl =
                                response.getCheckoutUrl();

                        if (
                                checkoutUrl == null
                                        || checkoutUrl.trim().isEmpty()
                        )
                        {
                            throw new RuntimeException(
                                    "RuneRadar checkout returned no link."
                            );
                        }

                        SwingUtilities.invokeLater(
                                () ->
                                {
                                    if (!requestedSessionToken.equals(sessionToken))
                                    {
                                        return;
                                    }

                                    setAccountBusy(false, "");
                                    LinkBrowser.browse(checkoutUrl.trim());
                                    setAccountStatus(
                                            "Checkout opened. Refresh after payment.",
                                            GOLD
                                    );
                                }
                        );
                    }
                    catch (Exception exception)
                    {
                        SwingUtilities.invokeLater(
                                () ->
                                {
                                    if (!requestedSessionToken.equals(sessionToken))
                                    {
                                        return;
                                    }

                                    if (isAuthenticationFailure(exception))
                                    {
                                        clearAccountSession(
                                                "Your session expired. Sign in again."
                                        );
                                    }
                                    else
                                    {
                                        setAccountBusy(false, "");
                                        setAccountStatus(
                                                friendlyAccountError(exception),
                                                RED
                                        );
                                    }
                                }
                        );
                    }
                }
        );
    }

    private void handleAccountRefreshFailure(
            Exception exception
    )
    {
        if (isAuthenticationFailure(exception))
        {
            clearAccountSession(
                    "Your session expired or was revoked. Sign in again."
            );
            return;
        }

        setAccountBusy(false, "");
        setAccountStatus(
                friendlyAccountError(exception),
                RED
        );
        renderAccountContent();
    }

    private void clearAccountSession(
            String status
    )
    {
        saveSessionToken("");
        accountEmail = "";
        accountPlan = "FREE";
        resetFreeOnlySelections();
        accountDevices.clear();
        devicesVisible = false;
        accountBusy = false;
        setAccountStatus(status, MUTED);
        renderAccountContent();
    }

    private boolean isAuthenticationFailure(
            Exception exception
    )
    {
        return exception instanceof RuneRadarApiClient.ApiException
                && ((RuneRadarApiClient.ApiException) exception)
                .isAuthenticationFailure();
    }

    private String friendlyAccountError(
            Exception exception
    )
    {
        String message =
                exception == null
                        ? null
                        : exception.getMessage();

        if (
                message == null
                        || message.trim().isEmpty()
        )
        {
            return "RuneRadar account service is unavailable.";
        }

        return message.trim();
    }

    private void setAccountBusy(
            boolean busy,
            String status
    )
    {
        accountBusy = busy;
        setAccountStatus(
                status,
                busy ? GOLD : MUTED
        );
        renderAccountContent();
    }

    private void setAccountStatus(
            String status,
            Color color
    )
    {
        accountStatusLabel.setText(
                status == null ? "" : status
        );
        accountStatusLabel.setForeground(color);
    }

    private void startAccountThread(
            String name,
            Runnable task
    )
    {
        Thread thread =
                new Thread(task);

        thread.setName(name);
        thread.setDaemon(true);
        thread.start();
    }

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

    private final JComboBox<String> sortSelector =
            new JComboBox<>(
                    new String[] {
                            "Default ranking",
                            "Profit",
                            "ROI",
                            "Score",
                            "Volume"
                    }
            );

    private String currentSortMode =
            "DEFAULT";

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

    private List<RuneRadarApiClient.Recommendation>
            defaultRecommendations =
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
                null,
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
        this(
                config,
                activeFlipStore,
                activeFlipsPanel,
                profitTrackerPanel,
                null
        );
    }

    public RuneRadarPanel(
            RuneRadarConfig config,
            ActiveFlipStore activeFlipStore,
            ActiveFlipsPanel activeFlipsPanel,
            ProfitTrackerPanel profitTrackerPanel,
            ConfigManager configManager
    )
    {
        this.config = config;

        this.configManager = configManager;

        deviceId =
                getOrCreateDeviceId();

        sessionToken =
                getPersistedValue(
                        AUTH_SESSION_TOKEN_KEY,
                        ""
                );

        RuneRadarApiClient.setSessionToken(
                sessionToken
        );

        currentResultMode = loadPersistedChoice(
                RESULT_MODE_KEY,
                "STRONG",
                "STRONG",
                "MORE"
        );

        currentFlipType = loadPersistedChoice(
                FLIP_TYPE_KEY,
                "FAST",
                "FAST",
                "BALANCED",
                "SLOW",
                "HIGH_PROFIT",
                "ALL"
        );

        currentSortMode = loadPersistedChoice(
                SORT_MODE_KEY,
                "DEFAULT",
                "DEFAULT",
                "PROFIT",
                "ROI",
                "SCORE",
                "VOLUME"
        );

        advancedVisible = Boolean.parseBoolean(
                getPersistedValue(
                        ADVANCED_VISIBLE_KEY,
                        "false"
                )
        );

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
                        0L,
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

        updateSortSelector();

        renderAccountContent();

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

        if (!sessionToken.isEmpty())
        {
            refreshAccount();
        }

        loadRecommendations(
                false,
                false
        );
    }

    private String getPersistedValue(
            String key,
            String fallback
    )
    {
        if (configManager == null)
        {
            return fallback;
        }

        String value = configManager.getConfiguration(
                CONFIG_GROUP,
                key
        );

        if (
                value == null
                        || value.trim().isEmpty()
        )
        {
            return fallback;
        }

        return value.trim();
    }

    private String loadPersistedChoice(
            String key,
            String fallback,
            String... allowedValues
    )
    {
        String value = getPersistedValue(
                key,
                fallback
        ).toUpperCase();

        for (String allowedValue : allowedValues)
        {
            if (allowedValue.equals(value))
            {
                return value;
            }
        }

        return fallback;
    }

    private void persistSetting(
            String key,
            Object value
    )
    {
        if (configManager != null)
        {
            configManager.setConfiguration(
                    CONFIG_GROUP,
                    key,
                    value
            );
        }
    }

    private String getOrCreateDeviceId()
    {
        String savedDeviceId =
                getPersistedValue(
                        AUTH_DEVICE_ID_KEY,
                        ""
                );

        if (!savedDeviceId.isEmpty())
        {
            return savedDeviceId;
        }

        String newDeviceId =
                UUID.randomUUID().toString();

        persistSetting(
                AUTH_DEVICE_ID_KEY,
                newDeviceId
        );

        return newDeviceId;
    }

    private void saveSessionToken(
            String token
    )
    {
        sessionToken =
                token == null
                        ? ""
                        : token.trim();

        RuneRadarApiClient.setSessionToken(
                sessionToken
        );

        if (configManager != null)
        {
            if (sessionToken.isEmpty())
            {
                configManager.unsetConfiguration(
                        CONFIG_GROUP,
                        AUTH_SESSION_TOKEN_KEY
                );
            }
            else
            {
                persistSetting(
                        AUTH_SESSION_TOKEN_KEY,
                        sessionToken
                );
            }
        }
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

    public void stop()
    {
        autoRefreshTimer.stop();
        freshnessTimer.stop();
        requestNumber++;
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
                createAccountSection()
        );

        wrapper.add(
                Box.createVerticalStrut(
                        10
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

        wrapper.add(
                Box.createVerticalStrut(
                        6
                )
        );

        wrapper.add(
                createSortSection()
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

            if (configManager != null)
            {
                configManager.setConfiguration(
                        "runeradar",
                        "cashStack",
                        currentCashStack
                );
            }

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

            if (cash < 0)
            {
                throw new IllegalArgumentException(
                        "Cash stack cannot be below 0"
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
        if (currentCashStack == 0)
        {
            cashLabel.setText(
                    "All cash stacks • no budget filter"
            );

            return;
        }

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

        persistSetting(
                RESULT_MODE_KEY,
                currentResultMode
        );

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
        boolean pro =
                hasProAccess();

        strongButton.setEnabled(
                !blockingLoad
                        && !currentResultMode.equals(
                        "STRONG"
                )
        );

        moreButton.setEnabled(
                pro
                        && !blockingLoad
                        && !currentResultMode.equals(
                        "MORE"
                )
        );

        moreButton.setToolTipText(
                pro
                        ? "Show the expanded RuneRadar Pro list"
                        : "RuneRadar Pro required"
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

        persistSetting(
                FLIP_TYPE_KEY,
                currentFlipType
        );

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
        boolean pro =
                hasProAccess();

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
                pro
                        && !blockingLoad
                        && !currentFlipType.equals(
                        "HIGH_PROFIT"
                )
        );

        highProfitButton.setToolTipText(
                pro
                        ? "Show high-profit RuneRadar Pro opportunities"
                        : "RuneRadar Pro required"
        );

        allButton.setEnabled(
                !blockingLoad
                        && !currentFlipType.equals(
                        "ALL"
                )
        );
    }

    private JPanel createSortSection()
    {
        JPanel panel =
                new JPanel(
                        new BorderLayout(
                                6,
                                0
                        )
                );

        panel.setBackground(
                BACKGROUND
        );

        JLabel label =
                new JLabel(
                        "Sort"
                );

        label.setForeground(
                MUTED
        );

        sortSelector.setToolTipText(
                "Default keeps RuneRadar's recommended ranking"
        );

        sortSelector.addActionListener(
                event -> setSortModeFromSelector()
        );

        panel.add(
                label,
                BorderLayout.WEST
        );

        panel.add(
                sortSelector,
                BorderLayout.CENTER
        );

        return panel;
    }

    private void updateSortSelector()
    {
        String displayValue;

        switch (currentSortMode)
        {
            case "PROFIT":
                displayValue = "Profit";
                break;

            case "ROI":
                displayValue = "ROI";
                break;

            case "SCORE":
                displayValue = "Score";
                break;

            case "VOLUME":
                displayValue = "Volume";
                break;

            default:
                displayValue = "Default ranking";
                break;
        }

        sortSelector.setSelectedItem(
                displayValue
        );
    }

    private void setSortModeFromSelector()
    {
        Object selected =
                sortSelector.getSelectedItem();

        String selectedText =
                selected == null
                        ? "Default ranking"
                        : selected.toString();

        switch (selectedText)
        {
            case "Profit":
                currentSortMode = "PROFIT";
                break;

            case "ROI":
                currentSortMode = "ROI";
                break;

            case "Score":
                currentSortMode = "SCORE";
                break;

            case "Volume":
                currentSortMode = "VOLUME";
                break;

            default:
                currentSortMode = "DEFAULT";
                break;
        }

        persistSetting(
                SORT_MODE_KEY,
                currentSortMode
        );

        applyRecommendationSort();

        currentIndex = 0;

        showCurrentFlip();
    }

    private void applyRecommendationSort()
    {
        recommendations =
                new ArrayList<>(
                        defaultRecommendations
                );

        Comparator<RuneRadarApiClient.Recommendation> comparator = null;

        switch (currentSortMode)
        {
            case "PROFIT":
                comparator = Comparator.comparingLong(
                        RuneRadarApiClient.Recommendation::getNetExpectedProfit
                );
                break;

            case "ROI":
                comparator = Comparator.comparingDouble(
                        RuneRadarApiClient.Recommendation::getRoi
                );
                break;

            case "SCORE":
                comparator = Comparator.comparingInt(
                        RuneRadarApiClient.Recommendation::getScore
                );
                break;

            case "VOLUME":
                comparator = Comparator.comparingLong(
                        RuneRadarApiClient.Recommendation::getVolume
                );
                break;

            default:
                break;
        }

        if (comparator != null)
        {
            recommendations.sort(
                    comparator.reversed()
            );
        }
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
                advancedVisible
        );

        advancedButton.setText(
                advancedVisible
                        ? "Hide Advanced Details"
                        : "Show Advanced Details"
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

        persistSetting(
                ADVANCED_VISIBLE_KEY,
                advancedVisible
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
                item.getNetExpectedProfit(),
                getTrackingCategoryForRecommendation(
                        item
                )
        );

        activeFlipsPanel.refresh();

        updateStartActiveFlipButton();

        updatedLabel.setText(
                "Added to Active Flips • NET target after GE tax"
        );

        revalidate();

        repaint();
    }

    public String getTrackingCategoryForRecommendation(
            RuneRadarApiClient.Recommendation recommendation
    )
    {
        if (
                "HIGH_PROFIT".equals(
                        currentFlipType
                )
        )
        {
            return "HIGH_PROFIT";
        }

        if (recommendation != null)
        {
            String tradingSpeed =
                    recommendation.getTradingSpeed();

            if (tradingSpeed != null)
            {
                String cleaned =
                        tradingSpeed
                                .trim()
                                .toUpperCase();

                if (
                        "FAST".equals(
                                cleaned
                        )
                                || "BALANCED".equals(
                                cleaned
                        )
                                || "SLOW".equals(
                                cleaned
                        )
                )
                {
                    return cleaned;
                }
            }
        }

        return "BALANCED";
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

        sortSelector.setEnabled(
                !blockingLoad
        );

        if (quietRefresh)
        {
            updatedLabel.setText(
                    "Refreshing market data..."
            );

            statusLabel.setForeground(
                    GOLD
            );

            statusLabel.setText(
                    "Loading latest recommendations..."
            );
        }
        else
        {
            setLoadingState();
        }

        final long cashStack =
                currentCashStack == 0
                        ? MAX_CASH_STACK
                        : currentCashStack;

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
                                                    MAX_RESULT_LIMIT
                                            );
                                }
                                else
                                {
                                    response =
                                            apiClient.getRecommendations(
                                                    cashStack,
                                                    requestedFlipType,
                                                    MAX_RESULT_LIMIT
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

                                            if (
                                                    response.getPlan() != null
                                                            && !response.getPlan().trim().isEmpty()
                                                            && !response.getPlan().equalsIgnoreCase(
                                                            accountPlan
                                                    )
                                            )
                                            {
                                                accountPlan =
                                                        response.getPlan();
                                                resetFreeOnlySelections();
                                                renderAccountContent();
                                            }

                                            defaultRecommendations =
                                                    new ArrayList<>(
                                                            response.getRecommendations()
                                                    );

                                            applyRecommendationSort();

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

                                            sortSelector.setEnabled(
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

                                            if (!sessionToken.isEmpty())
                                            {
                                                refreshAccount(true);
                                            }
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

                                            sortSelector.setEnabled(
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
                                                        friendlyApiError(
                                                                exception.getMessage(),
                                                                true
                                                        )
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

        defaultRecommendations.clear();

        boolean proRequired =
                message != null
                        && message.toLowerCase().contains(
                        "runeradar pro"
                );

        itemNameLabel.setText(
                proRequired
                        ? "RuneRadar Pro required"
                        : "RuneRadar API unavailable"
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
                proRequired
                        ? "This recommendation mode is Pro-only."
                        : "The backend or network is currently unavailable."
        );

        statusLabel.setForeground(
                RED
        );

        statusLabel.setText(
                friendlyApiError(
                        message,
                        false
                )
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

    public RuneRadarApiClient.Recommendation getCurrentRecommendationForOverlay()
    {
        if (
                recommendations.isEmpty()
                        || currentIndex < 0
                        || currentIndex >= recommendations.size()
        )
        {
            return null;
        }

        return recommendations.get(
                currentIndex
        );
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
                    "No results met the current quality and safety filters."
            );

            statusLabel.setForeground(
                    MUTED
            );

            statusLabel.setText(
                    "Last updated "
                            + secondsAgo()
                            + " sec ago • try another type or cash stack"
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
                        + " • Last updated "
                        + secondsAgo()
                        + " sec ago"
        );
    }

    private String friendlyApiError(
            String message,
            boolean keepingPreviousData
    )
    {
        String cleaned =
                message == null
                        ? ""
                        : message.trim();

        if (cleaned.toLowerCase().contains("runeradar pro"))
        {
            return cleaned;
        }

        if (keepingPreviousData)
        {
            return "Refresh failed • showing last valid data";
        }

        return "Could not load recommendations • check your connection and retry";
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
