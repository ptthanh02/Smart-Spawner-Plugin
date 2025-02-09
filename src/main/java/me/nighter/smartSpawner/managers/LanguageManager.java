package me.nighter.smartSpawner.managers;
import me.nighter.smartSpawner.utils.SupportedLanguage;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LanguageManager {
    private final Plugin plugin;
    private final Logger logger;
    private final Map<String, FileConfiguration> languageMessages;
    private FileConfiguration messages;
    private SupportedLanguage currentLanguage;
    private final Map<String, String> messageCache;
    private static final int CACHE_SIZE = 100;

    public enum MessageType {
        CHAT,
        ACTION_BAR,
        BOTH
    }

    private final Map<String, Object> defaultMessages = new HashMap<String, Object>() {{
        // Prefix
        put("prefix", "&8[&#3287A9&lSmartSpawner&8]&r");

        // Format Numbers
        put("format-number.thousand", "%sK");
        put("format-number.million", "%sM");
        put("format-number.billion", "%sB");
        put("format-number.trillion", "%sT");
        put("format-number.default", "%s");

        // Mob Names
        put("mob_names.BLAZE", "Blaze");
        put("mob_names.BOGGED", "Bogged");
        put("mob_names.BREEZE", "Breeze");
        put("mob_names.CAVE_SPIDER", "Cave Spider");
        put("mob_names.CHICKEN", "Chicken");
        put("mob_names.COW", "Cow");
        put("mob_names.CREEPER", "Creeper");
        put("mob_names.DROWNED", "Drowned");
        put("mob_names.ENDERMAN", "Enderman");
        put("mob_names.EVOKER", "Evoker");
        put("mob_names.GHAST", "Ghast");
        put("mob_names.GLOW_SQUID", "Glow Squid");
        put("mob_names.GUARDIAN", "Guardian");
        put("mob_names.HOGLIN", "Hoglin");
        put("mob_names.HUSK", "Husk");
        put("mob_names.IRON_GOLEM", "Iron Golem");
        put("mob_names.MAGMA_CUBE", "Magma Cube");
        put("mob_names.MOOSHROOM", "Mooshroom");
        put("mob_names.PIG", "Pig");
        put("mob_names.PIGLIN", "Piglin");
        put("mob_names.PIGLIN_BRUTE", "Piglin Brute");
        put("mob_names.PILLAGER", "Pillager");
        put("mob_names.PUFFERFISH", "Pufferfish");
        put("mob_names.RABBIT", "Rabbit");
        put("mob_names.RAVAGER", "Ravager");
        put("mob_names.SALMON", "Salmon");
        put("mob_names.SHEEP", "Sheep");
        put("mob_names.SHULKER", "Shulker");
        put("mob_names.SILVERFISH", "Silverfish");
        put("mob_names.SKELETON", "Skeleton");
        put("mob_names.SLIME", "Slime");
        put("mob_names.SPIDER", "Spider");
        put("mob_names.SQUID", "Squid");
        put("mob_names.STRAY", "Stray");
        put("mob_names.STRIDER", "Strider");
        put("mob_names.TROPICAL_FISH", "Tropical Fish");
        put("mob_names.VINDICATOR", "Vindicator");
        put("mob_names.WITCH", "Witch");
        put("mob_names.WITHER_SKELETON", "Wither Skeleton");
        put("mob_names.ZOGLIN", "Zoglin");
        put("mob_names.ZOMBIE", "Zombie");
        put("mob_names.ZOMBIE_VILLAGER", "Zombie Villager");
        put("mob_names.ZOMBIFIED_PIGLIN", "Zombified Piglin");

        // Spawner Name With Entity
        put("spawner-name", "&6%entity% Spawner");

        // Spawner Interaction Messages
        put("messages.activated.message", "&#d6e7edSpawner &#3287A9activated&#d6e7ed! Mobs won’t spawn naturally, collect loot and XP through the GUI instead.");
        put("messages.activated.prefix", true);
        put("messages.activated.type", "CHAT");
        put("messages.activated.sound", "entity.experience_orb.pickup");

        put("messages.entity-spawner-placed.message", "&#d6e7edThis spawner is &#3287A9not activated&#d6e7ed! Mobs will spawn naturally.");
        put("messages.entity-spawner-placed.prefix", true);
        put("messages.entity-spawner-placed.type", "CHAT");
        put("messages.entity-spawner-placed.sound", "block.note_block.pling");

        put("messages.changed.message", "&#d6e7edSpawner changed to &#3287A9%type%&#d6e7ed!");
        put("messages.changed.prefix", true);
        put("messages.changed.type", "CHAT");
        put("messages.changed.sound", "block.note_block.pling");

        put("messages.invalid-egg.message", "&cInvalid spawn egg! or spawn egg not supported!");
        put("messages.invalid-egg.prefix", true);
        put("messages.invalid-egg.type", "CHAT");

        put("messages.break-warning.message", "&c[!] Warning! All items and xp will be lost!");
        put("messages.break-warning.prefix", false);
        put("messages.break-warning.type", "ACTION_BAR");
        put("messages.break-warning.sound", "block.note_block.chime");

        put("messages.required-tools.message", "&c[!] Can't break spawner with this tool!");
        put("messages.required-tools.prefix", false);
        put("messages.required-tools.type", "ACTION_BAR");
        put("messages.required-tools.sound", "item.shield.block");

        put("messages.silk-touch-required.message", "&c[!] Required &#3287A9Silk Touch&c to break this spawner!");
        put("messages.silk-touch-required.prefix", false);
        put("messages.silk-touch-required.type", "ACTION_BAR");
        put("messages.silk-touch-required.sound", "block.note_block.pling");

        put("messages.spawner-in-use.message", "&c[!] Other player is currently using this spawner! Please wait.");
        put("messages.spawner-in-use.prefix", false);
        put("messages.spawner-in-use.type", "ACTION_BAR");
        put("messages.spawner-in-use.sound", "block.note_block.pling");

        put("messages.spawner-protected.message", "&c[!] This spawner is protected!");
        put("messages.spawner-protected.prefix", false);
        put("messages.spawner-protected.type", "ACTION_BAR");
        put("messages.spawner-protected.sound", "block.note_block.pling");

        // Selling Items from Spawner
        put("messages.sell-all.message", "&#d6e7edYou sold a total of &#3287A9%amount% items&#d6e7ed for&a %price%$ &#d6e7ed!");
        put("messages.sell-all.prefix", true);
        put("messages.sell-all.type", "CHAT");
        put("messages.sell-all.sound", "block.note_block.bell");

        put("messages.no-items.message", "&cThere are no items to sell in the spawner.");
        put("messages.no-items.prefix", true);
        put("messages.no-items.type", "CHAT");
        put("messages.no-items.sound", "block.note_block.pling");

        put("messages.no-sellable-items.message", "&cNo items can be sold from this spawner.");
        put("messages.no-sellable-items.prefix", true);
        put("messages.no-sellable-items.type", "CHAT");
        put("messages.no-sellable-items.sound", "block.note_block.pling");

        put("messages.sell-failed.message", "&cFailed to sell items! Please try again.");
        put("messages.sell-failed.prefix", true);
        put("messages.sell-failed.type", "CHAT");
        put("messages.sell-failed.sound", "block.note_block.pling");

        // Spawner Stacking/Unstacking Messages
        put("messages.hand-stack.message", "&#00E689[✔] Successfully stacked &6%amount%&#00E689 spawners!");
        put("messages.hand-stack.prefix", false);
        put("messages.hand-stack.type", "ACTION_BAR");
        put("messages.hand-stack.sound", "entity.experience_orb.pickup");

        put("messages.items-lost.message", "&cSome items were lost due to unstacking!");
        put("messages.items-lost.prefix", true);
        put("messages.items-lost.type", "CHAT");
        put("messages.items-lost.sound", "block.note_block.pling");

        put("messages.cannot-go-below-one.message", "&cCannot go below 1! Only decreasing by %amount%!");
        put("messages.cannot-go-below-one.prefix", true);
        put("messages.cannot-go-below-one.type", "CHAT");
        put("messages.cannot-go-below-one.sound", "block.note_block.pling");

        put("messages.stack-full.message", "&cStack limit reached! Cannot increase anymore!");
        put("messages.stack-full.prefix", true);
        put("messages.stack-full.type", "CHAT");
        put("messages.stack-full.sound", "block.note_block.pling");

        put("messages.not-enough-spawners.message", "&cYou don't have enough spawners! Need %amountChange% but only have %amountAvailable%!");
        put("messages.not-enough-spawners.prefix", true);
        put("messages.not-enough-spawners.type", "CHAT");
        put("messages.not-enough-spawners.sound", "block.note_block.pling");

        put("messages.stack-full-overflow.message", "&cStack limit reached! Only stack %amount% spawners!");
        put("messages.stack-full-overflow.prefix", true);
        put("messages.stack-full-overflow.type", "CHAT");
        put("messages.stack-full-overflow.sound", "block.note_block.pling");

        put("messages.inventory-full-drop.message", "&cSome spawners were dropped at your feet due to full inventory!");
        put("messages.inventory-full-drop.prefix", true);
        put("messages.inventory-full-drop.type", "CHAT");
        put("messages.inventory-full-drop.sound", "block.note_block.pling");

        put("messages.invalid-spawner.message", "&cInvalid spawner type!");
        put("messages.invalid-spawner.prefix", true);
        put("messages.invalid-spawner.type", "CHAT");
        put("messages.invalid-spawner.sound", "block.note_block.pling");

        put("messages.different-type.message", "&cYou can only stack spawners of the same type!");
        put("messages.different-type.prefix", true);
        put("messages.different-type.type", "CHAT");
        put("messages.different-type.sound", "block.note_block.pling");

        // Spawner Experience Collection
        put("messages.exp-collected.message", "&#d6e7edCollected &a%exp%&#d6e7ed experience points!");
        put("messages.exp-collected.prefix", true);
        put("messages.exp-collected.type", "CHAT");

        put("messages.exp-collected-with-mending.message", "&#d6e7edUsed &a%exp-mending%&d6e7ed experience points to repair items! Collected &a%exp%&d6e7ed experience points!");
        put("messages.exp-collected-with-mending.prefix", true);
        put("messages.exp-collected-with-mending.type", "CHAT");

        put("messages.no-exp.message", "&cThere is no experience to take!");
        put("messages.no-exp.prefix", true);
        put("messages.no-exp.type", "CHAT");
        put("messages.no-exp.sound", "block.note_block.pling");

        // Spawner Storage Interaction
        put("messages.no-items-to-take.message", "&cThere are no items to take!");
        put("messages.no-items-to-take.prefix", true);
        put("messages.no-items-to-take.type", "CHAT");
        put("messages.no-items-to-take.sound", "block.note_block.pling");

        put("messages.inventory-full.message", "&cYour inventory is full!");
        put("messages.inventory-full.prefix", true);
        put("messages.inventory-full.type", "CHAT");
        put("messages.inventory-full.sound", "block.note_block.pling");

        put("messages.take-some-items.message", "&#d6e7edYou have taken &3287A9%amount%&d6e7ed items! Your inventory is now full!");
        put("messages.take-some-items.prefix", true);
        put("messages.take-some-items.type", "CHAT");

        put("messages.take-all-items.message", "&#d6e7edSuccessfully taken &3287A9%amount%&d6e7ed items!");
        put("messages.take-all-items.prefix", true);
        put("messages.take-all-items.type", "CHAT");
        put("messages.take-all-items.sound", "block.note_block.chime");

        // Hopper Interaction
        put("messages.hopper-paused.message", "&cHopper has been paused while interacting with the spawner!");
        put("messages.hopper-paused.prefix", true);
        put("messages.hopper-paused.type", "CHAT");
        put("messages.hopper-paused.sound", "block.note_block.pling");

        put("messages.hopper-resumed.message", "&aHopper has been resumed!");
        put("messages.hopper-resumed.prefix", true);
        put("messages.hopper-resumed.type", "CHAT");
        put("messages.hopper-resumed.sound", "block.note_block.chime");

        // Spawner List Teleport Message
        put("messages.teleported.message", "&aSuccessfully teleported to &6Spawner #%spawnerId%");
        put("messages.teleported.prefix", true);
        put("messages.teleported.type", "CHAT");
        put("messages.teleported.sound", "entity.enderman.teleport");

        put("messages.not-found.message", "&cCould not teleport to that Spawner! Spawner not found.");
        put("messages.not-found.prefix", true);
        put("messages.not-found.type", "CHAT");
        put("messages.not-found.sound", "block.note_block.pling");

        // GUI Titles
        put("gui-title.menu", "%entity% Spawner");
        put("gui-title.stacked-menu", "%amount% %entity% Spawner");
        put("gui-title.stacker-menu", "Spawner Stacker");
        put("gui-title.loot-menu", "Spawner Storage");

        // GUI Items - Spawner Loot
        put("spawner-loot-item.name", "&#FCD05C&lLoot Storage");
        put("spawner-loot-item.lore.chest", "\n&8▪ &#FCD05CSlots: &f%current_items%&7/&f%max_slots%\n&8▪ &#FCD05CStorage: &a%percent_storage%&a%&f full\n\n&#FCD05C➜ &7Click to open");

        // GUI Items - Spawner Info
        put("spawner-info-item.name", "&#4fc3f7&lSpawner Info");
        put("spawner-info-item.lore.spawner-info", "\n&8▪ &#81d4faEntity: &f%entity%\n&8▪ &#81d4faRange: &f%range% &7blocks\n&8▪ &#81d4faStack Size: &f%stack_size%\n&8▪ &#81d4faMob Rate: &f%min_mobs% &7- &f%max_mobs%\n&8▪ &#81d4faSpawn Delay: &f%delay%&7s\n&8▪ &#81d4faNext Spawn: &e\n&8&m\n&#81d4fa➜ &7Click to open stack menu");
        put("spawner-info-item.lore-change", "&8▪ &#81d4faNext Spawn: &e");
        put("spawner-info-item.lore-inactive", "&cSpawner is inactive!");
        put("spawner-info-item.lore-now", "&cNow!");
        put("spawner-info-item.lore-error", "&cError in spawn time! Please restart the server.");

        // Experience Info
        put("exp-info-item.name", "&#00F898&lStored Exp: &e%current_exp%&#00F898");
        put("exp-info-item.lore.exp-bottle", "\n&8▪ &#00E689Current: &e%current_exp%&7/&e%max_exp% XP\n&8▪ &#00E689Stored: &e%percent_exp%&e%&7 XP\n\n&#00E689➜ &7Click to collect XP");

        // Stacker GUI Buttons
        put("button.name.spawner", "&#4fc3f7%entity% Spawner");
        put("button.name.decrease-64", "&c-64 Spawners");
        put("button.name.decrease-16", "&c-16 Spawners");
        put("button.name.decrease-1", "&c-1 Spawner");
        put("button.name.increase-64", "&a+64 Spawners");
        put("button.name.increase-16", "&a+16 Spawners");
        put("button.name.increase-1", "&a+1 Spawner");

        put("button.lore.remove", "&7Click to remove %amount% spawner\n&7from the stack\n&7Current stack: &e%stack_size%");
        put("button.lore.add", "&7Click to add %amount% spawner\n&7to the stack\n&7Current stack: &e%stack_size%");
        put("button.lore.spawner", "\n&8▪ &#81d4faEntity: &f%entity%\n&8▪ &#81d4faStack Size: &f%stack_size%\n&8▪ &#81d4faMax Stack Size: &f%max_stack_size%\n\n&#81d4fa➜ &7Click to return main menu");

        // Navigation Buttons
        put("navigation-button.previous.name", "&#81d4faPrevious Page");
        put("navigation-button.previous.lore", "&#81d4faClick to go to page %target_page%");
        put("navigation-button.next.name", "&#81d4faNext Page");
        put("navigation-button.next.lore", "&#81d4faClick to go to page %target_page%");

        // Page Indicator
        put("page-indicator.name", "&#f4d842Page %current_page%/&#f4d842%total_pages%");
        put("page-indicator.lore", "&7Total Items: &a%current_items%&7/&f%max_slots%");

        // Shop Page Indicator
        put("shop-page-indicator.name", "&#ffd700Sell All Items");
        put("shop-page-indicator.lore", "\n&8▪ &#ffd700Total Items: &a%current_items%&7/&f%max_slots%\n&8▪ &#ffd700Storage: &a%percent_storage%&a%&f full\n\n&#ffd700➜ &7Click to sell all items");

        // Other GUI Buttons
        put("return-button.name", "&#ff6b6b Return to Main Menu");
        put("take-all-button.name", "&#00E689Take All Items");
        put("equipment-toggle.name", "&#f4d842Equipment Drops");
        put("equipment-toggle.lore.enabled", "&7Status: &a&lAllowed");
        put("equipment-toggle.lore.disabled", "&7Status: &c&lBlocked");

        // Command Messages
        put("command.usage", "&#00E689SmartSpawner Commands:\n&f/smartspawner reload &7- Reload the plugin configuration\n&f/smartspawner list &7- Open the spawner list (for admin management)\n&f/smartspawner give <player> <mobtype> <amount> &7- Give spawners to a player");
        put("command.reload.usage", "&cUsage: /smartspawner reload");
        put("command.reload.success", "&aPlugin reloaded successfully!");
        put("command.reload.error", "&cError reloading plugin. Check console for details.");

        put("command.give.usage", "&cUsage: /smartspawner give <player> <mobtype> <amount>");
        put("command.give.player-not-found", "&cPlayer not found!");
        put("command.give.invalid-mob-type", "&cInvalid mob type! Use tab completion to see available types.");
        put("command.give.invalid-amount", "&cInvalid amount! Please enter a number between 1 and 64.");
        put("command.give.amount-too-large", "&cMaximum amount allowed is %max%!");
        put("command.give.inventory-full", "&eYour inventory is full! Some items have been dropped on the ground.");
        put("command.give.spawner-received", "&aYou have received %amount% %entity% spawner(s)!");
        put("command.give.spawner-given", "&aYou have given %player% %amount% %entity% spawner(s)!");
        put("command.give.spawner-given-dropped", "&eYou have given %player% %amount% %entity% spawner(s) (some items were dropped on the ground)");

        // No Permission Message
        put("no-permission.message", "&cYou do not have permission to do that!");
        put("no-permission.prefix", true);
        put("no-permission.type", "CHAT");
        put("no-permission.sound", "block.note_block.pling");

        //---------------------------------------------------
        // Spawner List GUI (Command)
        //---------------------------------------------------
        put("spawner-list.gui-title.world-selection", "&#3287A9&lSpawner List");
        put("spawner-list.gui-title.page-title", "{world} &r- [{current}/{total}]");

        // World Buttons
        put("spawner-list.world-buttons.overworld.name", "&a&lOverworld");
        put("spawner-list.world-buttons.overworld.lore", "&8▪ &7Total spawners: &a{total}\n\n&a➜ &7Click to view Overworld spawners");

        put("spawner-list.world-buttons.nether.name", "&c&lNether");
        put("spawner-list.world-buttons.nether.lore", "&8▪ &7Total spawners: &c{total}\n\n&c➜ &7Click to view Nether spawners");

        put("spawner-list.world-buttons.end.name", "&5&lThe End");
        put("spawner-list.world-buttons.end.lore", "&8▪ &7Total spawners: &5{total}\n\n&5➜ &7Click to view The End spawners");

        // Spawner Item
        put("spawner-list.spawner-item.name", "&#4fc3f7&lSpawner #{id}");
        put("spawner-list.spawner-item.id_pattern", "Spawner #([A-Za-z0-9]+)");
        put("spawner-list.spawner-item.lore.separator", "&8&m------------------------");
        put("spawner-list.spawner-item.lore.location", "&8▪ &7Location: &f{x}, {y}, {z}");
        put("spawner-list.spawner-item.lore.entity", "&8▪ &7Entity: &f{entity}");
        put("spawner-list.spawner-item.lore.stack_size", "&8▪ &7Stack Size: &6{size}");
        put("spawner-list.spawner-item.lore.status.active", "&8▪ &7Status: &a&lACTIVE");
        put("spawner-list.spawner-item.lore.status.inactive", "&8▪ &7Status: &c&lINACTIVE");
        put("spawner-list.spawner-item.lore.teleport", "&#3287A9➜ &eClick to teleport");

        // Navigation
        put("spawner-list.navigation.previous-page", "&e&lPrevious Page");
        put("spawner-list.navigation.next-page", "&e&lNext Page");
        put("spawner-list.navigation.back", "&c&lBack to World Selection");

    }};

    public LanguageManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.languageMessages = new HashMap<>();
        this.messageCache = Collections.synchronizedMap(
                new LinkedHashMap<String, String>(CACHE_SIZE + 1, .75F, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                        return size() > CACHE_SIZE;
                    }
                }
        );
        loadLanguages();
    }

    private void loadLanguages() {
        File messagesDir = new File(plugin.getDataFolder(), "messages");
        if (!messagesDir.exists()) {
            messagesDir.mkdirs();
            saveDefaultLanguageFiles();
        }

        messageCache.clear();
        languageMessages.clear();

        File[] languageFiles = messagesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (languageFiles != null) {
            for (File file : languageFiles) {
                String langCode = file.getName().replace(".yml", "");
                FileConfiguration langConfig = YamlConfiguration.loadConfiguration(file);

                boolean hasChanges = mergeDefaultMessages(langConfig);
                if (hasChanges) {
                    try {
                        langConfig.save(file);
                        logger.info("Updated language file: " + file.getName());
                    } catch (IOException e) {
                        logger.severe("Could not save updated language file " + file.getName() + ": " + e.getMessage());
                    }
                }

                languageMessages.put(langCode, langConfig);
            }
        }

        String configLang = plugin.getConfig().getString("settings.language", "en");
        currentLanguage = SupportedLanguage.fromCode(configLang);
        messages = languageMessages.get(currentLanguage.getCode());

        if (messages == null) {
            logger.warning("Language " + currentLanguage.getDisplayName() + " not found, falling back to English");
            currentLanguage = SupportedLanguage.ENGLISH;
            messages = languageMessages.get(SupportedLanguage.ENGLISH.getCode());

            if (messages == null) {
                createDefaultLanguageFile(SupportedLanguage.ENGLISH);
                messages = languageMessages.get(SupportedLanguage.ENGLISH.getCode());
            }
        }
    }

    private void saveDefaultLanguageFiles() {
        for (SupportedLanguage lang : SupportedLanguage.values()) {
            saveResource(lang.getResourcePath());
        }
    }

    private void saveResource(String resourcePath) {
        try {
            if (plugin.getResource(resourcePath) != null) {
                plugin.saveResource(resourcePath, false);
            } else {
                String langCode = resourcePath.substring(resourcePath.lastIndexOf('/') + 1).replace(".yml", "");
                SupportedLanguage lang = SupportedLanguage.fromCode(langCode);
                createDefaultLanguageFile(lang);
            }
        } catch (Exception e) {
            logger.severe("Failed to save resource " + resourcePath + ": " + e.getMessage());
        }
    }

    private void createDefaultLanguageFile(SupportedLanguage language) {
        File langFile = new File(plugin.getDataFolder(), language.getResourcePath());
        FileConfiguration langConfig = new YamlConfiguration();

        for (Map.Entry<String, Object> entry : defaultMessages.entrySet()) {
            langConfig.set(entry.getKey(), entry.getValue());
        }

        try {
            langConfig.save(langFile);
            languageMessages.put(language.getCode(), langConfig);
            logger.info("Created new language file: " + language.getResourcePath());
        } catch (IOException e) {
            logger.severe("Could not create language file " + language.getResourcePath() + ": " + e.getMessage());
        }
    }


    private boolean mergeDefaultMessages(FileConfiguration config) {
        boolean changed = false;
        for (Map.Entry<String, Object> entry : defaultMessages.entrySet()) {
            if (!config.contains(entry.getKey())) {
                config.set(entry.getKey(), entry.getValue());
                changed = true;
                logger.info("Added missing message: " + entry.getKey());
            }
        }
        return changed;
    }
    // get message from path
    public String getMessage(String path) {
        String cachedMessage = messageCache.get(path + "_" + currentLanguage.getCode());
        if (cachedMessage != null) {
            return cachedMessage;
        }

        String message = messages.getString(path);
        if (message == null && currentLanguage != SupportedLanguage.ENGLISH) {
            message = languageMessages.get(SupportedLanguage.ENGLISH.getCode()).getString(path);
        }

        message = message == null ? "Message not found: " + path : colorize(message);
        messageCache.put(path + "_" + currentLanguage.getCode(), message);
        return message;
    }
    // get message from path with replacements
    public String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        StringBuilder cacheKey = new StringBuilder(path);
        for (String replacement : replacements) {
            cacheKey.append(replacement);
        }

        String cachedMessage = messageCache.get(cacheKey.toString());
        if (cachedMessage != null) {
            return cachedMessage;
        }

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }

        messageCache.put(cacheKey.toString(), message);
        return message;
    }
    // get message from path with replacements map
    public String getMessage(String path, Map<String, String> replacements) {
        String message = getMessage(path);

        // Create cache key from path and all replacements
        StringBuilder cacheKey = new StringBuilder(path);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            cacheKey.append(entry.getKey()).append(entry.getValue());
        }

        String cachedMessage = messageCache.get(cacheKey.toString());
        if (cachedMessage != null) {
            return cachedMessage;
        }

        // Apply all replacements
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }

        messageCache.put(cacheKey.toString(), message);
        return message;
    }

    public String getConsoleMessage(String path) {
        String cachedMessage = messageCache.get(path + "_" + currentLanguage.getCode());
        if (cachedMessage != null) {
            return applyColorToConsole(cachedMessage);  // Chuyển đổi màu nếu đã có trong cache
        }

        String message = messages.getString(path);
        if (message == null && currentLanguage != SupportedLanguage.ENGLISH) {
            message = languageMessages.get(SupportedLanguage.ENGLISH.getCode()).getString(path);
        }

        message = message == null ? "Message not found: " + path : applyColorToConsole(message);
        messageCache.put(path + "_" + currentLanguage.getCode(), message);
        return message;
    }

    // get console message from path with replacements
    public String getConsoleMessage(String path, String... replacements) {
        String message = getConsoleMessage(path);
        StringBuilder cacheKey = new StringBuilder(path);
        for (String replacement : replacements) {
            cacheKey.append(replacement);
        }

        String cachedMessage = messageCache.get(cacheKey.toString());
        if (cachedMessage != null) {
            return applyColorToConsole(cachedMessage);
        }

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }

        messageCache.put(cacheKey.toString(), message);
        return message;
    }

    // get console message from path with replacements map
    public String getConsoleMessage(String path, Map<String, String> replacements) {
        String message = getConsoleMessage(path);

        // Create cache key from path and all replacements
        StringBuilder cacheKey = new StringBuilder(path);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            cacheKey.append(entry.getKey()).append(entry.getValue());
        }

        String cachedMessage = messageCache.get(cacheKey.toString());
        if (cachedMessage != null) {
            return applyColorToConsole(cachedMessage);  // Chuyển đổi màu nếu đã có trong cache
        }

        // Apply all replacements
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }

        messageCache.put(cacheKey.toString(), message);
        return message;
    }

    private String applyColorToConsole(String message) {
        // Remove hex color codes (e.g., &#FFFFFF)
        // This regular expression removes all hex color codes like &#RRGGBB
        message = message.replaceAll("&#[0-9a-fA-F]{6}", "");

        // Replace Minecraft color codes (e.g., &a, &c, etc.) with ANSI color codes for console
        message = message.replaceAll("(?i)&0", "\u001B[30m"); // &0 - black
        message = message.replaceAll("(?i)&1", "\u001B[34m"); // &1 - blue
        message = message.replaceAll("(?i)&2", "\u001B[32m"); // &2 - green
        message = message.replaceAll("(?i)&3", "\u001B[36m"); // &3 - cyan
        message = message.replaceAll("(?i)&4", "\u001B[31m"); // &4 - red
        message = message.replaceAll("(?i)&5", "\u001B[35m"); // &5 - purple
        message = message.replaceAll("(?i)&6", "\u001B[33m"); // &6 - yellow
        message = message.replaceAll("(?i)&7", "\u001B[37m"); // &7 - white
        message = message.replaceAll("(?i)&8", "\u001B[90m"); // &8 - gray
        message = message.replaceAll("(?i)&9", "\u001B[94m"); // &9 - light blue
        message = message.replaceAll("(?i)&a", "\u001B[32m"); // &a - light green
        message = message.replaceAll("(?i)&b", "\u001B[96m"); // &b - light cyan
        message = message.replaceAll("(?i)&c", "\u001B[91m"); // &c - light red
        message = message.replaceAll("(?i)&d", "\u001B[95m"); // &d - pink
        message = message.replaceAll("(?i)&e", "\u001B[93m"); // &e - light yellow
        message = message.replaceAll("(?i)&f", "\u001B[97m"); // &f - bright white

        // Replace Minecraft text effects (e.g., &k, &l, &n, &o, &r) with corresponding ANSI codes
        message = message.replaceAll("(?i)&k", "\u001B[8m");  // &k - obfuscated (text scramble)
        message = message.replaceAll("(?i)&l", "\u001B[1m");  // &l - bold
        message = message.replaceAll("(?i)&m", "\u001B[9m");  // &m - strikethrough
        message = message.replaceAll("(?i)&n", "\u001B[4m");  // &n - underline
        message = message.replaceAll("(?i)&o", "\u001B[3m");  // &o - italic
        message = message.replaceAll("(?i)&r", "\u001B[0m");  // &r - reset all effects (reset to default)

        // Append reset color to ensure the console defaults back to normal after this message
        return message + "\u001B[0m";
    }


    public String getMessageWithPrefix(String path) {
        String cacheKey = "prefix_" + path + "_" + currentLanguage;
        String cachedMessage = messageCache.get(cacheKey);
        if (cachedMessage != null) {
            return cachedMessage;
        }

        String prefix = colorize(messages.getString("prefix", ""));
        String message = getMessage(path);
        String result = prefix.isEmpty() ? message : prefix + " " + message;
        messageCache.put(cacheKey, result);
        return result;
    }

    public String getMessageWithPrefix(String path, String... replacements) {
        StringBuilder cacheKey = new StringBuilder("prefix_").append(path);
        for (String replacement : replacements) {
            cacheKey.append(replacement);
        }

        String cachedMessage = messageCache.get(cacheKey.toString());
        if (cachedMessage != null) {
            return cachedMessage;
        }

        String prefix = colorize(messages.getString("prefix", ""));
        String message = getMessage(path, replacements);
        String result = prefix.isEmpty() ? message : prefix + " " + message;

        messageCache.put(cacheKey.toString(), result);
        return result;
    }

    public String getMessageWithPrefix(String path, Map<String, String> replacements) {
        // Create cache key from prefix, path and all replacements
        StringBuilder cacheKey = new StringBuilder("prefix_").append(path);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            cacheKey.append(entry.getKey()).append(entry.getValue());
        }

        String cachedMessage = messageCache.get(cacheKey.toString());
        if (cachedMessage != null) {
            return cachedMessage;
        }

        String prefix = colorize(messages.getString("prefix", ""));
        String message = getMessage(path, replacements);
        String result = prefix.isEmpty() ? message : prefix + " " + message;

        messageCache.put(cacheKey.toString(), result);
        return result;
    }

    public String colorize(String message) {
        if (message == null) return "";

        // Process Hex color (&#RRGGBB)
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);

        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + group).toString());
        }
        matcher.appendTail(buffer);

        // Process Minecraft color codes
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public String getLocalizedMobName(EntityType type) {
        String cacheKey = "mob_" + type.name() + "_" + currentLanguage;
        String cachedName = messageCache.get(cacheKey);
        if (cachedName != null) {
            return cachedName;
        }

        String path = "mob_names." + type.name();
        String localizedName = messages.getString(path);

        if (localizedName == null && !currentLanguage.equals("en")) {
            localizedName = languageMessages.get("en").getString(path);
        }

        localizedName = localizedName != null ? localizedName : type.name();
        messageCache.put(cacheKey, localizedName);
        return localizedName;
    }

    public String getFormattedMobName(EntityType type) {
        return colorize(getLocalizedMobName(type));
    }

    public SupportedLanguage getCurrentLanguage() {
        return currentLanguage;
    }

    public List<SupportedLanguage> getSupportedLanguages() {
        return Arrays.asList(SupportedLanguage.values());
    }

    public void reload() {
        loadLanguages();
    }

    private MessageType getMessageType(String path) {
        String typePath = path + ".type";
        String typeStr = messages.getString(typePath, "CHAT");
        try {
            return MessageType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid message type specified for path: " + path + ". Defaulting to CHAT");
            return MessageType.CHAT;
        }
    }

    public void sendMessage(Player player, String path) {
        if (path == null) return;

        String messagePath = path + ".message";
        String message;

        // Check if this message should use prefix
        boolean usePrefix = messages.getBoolean(path + ".prefix", false);
        if (usePrefix) {
            message = getMessageWithPrefix(messagePath);
        } else {
            message = getMessage(messagePath);
        }

        if (message == null || message.isEmpty()) return;

        MessageType type = getMessageType(path);
        String soundPath = path + ".sound";
        String sound = messages.getString(soundPath);

        switch (type) {
            case CHAT:
                player.sendMessage(message);
                break;
            case ACTION_BAR:
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
                break;
            case BOTH:
                player.sendMessage(message);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
                break;
        }

        if (sound != null && !sound.isEmpty()) {
            try {
                Sound soundEnum = Registry.SOUNDS.get(NamespacedKey.minecraft(sound));
                if (soundEnum != null) {
                    player.playSound(player.getLocation(), soundEnum, 1.0f, 1.0f);
                } else {
                    logger.warning("Invalid sound specified for message path: " + path);
                }
            } catch (IllegalArgumentException e) {
                logger.warning("Error with sound for message path: " + path);
                logger.warning(e.getMessage());
            }
        }
    }

    public void sendMessage(Player player, String path, String... replacements) {
        if (path == null) return;
        String messagePath = path + ".message";
        String message = "";

        // Check if this message should use prefix
        boolean usePrefix = messages.getBoolean(path + ".prefix", false);
        if (usePrefix) {
            message = getMessageWithPrefix(messagePath, replacements);
        } else {
            message = getMessage(messagePath, replacements);
        }

        if (message == null || message.isEmpty()) return;

        MessageType type = getMessageType(path);
        String soundPath = path + ".sound";
        String sound = messages.getString(soundPath);

        switch (type) {
            case CHAT:
                player.sendMessage(message);
                break;
            case ACTION_BAR:
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
                break;
            case BOTH:
                player.sendMessage(message);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
                break;
        }

        if (sound != null && !sound.isEmpty()) {
            try {
                Sound soundEnum = Registry.SOUNDS.get(NamespacedKey.minecraft(sound));
                if (soundEnum != null) {
                    player.playSound(player.getLocation(), soundEnum, 1.0f, 1.0f);
                } else {
                    logger.warning("Invalid sound specified for message path: " + path);
                }
            } catch (IllegalArgumentException e) {
                logger.warning("Error with sound for message path: " + path);
                logger.warning(e.getMessage());
            }
        }
    }

    // For gui titles
    public String getGuiTitle(String path) {
        return messages.getString(path);
    }

    public String getGuiTitle(String path, String... replacements) {
        return getMessage(path, replacements);
    }

    public String formatNumber(long number) {
        String formatNumberPath = "format-number.";
        if (number >= 1_000_000_000_000L) {
            return String.format(getMessage(formatNumberPath + "trillion"), formatNumberWithDecimals(number / 1_000_000_000_000D));
        } else if (number >= 1_000_000_000) {
            return String.format(getMessage(formatNumberPath + "billion"), formatNumberWithDecimals(number / 1_000_000_000D));
        } else if (number >= 1_000_000) {
            return String.format(getMessage(formatNumberPath + "million"), formatNumberWithDecimals(number / 1_000_000D));
        } else if (number >= 1_000) {
            return String.format(getMessage(formatNumberPath + "thousand"), formatNumberWithDecimals(number / 1_000D));
        } else {
            return String.format(getMessage(formatNumberPath + "default"), number);
        }
    }

    private String formatNumberWithDecimals(double number) {
        if (number < 10) {
            return String.format("%.1f", number);
        } else {
            return String.format("%.0f", number);
        }
    }
}