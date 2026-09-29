package com.runeradar;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class RuneRadarApiClient
{
    private static final String API_BASE_URL =
            System.getProperty(
                    "runeradar.apiBaseUrl",
                    "https://runeradar-production.up.railway.app"
            );

    private static Gson gson;

    private static OkHttpClient httpClient;

    private static String installId =
            "";

    private static String pluginVersion =
            "";

    private static volatile String sessionToken =
            "";

    private static final MediaType JSON_MEDIA_TYPE =
            MediaType.parse(
                    "application/json; charset=utf-8"
            );

    public static void setGson(
            Gson injectedGson
    )
    {
        if (injectedGson == null)
        {
            throw new IllegalArgumentException(
                    "Injected Gson cannot be null."
            );
        }

        gson =
                injectedGson;
    }

    public static Gson getInjectedGson()
    {
        if (gson == null)
        {
            throw new IllegalStateException(
                    "RuneRadar Gson has not been initialized."
            );
        }

        return gson;
    }

    public static void setHttpClient(
            OkHttpClient injectedHttpClient
    )
    {
        if (injectedHttpClient == null)
        {
            throw new IllegalArgumentException(
                    "Injected OkHttpClient cannot be null."
            );
        }

        httpClient =
                injectedHttpClient;
    }

    private static OkHttpClient getInjectedHttpClient()
    {
        if (httpClient == null)
        {
            throw new IllegalStateException(
                    "RuneRadar OkHttpClient has not been initialized."
            );
        }

        return httpClient;
    }

    public static void setAnalyticsIdentity(
            String analyticsInstallId,
            String analyticsPluginVersion
    )
    {
        installId =
                cleanHeaderValue(
                        analyticsInstallId
                );

        pluginVersion =
                cleanHeaderValue(
                        analyticsPluginVersion
                );
    }

    public static void setSessionToken(
            String accountSessionToken
    )
    {
        sessionToken =
                cleanHeaderValue(
                        accountSessionToken
                );
    }

    private static String cleanHeaderValue(
            String value
    )
    {
        if (value == null)
        {
            return "";
        }

        return value
                .trim()
                .replace(
                        "\r",
                        ""
                )
                .replace(
                        "\n",
                        ""
                );
    }

    private static Request.Builder createRequestBuilder(
            String urlText
    )
    {
        Request.Builder builder =
                new Request.Builder()
                        .url(
                                urlText
                        )
                        .get()
                        .header(
                                "Accept",
                                "application/json"
                        );

        if (!installId.isEmpty())
        {
            builder.header(
                    "X-RuneRadar-Install-ID",
                    installId
            );
        }

        if (!pluginVersion.isEmpty())
        {
            builder.header(
                    "X-RuneRadar-Version",
                    pluginVersion
            );
        }

        if (!sessionToken.isEmpty())
        {
            builder.header(
                    "Authorization",
                    "Bearer " + sessionToken
            );
        }

        return builder;
    }

    public RequestCodeResponse requestLoginCode(
            String email
    ) throws Exception
    {
        Map<String, String> payload =
                new HashMap<>();

        payload.put("email", email);

        return postJson(
                "/auth/request-code",
                payload,
                RequestCodeResponse.class,
                false
        );
    }

    public VerifyCodeResponse verifyLoginCode(
            String email,
            String code,
            String deviceId,
            String deviceName
    ) throws Exception
    {
        Map<String, String> payload =
                new HashMap<>();

        payload.put("email", email);
        payload.put("code", code);
        payload.put("device_id", deviceId);
        payload.put("device_name", deviceName);

        return postJson(
                "/auth/verify-code",
                payload,
                VerifyCodeResponse.class,
                false
        );
    }

    public AccountResponse getAccount() throws Exception
    {
        Request request =
                createRequestBuilder(
                        API_BASE_URL + "/auth/account"
                ).build();

        return executeJson(
                request,
                AccountResponse.class
        );
    }

    public BasicResponse revokeDevice(
            String deviceId
    ) throws Exception
    {
        Map<String, String> payload =
                new HashMap<>();

        payload.put("device_id", deviceId);

        return postJson(
                "/auth/devices/revoke",
                payload,
                BasicResponse.class,
                true
        );
    }

    public BasicResponse logout() throws Exception
    {
        return postJson(
                "/auth/logout",
                Collections.emptyMap(),
                BasicResponse.class,
                true
        );
    }

    public CheckoutSessionResponse createCheckoutSession() throws Exception
    {
        return postJson(
                "/billing/checkout-session",
                Collections.emptyMap(),
                CheckoutSessionResponse.class,
                true
        );
    }

    private <T> T postJson(
            String endpoint,
            Object payload,
            Class<T> responseClass,
            boolean authenticated
    ) throws Exception
    {
        RequestBody body =
                RequestBody.create(
                        JSON_MEDIA_TYPE,
                        getInjectedGson().toJson(payload)
                );

        Request.Builder builder =
                createRequestBuilder(
                        API_BASE_URL + endpoint
                );

        if (!authenticated)
        {
            builder.removeHeader("Authorization");
        }

        return executeJson(
                builder.post(body).build(),
                responseClass
        );
    }

    private <T> T executeJson(
            Request request,
            Class<T> responseClass
    ) throws Exception
    {
        String responseText;

        try (
                Response response =
                        getInjectedHttpClient()
                                .newCall(request)
                                .execute()
        )
        {
            if (!response.isSuccessful())
            {
                throw createApiException(response);
            }

            ResponseBody responseBody =
                    response.body();

            if (responseBody == null)
            {
                throw new RuntimeException(
                        "RuneRadar API returned no response body."
                );
            }

            responseText =
                    responseBody.string();
        }

        T parsed =
                getInjectedGson().fromJson(
                        responseText,
                        responseClass
                );

        if (parsed == null)
        {
            throw new RuntimeException(
                    "RuneRadar API returned no data."
            );
        }

        return parsed;
    }

    public ApiResponse getRecommendations(
            long cashStack,
            String flipType,
            int limit
    ) throws Exception
    {
        return requestRecommendations(
                "/recommendations",
                cashStack,
                flipType,
                limit
        );
    }

    public ApiResponse getMoreOpportunities(
            long cashStack,
            String flipType,
            int limit
    ) throws Exception
    {
        return requestRecommendations(
                "/more-opportunities",
                cashStack,
                flipType,
                limit
        );
    }

    public MarketItemResponse getMarketItem(
            int itemId
    ) throws Exception
    {
        String urlText =
                API_BASE_URL
                        + "/market-item?id="
                        + itemId;

        Request request =
                createRequestBuilder(
                        urlText
                ).build();

        String responseText;

        try (
                Response response =
                        getInjectedHttpClient()
                                .newCall(
                                        request
                                )
                                .execute()
        )
        {
            if (!response.isSuccessful())
            {
                throw createApiException(
                        response
                );
            }

            ResponseBody responseBody =
                    response.body();

            if (responseBody == null)
            {
                throw new RuntimeException(
                        "RuneRadar API returned no response body."
                );
            }

            responseText =
                    responseBody.string();
        }

        MarketItemResponse response =
                getInjectedGson().fromJson(
                        responseText,
                        MarketItemResponse.class
                );

        if (
                response == null
                        || response.item == null
        )
        {
            throw new RuntimeException(
                    "RuneRadar API returned no market item."
            );
        }

        return response;
    }

    private ApiResponse requestRecommendations(
            String endpoint,
            long cashStack,
            String flipType,
            int limit
    ) throws Exception
    {
        String cleanedFlipType =
                flipType == null
                        ? "FAST"
                        : flipType
                        .trim()
                        .toUpperCase()
                        .replace(
                                " ",
                                "_"
                        );

        String urlText =
                API_BASE_URL
                        + endpoint
                        + "?cash=" + cashStack
                        + "&type=" + cleanedFlipType
                        + "&limit=" + limit;

        Request request =
                createRequestBuilder(
                        urlText
                ).build();

        String responseText;

        try (
                Response response =
                        getInjectedHttpClient()
                                .newCall(
                                        request
                                )
                                .execute()
        )
        {
            if (!response.isSuccessful())
            {
                throw createApiException(
                        response
                );
            }

            ResponseBody responseBody =
                    response.body();

            if (responseBody == null)
            {
                throw new RuntimeException(
                        "RuneRadar API returned no response body."
                );
            }

            responseText =
                    responseBody.string();
        }

        ApiResponse response =
                getInjectedGson().fromJson(
                        responseText,
                        ApiResponse.class
                );

        if (
                response == null
        )
        {
            throw new RuntimeException(
                    "RuneRadar API returned no data."
            );
        }

        if (
                response.recommendations
                        == null
        )
        {
            response.recommendations =
                    Collections.emptyList();
        }

        return response;
    }

    private static RuntimeException createApiException(
            Response response
    )
    {
        String code =
                "HTTP_" + response.code();

        String message =
                "RuneRadar API returned HTTP "
                        + response.code();

        ResponseBody responseBody =
                response.body();

        if (responseBody != null)
        {
            try
            {
                ApiErrorResponse apiError =
                        getInjectedGson().fromJson(
                                responseBody.string(),
                                ApiErrorResponse.class
                        );

                if (
                        apiError != null
                                && apiError.message != null
                                && !apiError.message.trim().isEmpty()
                )
                {
                    message = apiError.message.trim();
                }

                if (
                        apiError != null
                                && apiError.code != null
                                && !apiError.code.trim().isEmpty()
                )
                {
                    code = apiError.code.trim();
                }
            }
            catch (Exception ignored)
            {
            }
        }

        return new ApiException(
                response.code(),
                code,
                message
        );
    }

    private static class ApiErrorResponse
    {
        private String code;

        private String message;
    }

    public static class ApiException extends RuntimeException
    {
        private final int statusCode;

        private final String code;

        private ApiException(
                int statusCode,
                String code,
                String message
        )
        {
            super(message);
            this.statusCode = statusCode;
            this.code = code;
        }

        public boolean isAuthenticationFailure()
        {
            return statusCode == 401
                    || "AUTH_REQUIRED".equals(code)
                    || "INVALID_SESSION".equals(code)
                    || "SESSION_EXPIRED".equals(code);
        }
    }

    public static class BasicResponse
    {
        private String status;

        private String message;

        public String getMessage()
        {
            return message;
        }
    }

    public static class RequestCodeResponse extends BasicResponse
    {
        @SerializedName("expires_in")
        private long expiresIn;

        @SerializedName("test_code")
        private String testCode;

        public long getExpiresIn()
        {
            return expiresIn;
        }

        public String getTestCode()
        {
            return testCode;
        }
    }

    public static class VerifyCodeResponse extends BasicResponse
    {
        @SerializedName("session_token")
        private String sessionToken;

        private Account account;

        private Device device;

        public String getSessionToken()
        {
            return sessionToken;
        }

        public Account getAccount()
        {
            return account;
        }

        public Device getDevice()
        {
            return device;
        }
    }

    public static class AccountResponse extends BasicResponse
    {
        private Account account;

        private List<Device> devices;

        @SerializedName("maximum_devices")
        private int maximumDevices;

        public Account getAccount()
        {
            return account;
        }

        public List<Device> getDevices()
        {
            return devices == null
                    ? Collections.emptyList()
                    : devices;
        }

        public int getMaximumDevices()
        {
            return maximumDevices;
        }
    }

    public static class CheckoutSessionResponse extends BasicResponse
    {
        @SerializedName("checkout_url")
        private String checkoutUrl;

        @SerializedName("expires_at")
        private long expiresAt;

        public String getCheckoutUrl()
        {
            return checkoutUrl;
        }

        public long getExpiresAt()
        {
            return expiresAt;
        }
    }

    public static class Account
    {
        private String email;

        private String plan;

        public String getEmail()
        {
            return email;
        }

        public String getPlan()
        {
            return plan;
        }
    }

    public static class Device
    {
        private String id;

        private String name;

        private boolean current;

        @SerializedName("last_seen")
        private long lastSeen;

        public String getId()
        {
            return id;
        }

        public String getName()
        {
            return name;
        }

        public boolean isCurrent()
        {
            return current;
        }

        public long getLastSeen()
        {
            return lastSeen;
        }
    }

    public static class MarketItemResponse
    {
        private String status;

        @SerializedName(
                "updated_at"
        )
        private long updatedAt;

        private MarketItem item;

        public String getStatus()
        {
            return status;
        }

        public long getUpdatedAt()
        {
            return updatedAt;
        }

        public MarketItem getItem()
        {
            return item;
        }
    }

    public static class MarketItem
    {
        private int id;

        private String name;

        @SerializedName(
                "buy_price"
        )
        private long buyPrice;

        @SerializedName(
                "sell_price"
        )
        private long sellPrice;

        @SerializedName(
                "avg_buy_price"
        )
        private long averageBuyPrice;

        @SerializedName(
                "avg_sell_price"
        )
        private long averageSellPrice;

        @SerializedName(
                "high_volume"
        )
        private long highVolume;

        @SerializedName(
                "low_volume"
        )
        private long lowVolume;

        @SerializedName(
                "buy_limit"
        )
        private long buyLimit;

        public int getId()
        {
            return id;
        }

        public String getName()
        {
            return name;
        }

        public long getBuyPrice()
        {
            return buyPrice;
        }

        public long getSellPrice()
        {
            return sellPrice;
        }

        public long getAverageBuyPrice()
        {
            return averageBuyPrice;
        }

        public long getAverageSellPrice()
        {
            return averageSellPrice;
        }

        public long getHighVolume()
        {
            return highVolume;
        }

        public long getLowVolume()
        {
            return lowVolume;
        }

        public long getBuyLimit()
        {
            return buyLimit;
        }
    }

    public static class ApiResponse
    {
        private String status;

        @SerializedName(
                "api_version"
        )
        private int apiVersion;

        private String mode;

        @SerializedName(
                "cash_stack"
        )
        private long cashStack;

        @SerializedName(
                "flip_type"
        )
        private String flipType;

        @SerializedName(
                "minimum_score"
        )
        private int minimumScore;

        private int count;

        @SerializedName(
                "updated_at"
        )
        private long updatedAt;

        @SerializedName(
                "profit_basis"
        )
        private String profitBasis;

        private String plan;

        @SerializedName(
                "recommendation_limit"
        )
        private int recommendationLimit;

        private List<Recommendation>
                recommendations;

        public String getStatus()
        {
            return status;
        }

        public int getApiVersion()
        {
            return apiVersion;
        }

        public String getMode()
        {
            return mode;
        }

        public long getCashStack()
        {
            return cashStack;
        }

        public String getFlipType()
        {
            return flipType;
        }

        public int getMinimumScore()
        {
            return minimumScore;
        }

        public int getCount()
        {
            return count;
        }

        public long getUpdatedAt()
        {
            return updatedAt;
        }

        public String getProfitBasis()
        {
            return profitBasis;
        }

        public String getPlan()
        {
            return plan;
        }

        public int getRecommendationLimit()
        {
            return recommendationLimit;
        }

        public List<Recommendation>
        getRecommendations()
        {
            return recommendations;
        }
    }

    public static class Recommendation
    {
        private int id;

        private String name;

        private long buy;

        private long sell;

        @SerializedName(
                "net_sell_price"
        )
        private long netSellPrice;

        @SerializedName(
                "break_even_sell"
        )
        private long breakEvenSell;

        // ====================================================
        // NET PROFIT
        // ====================================================

        @SerializedName(
                "expected_profit"
        )
        private long expectedProfit;

        @SerializedName(
                "net_expected_profit"
        )
        private long netExpectedProfit;

        @SerializedName(
                "profit_per_item"
        )
        private long profitPerItem;

        @SerializedName(
                "net_profit_per_item"
        )
        private long netProfitPerItem;

        // ====================================================
        // GROSS PROFIT
        // ====================================================

        @SerializedName(
                "gross_profit_per_item"
        )
        private long grossProfitPerItem;

        @SerializedName(
                "gross_expected_profit"
        )
        private long grossExpectedProfit;

        // ====================================================
        // GE TAX
        // ====================================================

        @SerializedName(
                "tax_per_item"
        )
        private long taxPerItem;

        @SerializedName(
                "total_tax"
        )
        private long totalTax;

        @SerializedName(
                "tax_exempt"
        )
        private boolean taxExempt;

        // ====================================================
        // QUANTITY
        // ====================================================

        private long quantity;

        @SerializedName(
                "recommended_qty"
        )
        private long recommendedQty;

        @SerializedName(
                "cash_allows"
        )
        private long cashAllows;

        @SerializedName(
                "buy_limit"
        )
        private long buyLimit;

        @SerializedName(
                "buy_limit_known"
        )
        private boolean buyLimitKnown;

        @SerializedName(
                "liquidity_quantity"
        )
        private long liquidityQuantity;

        private long volume;

        // ====================================================
        // QUALITY / MARKET
        // ====================================================

        private String risk;

        @SerializedName(
                "trading_speed"
        )
        private String tradingSpeed;

        private String liquidity;

        @SerializedName(
                "price_stability"
        )
        private String priceStability;

        private String confidence;

        private int score;

        // ====================================================
        // CAPITAL
        // ====================================================

        @SerializedName(
                "gp_needed"
        )
        private long gpNeeded;

        @SerializedName(
                "capital_used_percent"
        )
        private double capitalUsedPercent;

        // ====================================================
        // ROI
        // ====================================================

        private double roi;

        @SerializedName(
                "gross_roi"
        )
        private double grossRoi;

        @SerializedName(
                "opportunity_level"
        )
        private String opportunityLevel;

        // ====================================================
        // GETTERS
        // ====================================================

        public int getId()
        {
            return id;
        }

        public String getName()
        {
            return name;
        }

        public long getBuy()
        {
            return buy;
        }

        public long getSell()
        {
            return sell;
        }

        public long getNetSellPrice()
        {
            return netSellPrice;
        }

        public long getBreakEvenSell()
        {
            return breakEvenSell;
        }

        public long getExpectedProfit()
        {
            if (
                    netExpectedProfit
                            != 0
            )
            {
                return netExpectedProfit;
            }

            return expectedProfit;
        }

        public long getNetExpectedProfit()
        {
            if (
                    netExpectedProfit
                            != 0
            )
            {
                return netExpectedProfit;
            }

            return expectedProfit;
        }

        public long getProfitPerItem()
        {
            if (
                    netProfitPerItem
                            != 0
            )
            {
                return netProfitPerItem;
            }

            return profitPerItem;
        }

        public long getNetProfitPerItem()
        {
            if (
                    netProfitPerItem
                            != 0
            )
            {
                return netProfitPerItem;
            }

            return profitPerItem;
        }

        public long getGrossProfitPerItem()
        {
            return grossProfitPerItem;
        }

        public long getGrossExpectedProfit()
        {
            return grossExpectedProfit;
        }

        public long getTaxPerItem()
        {
            return taxPerItem;
        }

        public long getTotalTax()
        {
            return totalTax;
        }

        public boolean isTaxExempt()
        {
            return taxExempt;
        }

        public long getQuantity()
        {
            if (
                    recommendedQty
                            > 0
            )
            {
                return recommendedQty;
            }

            return quantity;
        }

        public long getRecommendedQty()
        {
            if (
                    recommendedQty
                            > 0
            )
            {
                return recommendedQty;
            }

            return quantity;
        }

        public long getCashAllows()
        {
            return cashAllows;
        }

        public long getBuyLimit()
        {
            return buyLimit;
        }

        public boolean isBuyLimitKnown()
        {
            return buyLimitKnown;
        }

        public long getLiquidityQuantity()
        {
            return liquidityQuantity;
        }

        public long getVolume()
        {
            return volume;
        }

        public String getRisk()
        {
            return risk;
        }

        public String getTradingSpeed()
        {
            return tradingSpeed;
        }

        public String getLiquidity()
        {
            return liquidity;
        }

        public String getPriceStability()
        {
            return priceStability;
        }

        public String getConfidence()
        {
            return confidence;
        }

        public int getScore()
        {
            return score;
        }

        public long getGpNeeded()
        {
            return gpNeeded;
        }

        public double getCapitalUsedPercent()
        {
            return capitalUsedPercent;
        }

        public double getRoi()
        {
            return roi;
        }

        public double getGrossRoi()
        {
            return grossRoi;
        }

        public String getOpportunityLevel()
        {
            return opportunityLevel;
        }
    }
}
