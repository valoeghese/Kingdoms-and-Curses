package valoeghese.kingdomcurses.mixin;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
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

	@Inject(at = @At("RETURN"), method = "populateEntities")
	private static void onPopulateEntities(ServerWorldAccess world, Biome biome, int chunkX, int chunkZ, Random random, CallbackInfo info) {
		int z = chunkZ << 4;
		int x = chunkX << 4;

		Kingdom kingdom = KingdomsAndCurses.getKingdom(world.toServerWorld(), x, z);

		if (biome.getCategory() != Biome.Category.OCEAN) {
			int dist = kingdom.getCityCentre().manhattan(x, z);

			if (dist < KingdomsAndCurses.CITY_SIZE) {
				int target = random.nextInt(3) + 1;

				for (int i = 0; i < target; ++i) {
					int ex = x + random.nextInt(16);
					int ez = z + random.nextInt(16);

					VillagerEntity villager = EntityType.VILLAGER.create(world.toServerWorld());
					BlockPos pos = getEntitySpawnPos(world, villager.getType(), ex, ez);
					villager.refreshPositionAndAngles(pos, random.nextFloat() * 360.0f, 0.0f);
					villager.headYaw = villager.yaw;
					villager.bodyYaw = villager.yaw;
					villager.initialize(world, world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null, null);
					world.spawnEntityAndPassengers(villager);
				}
			} else if (Curse.getCurse(world.toServerWorld(), kingdom) == Curse.NECROMANCY && dist > KingdomsAndCurses.CITY_SIZE_OUTER + 16) {
				KingdomsAndCurses.spawnNecromancy(random, world, x, z, (ent, ex, ez) -> getEntitySpawnPos(world, ent, ex, ez));
			}
		} else if (Curse.getCurse(world.toServerWorld(), kingdom) == Curse.NECROMANCY) {
			KingdomsAndCurses.spawnNecromancy(random, world, x, z, (ent, ex, ez) -> getEntitySpawnPos(world, ent, ex, ez));
		}
	}

	@Shadow
	private static BlockPos getEntitySpawnPos(WorldView world, EntityType<?> entityType, int x, int z) {
		throw new RuntimeException("Mixin getEntitySpawnPos failed to apply!");
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
