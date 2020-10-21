package valoeghese.kingdomcurses.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import valoeghese.kingdomcurses.KingdomsAndCurses;
import valoeghese.kingdomcurses.WorldGen;
import valoeghese.kingdomcurses.util.Vec2i;

@Mixin(NoiseChunkGenerator.class)
public class MixinNoiseChunkGenerator {
	@Shadow
	@Final
	private long worldSeed;

	@Redirect(method = "sampleNoiseColumn([DII)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/Biome;getDepth()F"))
	private float onSampleDepth(Biome biome, double[] buffer, int biomeX, int biomeZ) {
		float depth = biome.getDepth();
		int x = biomeX << 2;
		int z = biomeZ << 2;

		Vec2i centre = KingdomsAndCurses.calcKingdom(this.worldSeed, x, z).getCityCentre();
		int dist = centre.manhattan(x, z);

		if (dist < WorldGen.CITY_SIZE * 2) {
			if (dist < WorldGen.CITY_SIZE) {
				depth = (depth + depth + 0.2f) / 3f;
				return biome.getCategory() == Biome.Category.RIVER ? depth : Math.max(-0.3f, depth);
			}

			return (depth + depth + depth + 0.2f) * 0.25f;
		}

		return depth;
	}

	@Redirect(method = "sampleNoiseColumn([DII)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/Biome;getScale()F"))
	private float onSampleScale(Biome biome, double[] buffer, int biomeX, int biomeZ) {
		float scale = biome.getScale();
		int x = biomeX << 2;
		int z = biomeZ << 2;

		Vec2i centre = KingdomsAndCurses.calcKingdom(this.worldSeed, x, z).getCityCentre();
		int dist = centre.manhattan(x, z);

		if (dist < WorldGen.CITY_SIZE * 2) {
			if (dist < WorldGen.CITY_SIZE) {
				return (scale + 0.01f) * 0.5f;
			}

			return (scale + scale + 0.01f) / 3f;
		}

		return scale;
	}
}
