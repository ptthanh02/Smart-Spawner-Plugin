package me.nighter.smartSpawner.hooks.shops.api.shopguiplus;

import me.nighter.smartSpawner.SmartSpawner;
import me.nighter.smartSpawner.holders.StoragePageHolder;
import me.nighter.smartSpawner.hooks.shops.IShopIntegration;
import me.nighter.smartSpawner.hooks.shops.SaleLogger;
import me.nighter.smartSpawner.spawner.properties.VirtualInventory;
import me.nighter.smartSpawner.utils.ConfigManager;
import me.nighter.smartSpawner.utils.LanguageManager;
import me.nighter.smartSpawner.spawner.properties.SpawnerData;

import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.economy.EconomyManager;
import net.brcdev.shopgui.economy.EconomyType;
import net.brcdev.shopgui.provider.economy.EconomyProvider;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

public class ShopGuiPlus implements IShopIntegration {
    private final SmartSpawner plugin;
    private final LanguageManager languageManager;
    private final ConfigManager configManager;
    private final boolean isLoggingEnabled;

    // Cooldown system
    private final Map<UUID, Long> sellCooldowns = new ConcurrentHashMap<>();
    private static final long SELL_COOLDOWN_MS = 500; // 500ms cooldown

    // Transaction timeout
    private static final long TRANSACTION_TIMEOUT_MS = 5000; // 5 seconds timeout

    // Thread pool for async operations
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<UUID, CompletableFuture<Boolean>> pendingSales = new ConcurrentHashMap<>();

    public ShopGuiPlus(SmartSpawner plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.configManager = plugin.getConfigManager();
        this.isLoggingEnabled = configManager.isLoggingEnabled();
    }

    private boolean isOnCooldown(Player player) {
        long lastSellTime = sellCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastSellTime) < SELL_COOLDOWN_MS;
    }

    private void updateCooldown(Player player) {
        sellCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private void clearOldCooldowns() {
        long currentTime = System.currentTimeMillis();
        sellCooldowns.entrySet().removeIf(entry ->
                (currentTime - entry.getValue()) > SELL_COOLDOWN_MS * 10);
    }

    @Override
    public boolean sellAllItems(Player player, SpawnerData spawner) {
        // Check if shop system is enabled
        if (!isEnabled()) {
            return false;
        }

        // Prevent multiple concurrent sales for the same player
        if (pendingSales.containsKey(player.getUniqueId())) {
            languageManager.sendMessage(player, "messages.transaction-in-progress");
            return false;
        }

        // Check cooldown
        if (isOnCooldown(player)) {
            languageManager.sendMessage(player, "messages.sell-cooldown");
            return false;
        }

        // Get lock with timeout
        ReentrantLock lock = spawner.getLock();
        if (!lock.tryLock()) {
            languageManager.sendMessage(player, "messages.transaction-in-progress");
            return false;
        }

        try {
            // Start async sale process
            CompletableFuture<Boolean> saleFuture = CompletableFuture.supplyAsync(() ->
                    processSaleAsync(player, spawner), executorService);

            pendingSales.put(player.getUniqueId(), saleFuture);

            // Handle completion
            saleFuture.whenComplete((success, error) -> {
                pendingSales.remove(player.getUniqueId());
                lock.unlock();
                updateCooldown(player);

                if (error != null) {
                    plugin.getLogger().log(Level.SEVERE, "Error processing sale", error);
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            languageManager.sendMessage(player, "messages.sell-failed"));
                }
            });

            // Wait for a very short time to get immediate result if possible
            try {
                Boolean result = saleFuture.get(100, TimeUnit.MILLISECONDS);
                return result != null && result;
            } catch (TimeoutException e) {
                // Sale is still processing, return true to keep inventory open
                return true;
            } catch (Exception e) {
                return false;
            }
        } catch (Exception e) {
            lock.unlock();
            plugin.getLogger().log(Level.SEVERE, "Error initiating sale", e);
            return false;
        }
    }

    private boolean processSaleAsync(Player player, SpawnerData spawner) {
        VirtualInventory virtualInv = spawner.getVirtualInventory();
        Map<VirtualInventory.ItemSignature, Long> items = virtualInv.getConsolidatedItems();

        if (items.isEmpty()) {
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    languageManager.sendMessage(player, "messages.no-items"));
            return false;
        }

        // Calculate prices and prepare items by economy type
        SaleCalculationResult calculation = calculateSalePrices(player, items);
        if (!calculation.isValid()) {
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    languageManager.sendMessage(player, "messages.no-sellable-items"));
            return false;
        }

        // Pre-remove items to improve UX
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            virtualInv.removeItems(calculation.getItemsToRemove());
            // Force inventory update
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof StoragePageHolder) {
                StoragePageHolder holder = (StoragePageHolder) player.getOpenInventory().getTopInventory().getHolder();
                plugin.getSpawnerStorageUI().updateDisplay(
                        player.getOpenInventory().getTopInventory(),
                        holder.getSpawnerData(),
                        holder.getCurrentPage()
                );
            }
        });

        try {
            // Process transactions for each economy type
            CompletableFuture<Boolean> transactionFuture = new CompletableFuture<>();

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                boolean success = processTransactions(player, calculation);
                transactionFuture.complete(success);
            });

            boolean success = transactionFuture.get(TRANSACTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (!success) {
                // Restore items if payment fails
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    virtualInv.addItems(calculation.getItemsToRemove());
                    languageManager.sendMessage(player, "messages.sell-failed");
                    updateInventoryDisplay(player, spawner);
                });
                return false;
            }

            // Log sales asynchronously
            if (isLoggingEnabled) {
                logSalesAsync(calculation, player.getName());
            }

            // Send success message
            double taxPercentage = configManager.getTaxPercentage();
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    sendSuccessMessage(player, calculation.getTotalAmount(), calculation.getTotalPrice(), taxPercentage));

            return true;

        } catch (Exception e) {
            // Restore items on timeout/error
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                virtualInv.addItems(calculation.getItemsToRemove());
                languageManager.sendMessage(player, "messages.sell-failed");
                updateInventoryDisplay(player, spawner);
            });
            return false;
        }
    }

    private void updateInventoryDisplay(Player player, SpawnerData spawner) {
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof StoragePageHolder) {
            StoragePageHolder holder = (StoragePageHolder) player.getOpenInventory().getTopInventory().getHolder();
            plugin.getSpawnerStorageUI().updateDisplay(
                    player.getOpenInventory().getTopInventory(),
                    holder.getSpawnerData(),
                    holder.getCurrentPage()
            );
        }
    }

    private boolean processTransactions(Player player, SaleCalculationResult calculation) {
        double taxPercentage = configManager.getTaxPercentage();

        for (Map.Entry<EconomyType, Double> entry : calculation.getPricesByEconomy().entrySet()) {
            EconomyType economyType = entry.getKey();
            double totalPrice = entry.getValue();
            double finalPrice = calculateNetAmount(totalPrice, taxPercentage);

            try {
                EconomyProvider economyProvider = ShopGuiPlusApi.getPlugin().getEconomyManager()
                        .getEconomyProvider(economyType);

                if (economyProvider == null) {
                    plugin.getLogger().severe("No economy provider found for type: " + economyType);
                    return false;
                }

                economyProvider.deposit(player, finalPrice);
            } catch (Exception e) {
                plugin.getLogger().severe("Error processing transaction for economy " +
                        economyType + ": " + e.getMessage());
                return false;
            }
        }
        return true;
    }

    private double calculateNetAmount(double grossAmount, double taxPercentage) {
        if (taxPercentage <= 0) {
            return grossAmount;
        }
        return grossAmount * (1 - taxPercentage / 100.0);
    }

    private void logSalesAsync(SaleCalculationResult calculation, String playerName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (Map.Entry<String, SaleInfo> entry : calculation.getItemSales().entrySet()) {
                SaleInfo saleInfo = entry.getValue();
                SaleLogger.getInstance().logSale(
                        playerName,
                        entry.getKey(),
                        saleInfo.getAmount(),
                        saleInfo.getPrice(),
                        saleInfo.getEconomyType().name()
                );
            }
        });
    }

    private void sendSuccessMessage(Player player, int totalAmount, double totalPrice, double taxPercentage) {
        String formattedPrice = String.format("%.2f", totalPrice);
        if (taxPercentage > 0) {
            languageManager.sendMessage(player, "messages.sell-all-tax",
                    "%amount%", String.valueOf(totalAmount),
                    "%price%", formattedPrice,
                    "%tax%", String.format("%.2f", taxPercentage)
            );
        } else {
            languageManager.sendMessage(player, "messages.sell-all",
                    "%amount%", String.valueOf(totalAmount),
                    "%price%", formattedPrice);
        }
    }

    private SaleCalculationResult calculateSalePrices(Player player, Map<VirtualInventory.ItemSignature, Long> items) {
        Map<EconomyType, Double> pricesByEconomy = new HashMap<>();
        Map<String, SaleInfo> itemSales = new HashMap<>();
        List<ItemStack> itemsToRemove = new ArrayList<>();
        int totalAmount = 0;
        boolean foundSellableItem = false;

        for (Map.Entry<VirtualInventory.ItemSignature, Long> entry : items.entrySet()) {
            ItemStack template = entry.getKey().getTemplate();
            long amount = entry.getValue();

            if (amount <= 0) continue;

            double sellPrice = ShopGuiPlusApi.getItemStackPriceSell(player, template);
            if (sellPrice <= 0) continue;

            EconomyType economyType = getEconomyType(template);
            foundSellableItem = true;

            ItemStack itemToRemove = template.clone();
            int removeAmount = (int) Math.min(amount, Integer.MAX_VALUE);
            itemToRemove.setAmount(removeAmount);
            itemsToRemove.add(itemToRemove);

            double totalItemPrice = sellPrice * amount;
            pricesByEconomy.merge(economyType, totalItemPrice, Double::sum);
            totalAmount += removeAmount;

            // Store sale info for logging
            String itemName = template.getType().name();
            itemSales.put(itemName, new SaleInfo(removeAmount, totalItemPrice, economyType));
        }

        return new SaleCalculationResult(pricesByEconomy, totalAmount, itemsToRemove, itemSales, foundSellableItem);
    }

    private EconomyType getEconomyType(ItemStack material) {
        EconomyType economyType = ShopGuiPlusApi.getItemStackShop(material).getEconomyType();
        if(economyType != null) {
            return economyType;
        }

        EconomyManager economyManager = ShopGuiPlusApi.getPlugin().getEconomyManager();
        EconomyProvider defaultEconomyProvider = economyManager.getDefaultEconomyProvider();
        if(defaultEconomyProvider != null) {
            String defaultEconomyTypeName = defaultEconomyProvider.getName().toUpperCase(Locale.US);
            try {
                return EconomyType.valueOf(defaultEconomyTypeName);
            } catch(IllegalArgumentException ex) {
                return EconomyType.CUSTOM;
            }
        }

        return EconomyType.CUSTOM;
    }

    @Override
    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    @Override
    public boolean isEnabled() {
        return ShopGuiPlusApi.getPlugin().getShopManager().areShopsLoaded();
    }

    private static class SaleCalculationResult {
        private final Map<EconomyType, Double> pricesByEconomy;
        private final int totalAmount;
        private final List<ItemStack> itemsToRemove;
        private final Map<String, SaleInfo> itemSales;
        private final boolean valid;

        public SaleCalculationResult(Map<EconomyType, Double> pricesByEconomy,
                                     int totalAmount,
                                     List<ItemStack> itemsToRemove,
                                     Map<String, SaleInfo> itemSales,
                                     boolean valid) {
            this.pricesByEconomy = pricesByEconomy;
            this.totalAmount = totalAmount;
            this.itemsToRemove = itemsToRemove;
            this.itemSales = itemSales;
            this.valid = valid;
        }

        public Map<EconomyType, Double> getPricesByEconomy() {
            return pricesByEconomy;
        }

        public double getTotalPrice() {
            return pricesByEconomy.values().stream().mapToDouble(Double::doubleValue).sum();
        }

        public int getTotalAmount() {
            return totalAmount;
        }

        public List<ItemStack> getItemsToRemove() {
            return itemsToRemove;
        }

        public Map<String, SaleInfo> getItemSales() {
            return itemSales;
        }

        public boolean isValid() {
            return valid;
        }
    }

    private static class SaleInfo {
        private final int amount;
        private final double price;
        private final EconomyType economyType;

        public SaleInfo(int amount, double price, EconomyType economyType) {
            this.amount = amount;
            this.price = price;
            this.economyType = economyType;
        }

        public int getAmount() {
            return amount;
        }

        public double getPrice() {
            return price;
        }

        public EconomyType getEconomyType() {
            return economyType;
        }
    }
}