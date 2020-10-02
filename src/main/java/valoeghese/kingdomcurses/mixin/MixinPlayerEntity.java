package valoeghese.kingdomcurses.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.player.PlayerEntity;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {
	/**
	 * @reason replace healing with long/short rest.
	 */
	@Inject(at = @At("RETURN"), method = "canFoodHeal", cancellable = true)
	private void onCanFoodHeal(CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(false);
	}
}
