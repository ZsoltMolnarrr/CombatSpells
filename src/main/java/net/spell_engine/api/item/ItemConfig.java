package net.spell_engine.api.item;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemConfig { public ItemConfig() { }
    public static class Attribute { public Attribute() {}
        public String id;
        public float value;
        public EntityAttributeModifier.Operation operation;

        public Attribute(String id, float value, EntityAttributeModifier.Operation operation) {
            this.id = id;
            this.value = value;
            this.operation = operation;
        }
        public static Attribute bonus(Identifier attributeId, float value) {
            return new Attribute(
                    attributeId.toString(),
                    value,
                    EntityAttributeModifier.Operation.ADD_VALUE
            );
        }

        public static Attribute multiply(Identifier attributeId, float value) {
            return new Attribute(
                    attributeId.toString(),
                    value,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
        }

        public static ArrayList<Attribute> bonuses(List<Identifier> attributeIds, float value) {
            ArrayList<Attribute> spellAttributes = new ArrayList<>();
            for (var attributeId: attributeIds) {
                spellAttributes.add(new Attribute(
                        attributeId.toString(),
                        value,
                        EntityAttributeModifier.Operation.ADD_VALUE
                    )
                );
            }
            return spellAttributes;
        }
    }

    public Map<String, Weapon> weapons = new HashMap<>();
    public static class Weapon {
        public float attack_damage = 0;
        public float attack_speed = 0;
        public ArrayList<Attribute> attributes = new ArrayList<>();

        public Weapon() { }
        public Weapon(float attack_damage, float attack_speed) {
            this.attack_damage = attack_damage;
            this.attack_speed = attack_speed;
        }
        public Weapon add(Attribute attribute) {
            attributes.add(attribute);
            return this;
        }
    }

    public Map<String, ArmorSet> armor_sets = new HashMap<>();
    public static class ArmorSet { public ArmorSet() { }
        public float armor_toughness = 0;
        public float knockback_resistance = 0;
        public Piece head = new Piece();
        public Piece chest = new Piece();
        public Piece legs = new Piece();
        public Piece feet = new Piece();
        public static class Piece { public Piece() { }
            public int armor = 0;
            public ArrayList<Attribute> attributes = new ArrayList<>();

            public Piece(int armor) {
                this.armor = armor;
            }

            public Piece add(Attribute attribute) {
                attributes.add(attribute);
                return this;
            }
            public Piece addAll(List<Attribute> attributes) {
                this.attributes.addAll(attributes);
                return this;
            }
        }

        public static ArmorSet with(Piece head, Piece chest, Piece legs, Piece feet) {
            var set = new ArmorSet();
            set.head = head;
            set.chest = chest;
            set.legs = legs;
            set.feet = feet;
            return set;
        }
    }
}