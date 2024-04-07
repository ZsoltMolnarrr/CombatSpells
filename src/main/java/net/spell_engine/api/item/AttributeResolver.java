package net.spell_engine.api.item;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.projectile_damage.api.EntityAttributes_ProjectileDamage;

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
        if (FabricLoader.getInstance().isModLoaded("projectile_damage")) {
            register(
                    EntityAttributes_ProjectileDamage.attributeId,
                    EntityAttributes_ProjectileDamage.GENERIC_PROJECTILE_DAMAGE
            );
        }

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
