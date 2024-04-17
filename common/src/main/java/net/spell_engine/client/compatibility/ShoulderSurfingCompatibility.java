package net.spell_engine.client.compatibility;

import com.github.exopandora.shouldersurfing.api.IShoulderSurfingPlugin;
import com.github.exopandora.shouldersurfing.api.IShoulderSurfingRegistrar;
import net.spell_engine.internals.SpellContainerHelper;

public class ShoulderSurfingCompatibility implements IShoulderSurfingPlugin {
    @Override
    public void register(IShoulderSurfingRegistrar registrar) {
        registrar.registerAdaptiveItemCallback(SpellContainerHelper::hasUsableContainer);
    }
}
