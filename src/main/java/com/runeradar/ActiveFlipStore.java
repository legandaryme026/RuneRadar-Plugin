package com.runeradar;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
public class ActiveFlipStore
{
    private final Gson gson;

    private static final Type ACTIVE_FLIP_LIST_TYPE =
            new TypeToken<List<ActiveFlip>>()
            {
            }.getType();

    private static final Path DATA_DIRECTORY =
            Paths.get(
                    System.getProperty("user.home"),
                    ".runelite",
                    "runeradar"
            );

    private static final Path DATA_FILE =
            DATA_DIRECTORY.resolve(
                    "active_flips.json"
            );

    private static final Path TEMP_FILE =
            DATA_DIRECTORY.resolve(
                    "active_flips.tmp"
            );

    private final List<ActiveFlip> activeFlips =
            new ArrayList<>();

    public ActiveFlipStore()
    {
        gson =
                RuneRadarApiClient
                        .getInjectedGson()
                        .newBuilder()
                        .setPrettyPrinting()
                        .create();

        load();
    }

    // ========================================================
    // READ
    // ========================================================

    public synchronized List<ActiveFlip> getAll()
    {
        return Collections.unmodifiableList(
                new ArrayList<>(
                        activeFlips
                )
        );
    }

    public synchronized int size()
    {
        return activeFlips.size();
    }

    public synchronized boolean isEmpty()
    {
        return activeFlips.isEmpty();
    }

    public synchronized ActiveFlip findById(
            String id
    )
    {
        if (id == null)
        {
            return null;
        }

        for (ActiveFlip flip : activeFlips)
        {
            if (id.equals(
                    flip.getId()
            ))
            {
                return flip;
            }
        }

        return null;
    }

    public synchronized ActiveFlip findOpenByItemId(
            int itemId
    )
    {
        for (ActiveFlip flip : activeFlips)
        {
            if (
                    flip != null
                            && !flip.isCompleted()
                            && flip.getItemId() == itemId
            )
            {
                return flip;
            }
        }

        return null;
    }

    // ========================================================
    // CREATE
    // ========================================================

    public synchronized ActiveFlip addRecommendation(
            int itemId,
            String itemName,
            long recommendedBuyPrice,
            long targetSellPrice,
            int recommendedQuantity,
            long expectedNetProfit
    )
    {
        return addRecommendation(
                itemId,
                itemName,
                recommendedBuyPrice,
                targetSellPrice,
                recommendedQuantity,
                expectedNetProfit,
                "BALANCED"
        );
    }

    public synchronized ActiveFlip addRecommendation(
            int itemId,
            String itemName,
            long recommendedBuyPrice,
            long targetSellPrice,
            int recommendedQuantity,
            long expectedNetProfit,
            String trackingCategory
    )
    {
        ActiveFlip existingFlip =
                findOpenByItemId(
                        itemId
                );

        if (existingFlip != null)
        {
            return existingFlip;
        }

        ActiveFlip flip =
                new ActiveFlip(
                        UUID.randomUUID()
                                .toString(),
                        itemId,
                        itemName,
                        recommendedBuyPrice,
                        targetSellPrice,
                        recommendedQuantity,
                        expectedNetProfit,
                        trackingCategory,
                        System.currentTimeMillis()
                );

        activeFlips.add(
                flip
        );

        save();

        return flip;
    }

    public synchronized void add(
            ActiveFlip flip
    )
    {
        if (flip == null)
        {
            return;
        }

        if (
                flip.getId() == null
                        || flip.getId()
                        .trim()
                        .isEmpty()
        )
        {
            flip.setId(
                    UUID.randomUUID()
                            .toString()
            );
        }

        long now =
                System.currentTimeMillis();

        if (flip.getCreatedAt() <= 0)
        {
            flip.setCreatedAt(
                    now
            );
        }

        flip.setUpdatedAt(
                now
        );

        activeFlips.add(
                flip
        );

        save();
    }

    // ========================================================
    // UPDATE
    // ========================================================

    public synchronized void update(
            ActiveFlip updatedFlip
    )
    {
        if (
                updatedFlip == null
                        || updatedFlip.getId() == null
        )
        {
            return;
        }

        for (
                int index = 0;
                index < activeFlips.size();
                index++
        )
        {
            ActiveFlip existing =
                    activeFlips.get(
                            index
                    );

            if (
                    updatedFlip.getId()
                            .equals(
                                    existing.getId()
                            )
            )
            {
                updatedFlip.setUpdatedAt(
                        System.currentTimeMillis()
                );

                activeFlips.set(
                        index,
                        updatedFlip
                );

                save();

                return;
            }
        }
    }

    // ========================================================
    // BUY TRACKING
    // ========================================================

    public synchronized void recordBuy(
            String flipId,
            int quantity,
            long pricePerItem
    )
    {
        if (
                quantity <= 0
                        || pricePerItem <= 0
        )
        {
            return;
        }

        recordBuyTotal(
                flipId,
                quantity,
                pricePerItem
                        * (long) quantity
        );
    }

    public synchronized void recordBuyTotal(
            String flipId,
            int quantity,
            long totalCost
    )
    {
        if (
                quantity <= 0
                        || totalCost <= 0
        )
        {
            return;
        }

        ActiveFlip flip =
                findById(
                        flipId
                );

        if (
                flip == null
                        || flip.isCompleted()
        )
        {
            return;
        }

        flip.setBoughtQuantity(
                flip.getBoughtQuantity()
                        + quantity
        );

        flip.setTotalBuyCost(
                flip.getTotalBuyCost()
                        + totalCost
        );

        if (
                flip.getBoughtQuantity()
                        >= flip.getRecommendedQuantity()
        )
        {
            flip.setStatus(
                    ActiveFlip.STATUS_READY_TO_SELL
            );
        }
        else
        {
            flip.setStatus(
                    ActiveFlip.STATUS_BUYING
            );
        }

        long now =
                System.currentTimeMillis();

        flip.setLastProgressAt(
                now
        );

        flip.setUpdatedAt(
                now
        );

        save();
    }

    // ========================================================
    // SELL TRACKING
    // ========================================================

    public synchronized void recordSale(
            String flipId,
            int quantity,
            long pricePerItem,
            long tax
    )
    {
        if (
                quantity <= 0
                        || pricePerItem <= 0
        )
        {
            return;
        }

        recordSaleTotal(
                flipId,
                quantity,
                pricePerItem
                        * (long) quantity,
                tax
        );
    }

    public synchronized void recordSaleTotal(
            String flipId,
            int quantity,
            long totalSellGross,
            long tax
    )
    {
        if (
                quantity <= 0
                        || totalSellGross <= 0
        )
        {
            return;
        }

        ActiveFlip flip =
                findById(
                        flipId
                );

        if (
                flip == null
                        || flip.isCompleted()
        )
        {
            return;
        }

        int openQuantity =
                flip.getOpenQuantity();

        if (openQuantity <= 0)
        {
            return;
        }

        int acceptedQuantity =
                Math.min(
                        quantity,
                        openQuantity
                );

        long acceptedSellGross =
                totalSellGross;

        long acceptedTax =
                Math.max(
                        tax,
                        0
                );

        if (acceptedQuantity != quantity)
        {
            acceptedSellGross =
                    totalSellGross
                            * acceptedQuantity
                            / quantity;

            acceptedTax =
                    acceptedTax
                            * acceptedQuantity
                            / quantity;
        }

        flip.setSoldQuantity(
                flip.getSoldQuantity()
                        + acceptedQuantity
        );

        flip.setTotalSellGross(
                flip.getTotalSellGross()
                        + acceptedSellGross
        );

        flip.setTotalTax(
                flip.getTotalTax()
                        + acceptedTax
        );

        if (
                flip.getBoughtQuantity() > 0
                        && flip.getSoldQuantity()
                        >= flip.getBoughtQuantity()
        )
        {
            flip.setStatus(
                    ActiveFlip.STATUS_COMPLETED
            );

            if (flip.getCompletedAt() <= 0)
            {
                flip.setCompletedAt(
                        System.currentTimeMillis()
                );
            }
        }
        else
        {
            flip.setStatus(
                    ActiveFlip.STATUS_SELLING
            );
        }

        long now =
                System.currentTimeMillis();

        flip.setLastProgressAt(
                now
        );

        flip.setUpdatedAt(
                now
        );

        save();
    }

    // ========================================================
    // STATUS
    // ========================================================

    public synchronized void setStatus(
            String flipId,
            String status
    )
    {
        ActiveFlip flip =
                findById(
                        flipId
                );

        if (flip == null)
        {
            return;
        }

        String previousStatus =
                flip.getStatus();

        boolean statusChanged =
                previousStatus == null
                        || !previousStatus.equals(
                        status
                );

        if (!statusChanged)
        {
            return;
        }

        long now =
                System.currentTimeMillis();

        flip.setStatus(
                status
        );

        if (
                ActiveFlip.STATUS_BUYING.equals(
                        status
                )
                        || ActiveFlip.STATUS_SELLING.equals(
                        status
                )
        )
        {
            flip.setLastProgressAt(
                    now
            );
        }

        flip.setUpdatedAt(
                now
        );

        save();
    }

    public synchronized List<ActiveFlip> getCompletedUnrecorded()
    {
        List<ActiveFlip> completed =
                new ArrayList<>();

        for (ActiveFlip flip : activeFlips)
        {
            if (
                    flip != null
                            && flip.isCompleted()
                            && !flip.isProfitTrackerRecorded()
            )
            {
                completed.add(
                        flip
                );
            }
        }

        return completed;
    }

    public synchronized void markProfitTrackerRecorded(
            String flipId
    )
    {
        ActiveFlip flip =
                findById(
                        flipId
                );

        if (flip == null)
        {
            return;
        }

        flip.setProfitTrackerRecorded(
                true
        );

        flip.setUpdatedAt(
                System.currentTimeMillis()
        );

        save();
    }

    // ========================================================
    // DELETE
    // ========================================================

    public synchronized boolean remove(
            String flipId
    )
    {
        if (flipId == null)
        {
            return false;
        }

        boolean removed =
                activeFlips.removeIf(
                        flip ->
                                flipId.equals(
                                        flip.getId()
                                )
                );

        if (removed)
        {
            save();
        }

        return removed;
    }

    public synchronized void clear()
    {
        activeFlips.clear();

        save();
    }

    // ========================================================
    // LOAD
    // ========================================================

    private synchronized void load()
    {
        activeFlips.clear();

        try
        {
            Files.createDirectories(
                    DATA_DIRECTORY
            );

            if (
                    !Files.exists(
                            DATA_FILE
                    )
            )
            {
                return;
            }

            String json =
                    new String(
                            Files.readAllBytes(
                                    DATA_FILE
                            ),
                            StandardCharsets.UTF_8
                    );

            if (
                    json.trim()
                            .isEmpty()
            )
            {
                return;
            }

            List<ActiveFlip> loaded =
                    gson.fromJson(
                            json,
                            ACTIVE_FLIP_LIST_TYPE
                    );

            if (loaded == null)
            {
                return;
            }

            for (ActiveFlip flip : loaded)
            {
                if (flip == null)
                {
                    continue;
                }

                normalizeLoadedFlip(
                        flip
                );

                activeFlips.add(
                        flip
                );
            }
        }
        catch (Exception exception)
        {
            log.warn(
                    "RuneRadar could not load active flips",
                    exception
            );
        }
    }

    private void normalizeLoadedFlip(
            ActiveFlip flip
    )
    {
        if (
                flip.getId() == null
                        || flip.getId()
                        .trim()
                        .isEmpty()
        )
        {
            flip.setId(
                    UUID.randomUUID()
                            .toString()
            );
        }

        if (
                flip.getStatus() == null
                        || flip.getStatus()
                        .trim()
                        .isEmpty()
        )
        {
            flip.setStatus(
                    ActiveFlip.STATUS_PLANNED
            );
        }

        if (flip.getCreatedAt() <= 0)
        {
            flip.setCreatedAt(
                    System.currentTimeMillis()
            );
        }

        if (flip.getUpdatedAt() <= 0)
        {
            flip.setUpdatedAt(
                    flip.getCreatedAt()
            );
        }

        if (flip.getLastProgressAt() <= 0)
        {
            flip.setLastProgressAt(
                    flip.getCreatedAt()
            );
        }

        flip.setTrackingCategory(
                flip.getTrackingCategory()
        );
    }

    // ========================================================
    // SAVE
    // ========================================================

    private synchronized void save()
    {
        try
        {
            Files.createDirectories(
                    DATA_DIRECTORY
            );

            String json =
                    gson.toJson(
                            activeFlips
                    );

            Files.write(
                    TEMP_FILE,
                    json.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            try
            {
                Files.move(
                        TEMP_FILE,
                        DATA_FILE,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            }
            catch (IOException atomicMoveException)
            {
                Files.move(
                        TEMP_FILE,
                        DATA_FILE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        }
        catch (Exception exception)
        {
            log.warn(
                    "RuneRadar could not save active flips",
                    exception
            );
        }
    }

    // ========================================================
    // ACTIVE FLIP MODEL
    // ========================================================

    public static class ActiveFlip
    {
        public static final String STATUS_PLANNED =
                "PLANNED";

        public static final String STATUS_BUYING =
                "BUYING";

        public static final String STATUS_READY_TO_SELL =
                "READY_TO_SELL";

        public static final String STATUS_SELLING =
                "SELLING";

        public static final String STATUS_COMPLETED =
                "COMPLETED";

        private String id;

        private int itemId;

        private String itemName;

        private long recommendedBuyPrice;

        private long targetSellPrice;

        private int recommendedQuantity;

        private long expectedNetProfit;

        private String trackingCategory;

        private long lastProgressAt;

        private int boughtQuantity;

        private int soldQuantity;

        private long totalBuyCost;

        private long totalSellGross;

        private long totalTax;

        private String status;

        private long createdAt;

        private long updatedAt;

        private long completedAt;

        private boolean profitTrackerRecorded;

        public ActiveFlip()
        {
        }

        public ActiveFlip(
                String id,
                int itemId,
                String itemName,
                long recommendedBuyPrice,
                long targetSellPrice,
                int recommendedQuantity,
                long expectedNetProfit,
                String trackingCategory,
                long createdAt
        )
        {
            this.id =
                    id;

            this.itemId =
                    itemId;

            this.itemName =
                    itemName;

            this.recommendedBuyPrice =
                    recommendedBuyPrice;

            this.targetSellPrice =
                    targetSellPrice;

            this.recommendedQuantity =
                    recommendedQuantity;

            this.expectedNetProfit =
                    expectedNetProfit;

            setTrackingCategory(
                    trackingCategory
            );

            this.lastProgressAt =
                    createdAt;

            this.boughtQuantity =
                    0;

            this.soldQuantity =
                    0;

            this.totalBuyCost =
                    0;

            this.totalSellGross =
                    0;

            this.totalTax =
                    0;

            this.status =
                    STATUS_PLANNED;

            this.createdAt =
                    createdAt;

            this.updatedAt =
                    createdAt;

            this.completedAt =
                    0;

            this.profitTrackerRecorded =
                    false;
        }

        // ====================================================
        // CALCULATED VALUES
        // ====================================================

        public long getAverageBuyPrice()
        {
            if (boughtQuantity <= 0)
            {
                return 0;
            }

            return totalBuyCost
                    / boughtQuantity;
        }

        public long getAverageSellPrice()
        {
            if (soldQuantity <= 0)
            {
                return 0;
            }

            return totalSellGross
                    / soldQuantity;
        }

        public long getNetSaleValue()
        {
            return totalSellGross
                    - totalTax;
        }

        public long getRealizedNetProfit()
        {
            if (
                    soldQuantity <= 0
                            || boughtQuantity <= 0
            )
            {
                return 0;
            }

            int matchedQuantity =
                    Math.min(
                            boughtQuantity,
                            soldQuantity
                    );

            long matchedBuyCost =
                    totalBuyCost
                            * matchedQuantity
                            / boughtQuantity;

            long matchedSellGross;

            long matchedTax;

            if (soldQuantity <= boughtQuantity)
            {
                matchedSellGross =
                        totalSellGross;

                matchedTax =
                        totalTax;
            }
            else
            {
                matchedSellGross =
                        totalSellGross
                                * matchedQuantity
                                / soldQuantity;

                matchedTax =
                        totalTax
                                * matchedQuantity
                                / soldQuantity;
            }

            return matchedSellGross
                    - matchedTax
                    - matchedBuyCost;
        }

        public int getOpenQuantity()
        {
            return Math.max(
                    0,
                    boughtQuantity
                            - soldQuantity
            );
        }

        public boolean isCompleted()
        {
            return STATUS_COMPLETED.equals(
                    status
            );
        }

        public boolean isProfitTrackerRecorded()
        {
            return profitTrackerRecorded;
        }

        // ====================================================
        // GETTERS / SETTERS
        // ====================================================

        public String getId()
        {
            return id;
        }

        public void setId(
                String id
        )
        {
            this.id =
                    id;
        }

        public int getItemId()
        {
            return itemId;
        }

        public void setItemId(
                int itemId
        )
        {
            this.itemId =
                    itemId;
        }

        public String getItemName()
        {
            return itemName;
        }

        public void setItemName(
                String itemName
        )
        {
            this.itemName =
                    itemName;
        }

        public long getRecommendedBuyPrice()
        {
            return recommendedBuyPrice;
        }

        public void setRecommendedBuyPrice(
                long recommendedBuyPrice
        )
        {
            this.recommendedBuyPrice =
                    recommendedBuyPrice;
        }

        public long getTargetSellPrice()
        {
            return targetSellPrice;
        }

        public void setTargetSellPrice(
                long targetSellPrice
        )
        {
            this.targetSellPrice =
                    targetSellPrice;
        }

        public int getRecommendedQuantity()
        {
            return recommendedQuantity;
        }

        public void setRecommendedQuantity(
                int recommendedQuantity
        )
        {
            this.recommendedQuantity =
                    recommendedQuantity;
        }

        public long getExpectedNetProfit()
        {
            return expectedNetProfit;
        }

        public void setExpectedNetProfit(
                long expectedNetProfit
        )
        {
            this.expectedNetProfit =
                    expectedNetProfit;
        }

        public String getTrackingCategory()
        {
            return trackingCategory;
        }

        public void setTrackingCategory(
                String trackingCategory
        )
        {
            String cleaned =
                    trackingCategory == null
                            ? ""
                            : trackingCategory
                            .trim()
                            .toUpperCase();

            if (
                    !"FAST".equals(
                            cleaned
                    )
                            && !"BALANCED".equals(
                            cleaned
                    )
                            && !"SLOW".equals(
                            cleaned
                    )
                            && !"HIGH_PROFIT".equals(
                            cleaned
                    )
            )
            {
                cleaned =
                        "BALANCED";
            }

            this.trackingCategory =
                    cleaned;
        }

        public long getLastProgressAt()
        {
            return lastProgressAt;
        }

        public void setLastProgressAt(
                long lastProgressAt
        )
        {
            this.lastProgressAt =
                    lastProgressAt;
        }

        public int getBoughtQuantity()
        {
            return boughtQuantity;
        }

        public void setBoughtQuantity(
                int boughtQuantity
        )
        {
            this.boughtQuantity =
                    Math.max(
                            boughtQuantity,
                            0
                    );
        }

        public int getSoldQuantity()
        {
            return soldQuantity;
        }

        public void setSoldQuantity(
                int soldQuantity
        )
        {
            this.soldQuantity =
                    Math.max(
                            soldQuantity,
                            0
                    );
        }

        public long getTotalBuyCost()
        {
            return totalBuyCost;
        }

        public void setTotalBuyCost(
                long totalBuyCost
        )
        {
            this.totalBuyCost =
                    Math.max(
                            totalBuyCost,
                            0
                    );
        }

        public long getTotalSellGross()
        {
            return totalSellGross;
        }

        public void setTotalSellGross(
                long totalSellGross
        )
        {
            this.totalSellGross =
                    Math.max(
                            totalSellGross,
                            0
                    );
        }

        public long getTotalTax()
        {
            return totalTax;
        }

        public void setTotalTax(
                long totalTax
        )
        {
            this.totalTax =
                    Math.max(
                            totalTax,
                            0
                    );
        }

        public String getStatus()
        {
            return status;
        }

        public void setStatus(
                String status
        )
        {
            if (
                    status == null
                            || status.trim()
                            .isEmpty()
            )
            {
                this.status =
                        STATUS_PLANNED;

                return;
            }

            this.status =
                    status;
        }

        public long getCreatedAt()
        {
            return createdAt;
        }

        public void setCreatedAt(
                long createdAt
        )
        {
            this.createdAt =
                    createdAt;
        }

        public long getUpdatedAt()
        {
            return updatedAt;
        }

        public void setUpdatedAt(
                long updatedAt
        )
        {
            this.updatedAt =
                    updatedAt;
        }

        public long getCompletedAt()
        {
            return completedAt;
        }

        public void setCompletedAt(
                long completedAt
        )
        {
            this.completedAt =
                    completedAt;
        }

        public void setProfitTrackerRecorded(
                boolean profitTrackerRecorded
        )
        {
            this.profitTrackerRecorded =
                    profitTrackerRecorded;
        }
    }
}
