package valoeghese.kingdomcurses.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import valoeghese.kingdomcurses.KingdomsAndCurses;

@Mixin(ServerWorld.class)
public class MixinServerWorld {
	@Inject(at = @At("HEAD"), method = "wakeSleepingPlayers")
	private void onWakeSleepingPlayers(CallbackInfo info) {
		World self = (World) (Object) this;
		KingdomsAndCurses.longRest(self);
	}
}
