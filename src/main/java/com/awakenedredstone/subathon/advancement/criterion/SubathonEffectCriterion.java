package com.awakenedredstone.subathon.advancement.criterion;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.core.effect.Effect;
import com.awakenedredstone.subathon.registry.EffectRegistry;
import com.awakenedredstone.subathon.registry.SubathonRegistries;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.advancement.criterion.ItemDurabilityChangedCriterion;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.AdvancementEntityPredicateSerializer;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class SubathonEffectCriterion /*extends AbstractCriterion<SubathonEffectCriterion.Conditions>*/ {
    /*static final Identifier ID = Subathon.id("effect_and_points");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public Conditions conditionsFromJson(JsonObject jsonObject, EntityPredicate.Extended extended, AdvancementEntityPredicateDeserializer advancementEntityPredicateDeserializer) {
        Identifier identifier = new Identifier(JsonHelper.getString(jsonObject, "effect"));
        Effect effect = SubathonRegistries.EFFECTS.get(identifier);
        return new Conditions(extended, JsonHelper.getLong(jsonObject, "targetAmount"), effect);
    }

    public void trigger(ServerPlayerEntity player, long points) {
        this.trigger(player, (conditions) -> conditions.matches(points));
    }

    public static Conditions create(long points, Effect effect) {
        return new Conditions(EntityPredicate.Extended.EMPTY, points, effect);
    }

    public static class Conditions
        extends AbstractCriterionConditions {
        private final long targetAmount;
        private final Effect effect;

        public Conditions(EntityPredicate.Extended player, long targetAmount, Effect effect) {
            super(ID, player);
            this.targetAmount = targetAmount;
            this.effect = effect;
        }

        public static ItemDurabilityChangedCriterion.Conditions create(ItemPredicate item, NumberRange.IntRange durability) {
            return ItemDurabilityChangedCriterion.Conditions.create(EntityPredicate.Extended.EMPTY, item, durability);
        }

        public static ItemDurabilityChangedCriterion.Conditions create(EntityPredicate.Extended player, ItemPredicate item, NumberRange.IntRange durability) {
            return new ItemDurabilityChangedCriterion.Conditions(player, item, durability, NumberRange.IntRange.ANY);
        }

        public boolean matches(long points) {
            return effect.enabled && points >= targetAmount;
        }

        @Override
        public JsonObject toJson(AdvancementEntityPredicateSerializer predicateSerializer) {
            JsonObject jsonObject = super.toJson(predicateSerializer);
            jsonObject.add("targetAmount", new JsonPrimitive(this.targetAmount));
            jsonObject.add("effect", new JsonPrimitive(this.effect.getIdentifier().toString()));
            return jsonObject;
        }
    }*/
}