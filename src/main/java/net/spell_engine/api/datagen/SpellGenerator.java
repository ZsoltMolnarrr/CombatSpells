package net.spell_engine.api.datagen;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.spell_engine.api.spell.Spell;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class SpellGenerator implements DataProvider {
    private final CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup;
    protected final FabricDataOutput dataOutput;

    public SpellGenerator(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        this.dataOutput = dataOutput;
        this.registryLookup = registryLookup;
    }

    public record Entry(Identifier id, Spell spell) { }
    public static class Builder {
        private final List<Entry> entries = new ArrayList<>();
        public void add(Identifier id, Spell spell) {
            entries.add(new Entry(id, spell));
        }
    }

    public abstract void generateSpells(Builder builder);

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        var builder = new Builder();
        generateSpells(builder);
        var entries = builder.entries;

        List<CompletableFuture> writes = new ArrayList<>();
        for (var entry: entries) {
            var spell = entry.spell;
            var spellId = entry.id;
            var json = gson.toJsonTree(spell);
            writes.add(writeOriginalFormat(writer, json, getFilePath(spellId)));
        }

        return CompletableFuture.allOf(writes.toArray(new CompletableFuture[0]));
    }

    private static CompletableFuture<?> writeOriginalFormat(DataWriter writer, JsonElement json, Path path) {
        return CompletableFuture.runAsync(() -> {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                HashingOutputStream hashingOutputStream = new HashingOutputStream(Hashing.sha1(), byteArrayOutputStream);
                JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(hashingOutputStream, StandardCharsets.UTF_8));

                try {
                    // Write the given json element to the json writer
                    jsonWriter.setSerializeNulls(false);
                    jsonWriter.setIndent("  ");
                    Streams.write(json, jsonWriter);
                } catch (Throwable var9) {
                    try {
                        jsonWriter.close();
                    } catch (Throwable var8) {
                        var9.addSuppressed(var8);
                    }

                    throw var9;
                }

                jsonWriter.close();
                writer.write(path, byteArrayOutputStream.toByteArray(), hashingOutputStream.hash());
            } catch (IOException var10) {
                IOException iOException = var10;
                LOGGER.error("Failed to save file to {}", path, iOException);
            }

        }, Util.getMainWorkerExecutor());
    }

    @Override
    public String getName() {
        return "Spell Generator";
    }

    private Path getFilePath(Identifier spellId) {
        return this.dataOutput.getResolver(DataOutput.OutputType.DATA_PACK, "spell").resolveJson(spellId);
    }
}
