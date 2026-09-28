package com.runeradar;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.runelite.client.util.Filepath;

public class ProfitTrackerStore
{
    private static Filepath defaultDataDirectory;

    private static final double GE_TAX_RATE = 0.02;

    private static final long GE_TAX_CAP_PER_ITEM =
            5_000_000L;

    private final Gson gson;

    private final Filepath storageFile;

    private final List<FlipRecord> records =
            new ArrayList<>();

    private long sessionProfit;

    private long todayProfit;

    private String todayDate =
            LocalDate.now().toString();

    private long totalProfit;

    private int completedFlipCount;

    public ProfitTrackerStore()
    {
        this(
                getDefaultDataDirectory()
        );
    }

    public ProfitTrackerStore(
            Filepath dataDirectory
    )
    {
        if (dataDirectory == null)
        {
            throw new IllegalArgumentException(
                    "dataDirectory must not be null"
            );
        }

        defaultDataDirectory =
                dataDirectory;

        gson =
                RuneRadarApiClient
                        .getInjectedGson()
                        .newBuilder()
                        .setPrettyPrinting()
                        .create();

        storageFile =
                dataDirectory.joinSegment(
                        "profit_history.json"
                );

        load();
    }

    private static Filepath getDefaultDataDirectory()
    {
        if (defaultDataDirectory == null)
        {
            throw new IllegalStateException(
                    "RuneRadar storage has not been initialized"
            );
        }

        return defaultDataDirectory;
    }

    // ========================================================
    // ADD FLIP
    // ========================================================

    public FlipRecord addCompletedFlip(
            String itemName,
            long buyPrice,
            long sellPrice,
            long quantity
    )
    {
        if (
                itemName == null
                        || itemName.trim().isEmpty()
        )
        {
            throw new IllegalArgumentException(
                    "Enter an item name."
            );
        }

        if (buyPrice <= 0)
        {
            throw new IllegalArgumentException(
                    "Buy price must be above 0."
            );
        }

        if (sellPrice <= 0)
        {
            throw new IllegalArgumentException(
                    "Sell price must be above 0."
            );
        }

        if (quantity <= 0)
        {
            throw new IllegalArgumentException(
                    "Quantity must be above 0."
            );
        }

        long taxPerItem =
                calculateEstimatedTaxPerItem(
                        sellPrice
                );

        long totalBoughtFor =
                safeMultiply(
                        buyPrice,
                        quantity
                );

        long totalSoldFor =
                safeMultiply(
                        sellPrice,
                        quantity
                );

        long totalTax =
                safeMultiply(
                        taxPerItem,
                        quantity
                );

        long netProfit =
                totalSoldFor
                        - totalBoughtFor
                        - totalTax;

        FlipRecord record =
                new FlipRecord(
                        Instant.now()
                                .getEpochSecond(),
                        itemName.trim(),
                        buyPrice,
                        sellPrice,
                        quantity,
                        totalTax,
                        netProfit
                );

        records.add(
                record
        );

        recordStatistics(
                record
        );

        sortNewestFirst();

        save();

        return record;
    }

    public synchronized FlipRecord addCompletedFlipExact(
            String itemName,
            long buyPrice,
            long sellPrice,
            long quantity,
            long tax,
            long netProfit
    )
    {
        if (
                itemName == null
                        || itemName.trim().isEmpty()
        )
        {
            throw new IllegalArgumentException(
                    "Enter an item name."
            );
        }

        if (buyPrice <= 0)
        {
            throw new IllegalArgumentException(
                    "Buy price must be above 0."
            );
        }

        if (sellPrice <= 0)
        {
            throw new IllegalArgumentException(
                    "Sell price must be above 0."
            );
        }

        if (quantity <= 0)
        {
            throw new IllegalArgumentException(
                    "Quantity must be above 0."
            );
        }

        FlipRecord record =
                new FlipRecord(
                        Instant.now()
                                .getEpochSecond(),
                        itemName.trim(),
                        buyPrice,
                        sellPrice,
                        quantity,
                        Math.max(
                                tax,
                                0
                        ),
                        netProfit
                );

        records.add(
                record
        );

        recordStatistics(
                record
        );

        sortNewestFirst();

        save();

        return record;
    }

    // ========================================================
    // TAX
    // ========================================================

    private long calculateEstimatedTaxPerItem(
            long sellPrice
    )
    {
        if (sellPrice <= 0)
        {
            return 0;
        }

        long tax =
                (long) Math.floor(
                        sellPrice
                                * GE_TAX_RATE
                );

        return Math.min(
                tax,
                GE_TAX_CAP_PER_ITEM
        );
    }

    // ========================================================
    // TOTALS
    // ========================================================

    public long getSessionProfit(
            long sessionStartedAt
    )
    {
        return sessionProfit;
    }

    public long getTodayProfit()
    {
        rollDailyStatisticsIfNeeded();

        return todayProfit;
    }

    public long getTotalProfit()
    {
        return totalProfit;
    }

    public int getCompletedFlipCount()
    {
        return completedFlipCount;
    }

    // ========================================================
    // HISTORY
    // ========================================================

    public List<FlipRecord> getHistory()
    {
        return new ArrayList<>(
                records
        );
    }

    public List<FlipRecord> getRecentHistory(
            int limit
    )
    {
        int safeLimit =
                Math.max(
                        0,
                        limit
                );

        int end =
                Math.min(
                        safeLimit,
                        records.size()
                );

        return new ArrayList<>(
                records.subList(
                        0,
                        end
                )
        );
    }

    public void deleteRecord(
            long timestamp
    )
    {
        records.removeIf(
                record ->
                        record.getTimestamp()
                                == timestamp
        );

        save();
    }

    public void clearHistory()
    {
        records.clear();

        save();
    }

    public void resetStatistics()
    {
        sessionProfit = 0;
        todayProfit = 0;
        todayDate = LocalDate.now().toString();
        totalProfit = 0;
        completedFlipCount = 0;

        save();
    }

    // ========================================================
    // LOAD / SAVE
    // ========================================================

    private void load()
    {
        records.clear();

        sessionProfit = 0;
        todayProfit = 0;
        todayDate = LocalDate.now().toString();
        totalProfit = 0;
        completedFlipCount = 0;

        if (
                !storageFile.exists()
        )
        {
            return;
        }

        try (
                Reader reader =
                        storageFile.openBufferedReader()
        )
        {
            JsonElement root =
                    gson.fromJson(
                            reader,
                            JsonElement.class
                    );

            if (root == null)
            {
                return;
            }

            if (root.isJsonArray())
            {
                Type listType =
                        new TypeToken<
                                List<FlipRecord>
                                >()
                        {
                        }
                                .getType();

                List<FlipRecord> loaded =
                        gson.fromJson(
                                root,
                                listType
                        );

                if (loaded != null)
                {
                    records.addAll(
                            loaded
                    );
                }

                rebuildStatisticsFromHistory();
                save();
            }
            else
            {
                TrackerData loaded =
                        gson.fromJson(
                                root,
                                TrackerData.class
                        );

                if (
                        loaded != null
                                && loaded.records != null
                )
                {
                    records.addAll(
                            loaded.records
                    );
                }

                if (
                        loaded == null
                                || loaded.schemaVersion < 2
                )
                {
                    rebuildStatisticsFromHistory();
                }
                else
                {
                    todayProfit = loaded.todayProfit;
                    todayDate = loaded.todayDate;
                    totalProfit = loaded.totalProfit;
                    completedFlipCount = loaded.completedFlipCount;
                }
            }

            sortNewestFirst();

            rollDailyStatisticsIfNeeded();
        }
        catch (Exception ignored)
        {
        }
    }

    private void save()
    {
        try
        {
            storageFile.getParent()
                    .createDirectories();

            try (
                    Writer writer =
                            storageFile.openBufferedWriter()
            )
            {
                gson.toJson(
                        new TrackerData(
                                records,
                                todayProfit,
                                todayDate,
                                totalProfit,
                                completedFlipCount
                        ),
                        writer
                );
            }
        }
        catch (Exception ignored)
        {
        }
    }

    private void sortNewestFirst()
    {
        records.sort(
                Comparator.comparingLong(
                                FlipRecord::getTimestamp
                        )
                        .reversed()
        );
    }

    private void recordStatistics(
            FlipRecord record
    )
    {
        rollDailyStatisticsIfNeeded();

        sessionProfit += record.getNetProfit();
        todayProfit += record.getNetProfit();
        totalProfit += record.getNetProfit();
        completedFlipCount++;
    }

    private void rollDailyStatisticsIfNeeded()
    {
        String currentDate =
                LocalDate.now().toString();

        if (!currentDate.equals(todayDate))
        {
            todayDate = currentDate;
            todayProfit = 0;
        }
    }

    private void rebuildStatisticsFromHistory()
    {
        LocalDate currentDate =
                LocalDate.now();

        todayDate = currentDate.toString();
        todayProfit = 0;
        totalProfit = 0;
        completedFlipCount = records.size();

        for (FlipRecord record : records)
        {
            totalProfit += record.getNetProfit();

            LocalDate recordDate =
                    Instant.ofEpochSecond(
                                    record.getTimestamp()
                            )
                            .atZone(
                                    ZoneId.systemDefault()
                            )
                            .toLocalDate();

            if (currentDate.equals(recordDate))
            {
                todayProfit += record.getNetProfit();
            }
        }
    }

    private long safeMultiply(
            long first,
            long second
    )
    {
        try
        {
            return Math.multiplyExact(
                    first,
                    second
            );
        }
        catch (ArithmeticException exception)
        {
            throw new IllegalArgumentException(
                    "The entered values are too large."
            );
        }
    }

    private static class TrackerData
    {
        private int schemaVersion = 2;

        private List<FlipRecord> records =
                new ArrayList<>();

        private long todayProfit;

        private String todayDate;

        private long totalProfit;

        private int completedFlipCount;

        private TrackerData()
        {
        }

        private TrackerData(
                List<FlipRecord> records,
                long todayProfit,
                String todayDate,
                long totalProfit,
                int completedFlipCount
        )
        {
            this.records = new ArrayList<>(records);
            this.todayProfit = todayProfit;
            this.todayDate = todayDate;
            this.totalProfit = totalProfit;
            this.completedFlipCount = completedFlipCount;
        }
    }

    // ========================================================
    // RECORD
    // ========================================================

    public static class FlipRecord
    {
        private long timestamp;

        private String itemName;

        private long buyPrice;

        private long sellPrice;

        private long quantity;

        private long tax;

        private long netProfit;

        public FlipRecord()
        {
        }

        public FlipRecord(
                long timestamp,
                String itemName,
                long buyPrice,
                long sellPrice,
                long quantity,
                long tax,
                long netProfit
        )
        {
            this.timestamp =
                    timestamp;

            this.itemName =
                    itemName;

            this.buyPrice =
                    buyPrice;

            this.sellPrice =
                    sellPrice;

            this.quantity =
                    quantity;

            this.tax =
                    tax;

            this.netProfit =
                    netProfit;
        }

        public long getTimestamp()
        {
            return timestamp;
        }

        public String getItemName()
        {
            return itemName;
        }

        public long getBuyPrice()
        {
            return buyPrice;
        }

        public long getSellPrice()
        {
            return sellPrice;
        }

        public long getQuantity()
        {
            return quantity;
        }

        public long getTax()
        {
            return tax;
        }

        public long getNetProfit()
        {
            return netProfit;
        }
    }
}

