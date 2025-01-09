package net.spell_engine.api.spell;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.world.World;

public class SpellRegistry_V2 {
    /**
     * Using vanilla name space on purpose!
     * So spell data file path looks like this:
     * `data/MOD/spell/SPELL.json`
     * instead of this:
     * `data/MOD/spell_engine/spell/SPELL.json`
     */
    public static final Identifier ID = Identifier.ofVanilla("spells");
    public static final RegistryKey<Registry<Spell>> KEY = RegistryKey.ofRegistry(ID);
    public static Registry<Spell> from(World world) {
        return world.getRegistryManager().get(KEY);
    }


    private static final Gson gson = new GsonBuilder().create();
    private static SpellEncoder encoder = new SpellEncoder();
    static class SpellEncoder implements Encoder<Spell> {
        @Override
        public <T> DataResult<T> encode(Spell input, DynamicOps<T> ops, T prefix) {
            return DataResult.success(gson.toJsonTree(input));
        }
    }


//    public static final Codec<Spell> CODEC = Codecs.exceptionCatching(Codecs.JSON_ELEMENT.xmap(
//            json -> {
//                System.out.println("Spell Engine: Decoding spell from json: " + json);
//                return gson.fromJson(json, Spell.class);
//            },
//            spell -> {
//                JsonElement jsonElement = gson.toJsonTree(spell);
//                System.out.println("Spell Engine: Encoding spell to json: " + jsonElement);
//                return jsonElement;
//            }
//    ));

    public static final Codec<Spell> CODEC = Codecs.exceptionCatching(Codec.of(
            json -> {
                System.out.println("Spell Engine: Decoding spell from json: " + json);
                return gson.fromJson(json, Spell.class);
            },
            spell -> {
                JsonElement jsonElement = gson.toJsonTree(spell);
                System.out.println("Spell Engine: Encoding spell to json: " + jsonElement);
                return jsonElement;
            }
    ));
}
