package com.runeradar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ProfitTrackerStore
{
    private static final double GE_TAX_RATE = 0.02;

    private static final long GE_TAX_CAP_PER_ITEM =
            5_000_000L;

    private final Gson gson =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private final Path storageFile;

    private final List<FlipRecord> records =
            new ArrayList<>();

    public ProfitTrackerStore()
    {
        storageFile =
                createStoragePath();

        load();
    }

    // ========================================================
    // STORAGE LOCATION
    // ========================================================

    private Path createStoragePath()
    {
        String localAppData =
                System.getenv(
                        "LOCALAPPDATA"
                );

        Path directory;

        if (
                localAppData != null
                        && !localAppData.isBlank()
        )
        {
            directory =
                    Paths.get(
                            localAppData,
                            "RuneRadar"
                    );
        }
        else
        {
            directory =
                    Paths.get(
                            System.getProperty(
                                    "user.home"
                            ),
                            ".runeradar"
                    );
        }

        try
        {
            Files.createDirectories(
                    directory
            );
        }
        catch (Exception ignored)
        {
        }

        return directory.resolve(
                "profit_history.json"
        );
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
        long total = 0;

        for (
                FlipRecord record
                : records
        )
        {
            if (
                    record.getTimestamp()
                            >= sessionStartedAt
            )
            {
                total +=
                        record.getNetProfit();
            }
        }

        return total;
    }

    public long getTodayProfit()
    {
        LocalDate today =
                LocalDate.now();

        long total = 0;

        for (
                FlipRecord record
                : records
        )
        {
            LocalDate recordDate =
                    Instant.ofEpochSecond(
                                    record.getTimestamp()
                            )
                            .atZone(
                                    ZoneId.systemDefault()
                            )
                            .toLocalDate();

            if (
                    recordDate.equals(
                            today
                    )
            )
            {
                total +=
                        record.getNetProfit();
            }
        }

        return total;
    }

    public long getTotalProfit()
    {
        long total = 0;

        for (
                FlipRecord record
                : records
        )
        {
            total +=
                    record.getNetProfit();
        }

        return total;
    }

    public int getCompletedFlipCount()
    {
        return records.size();
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

    // ========================================================
    // LOAD / SAVE
    // ========================================================

    private void load()
    {
        records.clear();

        if (
                !Files.exists(
                        storageFile
                )
        )
        {
            return;
        }

        try (
                Reader reader =
                        Files.newBufferedReader(
                                storageFile
                        )
        )
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
                            reader,
                            listType
                    );

            if (
                    loaded != null
            )
            {
                records.addAll(
                        loaded
                );
            }

            sortNewestFirst();
        }
        catch (Exception ignored)
        {
        }
    }

    private void save()
    {
        try
        {
            Files.createDirectories(
                    storageFile.getParent()
            );

            try (
                    Writer writer =
                            Files.newBufferedWriter(
                                    storageFile
                            )
            )
            {
                gson.toJson(
                        records,
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

