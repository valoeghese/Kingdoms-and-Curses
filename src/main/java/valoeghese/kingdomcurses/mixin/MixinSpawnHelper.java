package valoeghese.kingdomcurses.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;
import valoeghese.kingdomcurses.Curse;
import valoeghese.kingdomcurses.KingdomsAndCurses;
import valoeghese.kingdomcurses.kingdom.Kingdom;

@Mixin(SpawnHelper.class)
public class MixinSpawnHelper {
	/*@Redirect(at = @At(value = "INVOKE", target = "net/minecraft/world/biome/SpawnSettings.getCreatureSpawnProbability()F"),
			method = "populateEntities")
	private static float onSpawnChance(SpawnSettings settings, ServerWorldAccess world, Biome biome, int chunkX, int chunkZ, Random random) {
		float value = settings.getCreatureSpawnProbability();

	}*/

	@Inject(at = @At("RETURN"), method = "spawn")
	private static void onSpawn(ServerWorld world, WorldChunk chunk, SpawnHelper.Info info, boolean spawnAnimals, boolean spawnMonsters, boolean shouldSpawnAnimals, CallbackInfo cinfo) {
		if (spawnMonsters) {
			BlockPos spawnPos = getSpawnPos(world, chunk);

			int z = chunk.getPos().getStartZ();
			int x = chunk.getPos().getStartX();
			Kingdom kingdom = KingdomsAndCurses.getKingdom(world.toServerWorld(), x, z);

			if (chunk.getBiomeArray().getBiomeForNoiseGen(0, 75 >> 3, 0).getCategory() != Biome.Category.OCEAN) {
				int dist = kingdom.getCityCentre().manhattan(x, z);

				if (Curse.getCurse(world.toServerWorld(), kingdom) == Curse.NECROMANCY && dist > KingdomsAndCurses.CITY_SIZE_OUTER + 16) {
					KingdomsAndCurses.spawnNecromancy(world.getRandom(), world, x, z, (ent, ex, ez) -> spawnPos);
				}
			} else if (Curse.getCurse(world.toServerWorld(), kingdom) == Curse.NECROMANCY) {
				KingdomsAndCurses.spawnNecromancy(world.getRandom(), world, x, z, (ent, ex, ez) -> spawnPos);
			}
		}
	}

	@Shadow
	private static BlockPos getSpawnPos(World world, WorldChunk chunk) {
		throw new RuntimeException("Mixin getSpawnPos failed to apply!");
	}

	/*@Redirect(
			method = "populateEntities",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/SpawnSettings;getSpawnEntry(Lnet/minecraft/entity/SpawnGroup;)Ljava/util/List;")
			)
	private static List<SpawnEntry> redirectGetSpawnEntries(SpawnSettings settings, SpawnGroup group, ServerWorldAccess serverWorldAccess, Biome biome, int chunkX, int chunkZ, Random random) {
		List<SpawnEntry> result = settings.getSpawnEntry(group);

		if (KingdomsAndCurses.getKingdom(serverWorldAccess.toServerWorld(), chunkX << 4, chunkZ << 4).getCityCentre().manhattan(chunkX << 4, chunkZ << 4) < KingdomsAndCurses.CITY_SIZE - 10) {
			result.add(KingdomsAndCurses.VILLAGER_ENTRY);
		}

		return result;
	}*/
}
