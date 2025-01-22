package net.spell_engine.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.spell_engine.api.effect.EntityActionsAllowed;
import net.spell_engine.client.SpellEngineClient;
import net.spell_engine.internals.casting.SpellCast;

import java.util.Locale;

public class HudMessages {
    public static final HudMessages INSTANCE = new HudMessages();
    public static final int DEFAULT_ERROR_MESSAGE_DURATION = 20;
    public static final int DEFAULT_ERROR_MESSAGE_FADEOUT = 10;

    private ErrorMessageState currentError;

    private static final String castAttemptPrefix = "hud.cast_attempt_error.";
    private boolean attemptDisplayed = false;
    public void castAttemptError(SpellCast.Attempt attempt) {
        if (attemptDisplayed) {
            return;
        }
        if (attempt.isSuccess() || attempt.isFail()) {
            attemptDisplayed = true;
        }
        if (!attempt.isFail() || !SpellEngineClient.config.showSpellCastErrors) { return; }
        var translationKey = castAttemptPrefix + attempt.result().toString().toLowerCase(Locale.ENGLISH);
        MutableText message = null;
        switch (attempt.result()) {
            case MISSING_ITEM -> {
                var item = attempt.missingItem().item();
                if (item != null) {
                    var itemName = item.getTranslationKey();
                    message = Text.translatable(translationKey, itemName);
                }
            }
            case ON_COOLDOWN -> {
                message = Text.translatable(translationKey);
            }
        }
        if (message != null) {
            error(message.formatted(Formatting.RED));
        }
    }

    public void actionImpaired(EntityActionsAllowed.SemanticType reason) {
        error(I18n.translate("hud.action_impaired." + reason.toString().toLowerCase(Locale.ENGLISH)));
    }

    public void error(String message) {
        error(message, DEFAULT_ERROR_MESSAGE_DURATION, DEFAULT_ERROR_MESSAGE_FADEOUT);
    }

    public void error(String message, int duration, int fadeOut) {
        currentError = new ErrorMessageState(message, duration, fadeOut);
    }

    public void error(Text text) {
        error(text, DEFAULT_ERROR_MESSAGE_DURATION, DEFAULT_ERROR_MESSAGE_FADEOUT);
    }

    public void error(Text text, int duration, int fadeOut) {
        currentError = new ErrorMessageState(text, duration, fadeOut);
    }

    public void tick() {
        if (currentError != null) {
            if (currentError.durationLeft <= 0) {
                currentError = null;
            } else {
                currentError.durationLeft -= 1;
            }
        }
        var client = MinecraftClient.getInstance();
        if (!client.options.useKey.isPressed()) {
            attemptDisplayed = false;
        }
    }

    public static class ErrorMessageState {
        public ErrorMessageState(String message, int durationLeft, int fadeOut) {
            this.message = Text.literal(message).formatted(Formatting.RED);
            this.durationLeft = durationLeft;
            this.fadeOut = fadeOut;
        }

        public ErrorMessageState(Text message, int durationLeft, int fadeOut) {
            this.message = message;
            this.durationLeft = durationLeft;
            this.fadeOut = fadeOut;
        }

        public Text message;
        public int durationLeft;
        public int fadeOut;
    }

    public ErrorMessageState currentError() {
        return currentError;
    }
}