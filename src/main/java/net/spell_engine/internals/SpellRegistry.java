package net.spell_engine.internals;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.SpellContainer;
import net.spell_engine.api.spell.SpellPool;
import net.spell_engine.utils.WeaponCompatibility;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;

import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class SpellRegistry {
    public static class SpellEntry { public SpellEntry() { }
        public Spell spell;
        public int rawId;
        public SpellEntry(Spell spell, int rawId) {
            this.spell = spell;
            this.rawId = rawId;
        }
    }
    // Registry<Spell>
    private static final Map<Identifier, SpellEntry> spells = new HashMap<>();
    // Registry<Spell> Tags
    public static final Map<Identifier, SpellPool> pools = new HashMap<>();
    // Simply move it into SpellBooks.java
    public static final Map<Identifier, SpellContainer> book_containers = new HashMap<>();
    // Could be turned into a separate registry
    // BUT! Vanilla registries cannot be inserted programatically
    // (So SpellBook container assignment, and fallback/auto assignments would not be possible)
    // Resolution:
    // - SpellBook containers need no assignment, applying item component is suitable, or datafile can be added by devs
    // - Fallback/auto assignments ??? - MAYBE Inject(TAIL) RegistryLoader.loadFromResource (probably wont be synced to clients)
    public static final Map<Identifier, SpellContainer> containers = new HashMap<>();
    // Just remove this, we no longer need it
    private static final Map<SpellSchool, Integer> spellCount = new HashMap<>();

    public static Map<Identifier, SpellEntry> all() {
        return spells;
    }

    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(SpellRegistry::load);
    }

    private static void load(MinecraftServer minecraftServer) {
        loadSpells(minecraftServer.getResourceManager());
        loadPools(minecraftServer.getResourceManager());
        loadContainers(minecraftServer.getResourceManager());
        WeaponCompatibility.initialize();
        encodeContent();
    }

    public static void loadSpells(ResourceManager resourceManager) {
        var gson = new Gson();
        Map<Identifier, SpellEntry> parsed = new HashMap<>();
        // Reading all attribute files
        int rawId = 1;
        var directory = "spells";
        for (var entry : resourceManager.findResources(directory, fileName -> fileName.getPath().endsWith(".json")).entrySet()) {
            var identifier = entry.getKey();
            var resource = entry.getValue();
            try {
                // System.out.println("Checking resource: " + identifier);
                JsonReader reader = new JsonReader(new InputStreamReader(resource.getInputStream()));
                Spell container = gson.fromJson(reader, Spell.class);
                var id = identifier
                        .toString().replace(directory + "/", "");
                id = id.substring(0, id.lastIndexOf('.'));
                Validator.validate(container);
                parsed.put(Identifier.of(id), new SpellEntry(container, rawId++));
                // System.out.println("loaded spell - id: " + id +  " spell: " + gson.toJson(container));
            } catch (Exception e) {
                System.err.println("Spell Engine: Failed to parse spell: " + identifier + " | Reason: " + e.getMessage());
            }
        }
        spells.clear();
        spells.putAll(parsed);
        spellsUpdated();
    }

    public static void loadPools(ResourceManager resourceManager) {
        var gson = new Gson();
        Map<Identifier, SpellPool.DataFormat> parsed = new HashMap<>();
        // Reading all attribute files
        var directory = "spell_pools";
        for (var entry : resourceManager.findResources(directory, fileName -> fileName.getPath().endsWith(".json")).entrySet()) {
            var identifier = entry.getKey();
            var resource = entry.getValue();
            try {
                // System.out.println("Checking resource: " + identifier);
                JsonReader reader = new JsonReader(new InputStreamReader(resource.getInputStream()));
                SpellPool.DataFormat pool = gson.fromJson(reader, SpellPool.DataFormat.class);
                var id = identifier
                        .toString().replace(directory + "/", "");
                id = id.substring(0, id.lastIndexOf('.'));
                parsed.put(Identifier.of(id), pool);
                // System.out.println("loaded pool - " + id +  " ids: " + pool.spell_ids);
            } catch (Exception e) {
                System.err.println("Spell Engine: Failed to parse spell pool: " + identifier + " | Reason: " + e.getMessage());
            }
        }
        Map<Identifier, Spell> spellFlat = spells.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().spell));
        pools.clear();
        for (var entry: parsed.entrySet()) {
            pools.put(entry.getKey(), SpellPool.fromData(entry.getValue(), spellFlat));
        }
    }

    public static void loadContainers(ResourceManager resourceManager) {
        var gson = new Gson();
        Map<Identifier, SpellContainer> parsed = new HashMap<>();
        // Reading all attribute files
        var directory = "spell_assignments";
        for (var entry : resourceManager.findResources(directory, fileName -> fileName.getPath().endsWith(".json")).entrySet()) {
            var identifier = entry.getKey();
            var resource = entry.getValue();
            try {
                // System.out.println("Checking resource: " + identifier);
                JsonReader reader = new JsonReader(new InputStreamReader(resource.getInputStream()));
                SpellContainer container = gson.fromJson(reader, SpellContainer.class);
                var id = identifier
                        .toString().replace(directory + "/", "");
                id = id.substring(0, id.lastIndexOf('.'));
                parsed.put(Identifier.of(id), container);
                // System.out.println("loaded assignment - id: " + id +  " assignment: " + contaisner);
            } catch (Exception e) {
                System.err.println("Spell Engine: Failed to parse spell_assignment: " + identifier + " | Reason: " + e.getMessage());
            }
        }
        containers.clear();
        containers.putAll(parsed);
        containers.putAll(book_containers);
    }

    private static void spellsUpdated() {
        updateReverseMaps();
        spellCount.clear();
        for(var school: SpellSchools.all()) {
            spellCount.put(school, 0);
        }
        for(var spell: spells.entrySet()) {
            var school = spell.getValue().spell.school;
            var current = spellCount.get(school);
            spellCount.put(school, current + 1);
        }
    }

    public static int numberOfSpells(SpellSchool school) {
        return spellCount.get(school);
    }

    public static SpellContainer containerForItem(Identifier itemId) {
        if (itemId == null) {
            return null;
        }
        return containers.get(itemId);
    }

    public static Spell getSpell(Identifier spellId) {
        var entry = spells.get(spellId);
        if (entry != null) {
            return entry.spell;
        }
        return null;
    }

    public static SpellPool spellPool(Identifier id) {
        var pool = pools.get(id);
        return pool != null ? pool : SpellPool.empty;
    }

    public static List<String> encoded = List.of();

    public static class SyncFormat { public SyncFormat() { }
        public Map<String, SpellEntry> spells = new HashMap<>();
        public Map<String, SpellPool.SyncFormat> pools = new HashMap<>();
        public Map<String, SpellContainer> containers = new HashMap<>();
    }

    private static void encodeContent() {
        var gson = new Gson();
        var buffer = PacketByteBufs.create();

        var sync = new SyncFormat();
        spells.forEach((key, value) -> {
            sync.spells.put(key.toString(), value);
        });
        pools.forEach((key, value) -> {
            sync.pools.put(key.toString(), value.toSync());
        });
        containers.forEach((key, value) -> {
            sync.containers.put(key.toString(), value);
        });
        var json = gson.toJson(sync);

        List<String> chunks = new ArrayList<>();
        var chunkSize = 10000;
        for (int i = 0; i < json.length(); i += chunkSize) {
            chunks.add(json.substring(i, Math.min(json.length(), i + chunkSize)));
        }

        System.out.println("Encoded SpellRegistry size (with package overhead): " + "???"
                + " bytes (in " + chunks.size() + " string chunks with the size of "  + chunkSize + ")");

        encoded = chunks;
    }

    public static void decodeContent(List<String> chunks) {
        String json = "";
        for (var chunk: chunks) {
            json = json.concat(chunk);
        }
        var gson = new Gson();
        SyncFormat sync = gson.fromJson(json, SyncFormat.class);
        spells.clear();
        sync.spells.forEach((key, value) -> {
            spells.put(Identifier.of(key), value);
        });
        sync.pools.forEach((key, value) -> {
            pools.put(Identifier.of(key), SpellPool.fromSync(value));
        });
        sync.containers.forEach((key, value) -> {
            containers.put(Identifier.of(key), value);
        });
        spellsUpdated();
    }

    private record ReverseEntry(Identifier identifier, Spell spell) { }
    private static final Map<Integer, ReverseEntry> reverseSpells = new HashMap<>();

    private static void updateReverseMaps() {
        reverseSpells.clear();
        for (var entry: spells.entrySet()) {
            var id = entry.getKey();
            var spell = entry.getValue().spell;
            var rawId = entry.getValue().rawId;
            reverseSpells.put(rawId, new ReverseEntry(id, spell));
        }
    }

    public static int rawSpellId(Identifier identifier) {
        return spells.get(identifier).rawId;
    }

    public static Optional<Identifier> fromRawSpellId(int rawId) {
        var reverseEntry = reverseSpells.get(rawId);
        if (reverseEntry != null) {
            return Optional.of(reverseEntry.identifier);
        }
        return Optional.empty();
    }
}
