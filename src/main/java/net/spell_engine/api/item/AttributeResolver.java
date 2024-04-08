package net.spell_engine.api.item;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;

public class AttributeResolver {
    private static final HashMap<Identifier, EntityAttribute> attributes = new HashMap<>();

    static {
        setup();
    }

    @Deprecated
    /**
     * Called upon initialization of this mod.
     */
    private static void setup() {
        // Mixin here to add custom attributes, don't call `register` outside of this function
        // @Inject(method = "setup", at = @At("TAIL"))
    }

    private static void register(Identifier id, EntityAttribute attribute) {
        attributes.put(id, attribute);
    }

    public static EntityAttribute get(Identifier id) {
        // Check for custom attribute
        var attribute = attributes.get(id);
        if (attribute == null) {
            attribute = Registries.ATTRIBUTE.get(id);
        }
        return attribute;
    }
}
