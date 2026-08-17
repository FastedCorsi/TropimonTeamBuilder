package fr.tropimon.teamsaver.mixin.client;

import com.bedrockk.molang.runtime.value.DoubleValue;
import com.bedrockk.molang.runtime.value.MoValue;
import com.cobblemon.mod.common.api.events.pokemon.HeldItemEvent;
import com.cobblemon.mod.common.api.molang.MoLangFunctions;
import fr.tropimon.teamsaver.client.AutomationVisibility;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplies the client registry context missing from Cobblemon 1.7.2 during automated held-item updates. */
@Mixin(value = HeldItemEvent.Post.class, remap = false)
abstract class HeldItemPostMixin {
    @Inject(method = "getContext", at = @At("HEAD"), cancellable = true, remap = false)
    private void tropimon$useClientRegistryForAutomatedItemUpdate(
            CallbackInfoReturnable<Map<String, MoValue>> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!AutomationVisibility.isAutomationActive() || client.world == null || !client.isOnThread()) return;

        HeldItemEvent.Post event = (HeldItemEvent.Post) (Object) this;
        Map<String, MoValue> context = new LinkedHashMap<>();
        context.put("pokemon", event.getPokemon().getStruct());
        context.put("received", MoLangFunctions.INSTANCE.asMoLangValue(
                event.getReceived(), client.world.getRegistryManager()));
        context.put("returned", MoLangFunctions.INSTANCE.asMoLangValue(
                event.getReturned(), client.world.getRegistryManager()));
        context.put("decremented", new DoubleValue(event.getDecremented()));
        cir.setReturnValue(context);
    }
}
