package com.runeradar;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class RuneRadarApiClient
{
    private static final String API_BASE_URL =
            "https://runeradar-production.up.railway.app";

    private final Gson gson =
            new Gson();

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

        URL url =
                new URL(
                        urlText
                );

        HttpURLConnection connection =
                (HttpURLConnection)
                        url.openConnection();

        connection.setRequestMethod(
                "GET"
        );

        connection.setConnectTimeout(
                5000
        );

        connection.setReadTimeout(
                15000
        );

        connection.setRequestProperty(
                "Accept",
                "application/json"
        );

        int responseCode =
                connection.getResponseCode();

        if (
                responseCode
                        != 200
        )
        {
            throw new RuntimeException(
                    "RuneRadar API returned HTTP "
                            + responseCode
            );
        }

        StringBuilder responseText =
                new StringBuilder();

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection
                                                .getInputStream(),
                                        StandardCharsets.UTF_8
                                )
                        )
        )
        {
            String line;

            while (
                    (line = reader.readLine())
                            != null
            )
            {
                responseText.append(
                        line
                );
            }
        }
        finally
        {
            connection.disconnect();
        }

        ApiResponse response =
                gson.fromJson(
                        responseText.toString(),
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
