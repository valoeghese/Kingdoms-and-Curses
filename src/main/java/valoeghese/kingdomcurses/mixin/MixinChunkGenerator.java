package valoeghese.kingdomcurses.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.ChunkRegion;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import valoeghese.kingdomcurses.KingdomsAndCurses;

@Mixin(ChunkGenerator.class)
public class MixinChunkGenerator {
	@Inject(at = @At("HEAD"), method = "generateFeatures")
	private void onGenerateFeatures(ChunkRegion region, StructureAccessor accessor, CallbackInfo info) {
		KingdomsAndCurses.genCity(region, region.getCenterChunkX() * 16, region.getCenterChunkZ() * 16);
	}
}
