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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import valoeghese.kingdomcurses.KingdomsAndCurses;

@Mixin(SpawnHelper.class)
public class MixinSpawnHelper {
	@Inject(at = @At("RETURN"), method = "populateEntities")
	private static void onPopulateEntities(ServerWorldAccess world, Biome biome, int chunkX, int chunkZ, Random random, CallbackInfo info) {
		int z = chunkZ << 4;
		int x = chunkX << 4;

		if (biome.getCategory() != Biome.Category.OCEAN) {
			if (KingdomsAndCurses.getKingdom(world.toServerWorld(), x, z).getCityCentre().manhattan(x, z) < KingdomsAndCurses.CITY_SIZE) {
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
			}
		}
	}

	@Shadow
	private static BlockPos getEntitySpawnPos(WorldView world, EntityType<?> entityType, int x, int z) {
		throw new RuntimeException("Mixin getEntitySpawnPos failed to apply!");
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
