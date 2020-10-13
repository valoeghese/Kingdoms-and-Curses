package valoeghese.kingdomcurses.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import valoeghese.kingdomcurses.KingdomsAndCurses;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {
	public MixinLivingEntity(EntityType<?> type, World world) {
		super(type, world);
	}

	@Inject(at = @At("HEAD"), method = "setSprinting")
	private void onSetSprinting(boolean sprinting, CallbackInfo info) {
		LivingEntity self = (LivingEntity) (Object) this;

		if (self instanceof PlayerEntity) {
			PlayerEntity player = (PlayerEntity) self;
			KingdomsAndCurses.stats(player).setUnlockVonSprint(this.world.getTime(), !sprinting);
		}
	}

	@Shadow
	public abstract AttributeContainer getAttributes();

	/*@Override
	public void setMaxHealth(float maxHealth) {
		this.getAttributes().getValue(EntityAttributes.GENERIC_MAX_HEALTH);
		//this.dataTracker.set(EntityAttributes.GENERIC_MAX_HEALTH, maxHealth);
	}*/
}
