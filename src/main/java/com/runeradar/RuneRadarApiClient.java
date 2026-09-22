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
            "http://127.0.0.1:8765";

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
        String urlText =
                API_BASE_URL
                        + endpoint
                        + "?cash=" + cashStack
                        + "&type=" + flipType
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

        private List<Recommendation>
                recommendations;

        public String getStatus()
        {
            return status;
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
                "expected_profit"
        )
        private long expectedProfit;

        @SerializedName(
                "profit_per_item"
        )
        private long profitPerItem;

        private long quantity;

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

        @SerializedName(
                "gp_needed"
        )
        private long gpNeeded;

        @SerializedName(
                "capital_used_percent"
        )
        private double capitalUsedPercent;

        private double roi;

        @SerializedName(
                "opportunity_level"
        )
        private String opportunityLevel;

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

        public long getExpectedProfit()
        {
            return expectedProfit;
        }

        public long getProfitPerItem()
        {
            return profitPerItem;
        }

        public long getQuantity()
        {
            return quantity;
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

        public String getOpportunityLevel()
        {
            return opportunityLevel;
        }
    }
}
