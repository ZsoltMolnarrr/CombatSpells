package net.spell_engine.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import net.spell_engine.internals.target.EntityRelation;

import java.util.LinkedHashMap;

@Config(name = "server")
public class ServerConfig implements ConfigData { public ServerConfig() {}
    @Comment("Applied as multiplier on top of spell.cast.movement_speed. Default value of 1.0 means no change.")
    public float movement_multiplier_speed_while_casting = 1F;
    @Comment("Allow spells to bypass invulnerability frames. This is required in order for high attack frequency spells (such as beams) to work.")
    public boolean bypass_iframes = true;
    @Comment("Spell haste reduces the cooldown time of abilities")
    public boolean haste_affects_cooldown = true;
    @Comment("Spell costs exhausts (hunger) will be multiplied with this value. Set `0` for no exhaust.")
    public float spell_cost_exhaust_multiplier = 1F;
    @Comment("Spells should levelCost items. Set `false` to remove rune (or other item) levelCost from all spells.")
    public boolean spell_cost_item_allowed = true;
    @Comment("Spells should damage items on use. Set `false` to disable.")
    public boolean spell_cost_durability_allowed = true;
    @Comment("The time in ticks of global cooldown to apply to all instant cast spells when casted.")
    public int spell_instant_cast_gcd = 0;
    @Comment("Players cannot unequip a spell book, if one of the spells in it is on cooldown.")
    public boolean spell_item_cooldown_lock = true;
    @Comment("Players can use the Spell Binding Table to create spell books.")
    public boolean spell_book_creation_enabled = true;
    @Comment("Spell book creation level levelCost")
    public int spell_book_creation_cost = 1;
    @Comment("Spell book creation level requirement")
    public int spell_book_creation_requirement = 1;
    @Comment("Spell binding level levelCost multiplier")
    public int spell_binding_level_cost_multiplier = 1;
    @Comment("Spell binding lapis lazuli levelCost multiplier")
    public int spell_binding_lapis_cost_multiplier = 1;
    @Comment("Should spells on the offhand item be collected and used.")
    public boolean spell_container_from_offhand = false;
    @Comment("Should be used in compatibility mode, when Trinkets is not installed.")
    public boolean spell_container_from_offhand_ignore_dual_wielding = false;
    @Comment("Should spells on the offhand item be collected and used.")
    public boolean spell_container_from_equipment = true;
    @Comment("If set true, a Fireball doesn't collide with an ally, a healing projectile doesn't collide with an enemy")
    public boolean projectiles_pass_thru_irrelevant_targets = true;
    @Comment("Auto swap Bow & Spear cooldown ticks to apply for attack and itemUse")
    public int auto_swap_cooldown = 5;
    @Comment("Apply `Spell Casting from Spell Book` capability to Swords, Tridents, Maces")
    public boolean add_spell_casting_to_melee_weapons = true;
    @Comment("Apply `Spell Casting from Spell Book` capability to Bows, Crossbows")
    public boolean add_spell_casting_to_ranged_weapons = true;
    @Comment("Apply `Spell Casting from Spell Book` capability to any item matching this regex. (Not applied of empty)")
    public String add_spell_casting_regex = "";
    @Comment("Do not apply `Spell Casting from Spell Book` capability to any item matching this regex. (Not applied of empty)")
    public String blacklist_spell_casting_regex = "";

    @Comment("""
            Relations determine which cases the effect of a player casted spell can effect a target.
            +----------------+-------+----------+----------+----------+--------+
            |                | ALLY  | FRIENDLY | NEUTRAL  | HOSTILE  | MIXED  |
            +----------------+-------+----------+----------+----------+--------+
            | DIRECT DAMAGE  | 🚫    | ✅       | ✅       | ✅       | ✅    |
            | AREA DAMAGE    | 🚫    | 🚫       | 🚫       | ✅       | ✅    |
            | DIRECT HEALING | ✅    | ✅       | ✅       | 🚫       | ✅    |
            | AREA HEALING   | ✅    | ✅       | 🚫       | 🚫       | ✅    |
            +----------------+-------+----------+----------+----------+--------+
            
            The various relation related configs are being checked in the following order:
            - `player_relations`
            - `player_relation_to_passives`
            - `player_relation_to_hostiles`
            - `player_relation_to_other`
            (The first relation to be found for the target will be applied.)
            """)
    public LinkedHashMap<String, EntityRelation> player_relations = new LinkedHashMap<>() {{
        put("minecraft:player", EntityRelation.FRIENDLY);
        put("minecraft:villager", EntityRelation.FRIENDLY);
        put("minecraft:allay", EntityRelation.FRIENDLY);
        put("minecraft:iron_golem", EntityRelation.FRIENDLY);
        put("guardvillagers:guard", EntityRelation.FRIENDLY);
        put("minecraft:cat", EntityRelation.FRIENDLY);
    }};

    @Comment("Relation to unspecified entities those are instance of PassiveEntity(Yarn)")
    public EntityRelation player_relation_to_passives = EntityRelation.HOSTILE;
    @Comment("Relation to unspecified entities those are instance of HostileEntity(Yarn)")
    public EntityRelation player_relation_to_hostiles = EntityRelation.HOSTILE;
    @Comment("Fallback relation")
    public EntityRelation player_relation_to_other = EntityRelation.HOSTILE;
}
