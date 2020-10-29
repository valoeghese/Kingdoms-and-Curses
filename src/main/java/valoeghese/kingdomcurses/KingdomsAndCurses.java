package valoeghese.kingdomcurses;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.fabriccommunity.events.world.EntitySpawnCallback;
import io.github.fabriccommunity.events.world.WorldGenEvents;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import nerdhub.cardinal.components.api.ComponentRegistry;
import nerdhub.cardinal.components.api.ComponentType;
import nerdhub.cardinal.components.api.event.ChunkComponentCallback;
import nerdhub.cardinal.components.api.event.EntityComponentCallback;
import nerdhub.cardinal.components.api.util.EntityComponents;
import nerdhub.cardinal.components.api.util.RespawnCopyStrategy;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.data.client.model.BlockStateVariantMap.TriFunction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.GameRules;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import valoeghese.kingdomcurses.kingdom.Kingdom;
import valoeghese.kingdomcurses.kingdom.Voronoi;
import valoeghese.kingdomcurses.util.Vec2f;

public class KingdomsAndCurses implements ModInitializer {
	@Override
	public void onInitialize() {
		EntityComponents.setRespawnCopyStrategy(PLAYER_STATS, RespawnCopyStrategy.LOSSLESS_ONLY);
		EntityComponentCallback.event(PlayerEntity.class).register((player, components) -> components.put(PLAYER_STATS, new PlayerStats(player)));

		ChunkComponentCallback.EVENT.register((chunk, components) -> components.put(CHUNK_STATS, new ChunkStats(chunk)));

		// TODO use custom callbacks that run afterwards, only on success?
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			stats(player).attackedAt(world.getTime());
			return ActionResult.PASS;
		});

		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			stats(player).attackedAt(world.getTime());
			return ActionResult.PASS;
		});

		EntitySpawnCallback.POST.register((entity, world, pos, reason) -> {
			if (reason != SpawnReason.SPAWN_EGG) {
				Curse curse = Curse.getCurse(world.toServerWorld(), getKingdom(world.toServerWorld(), (int) pos.getX(), (int) pos.getZ()));
				EntityType<?> type = entity.getType();

				switch (curse) {
				case NECROMANCY:
					if (type == EntityType.ZOMBIE || type == EntityType.SKELETON) {
						((LivingEntity) entity).addStatusEffect(new StatusEffectInstance(StatusEffects.HEALTH_BOOST, Integer.MAX_VALUE, world.getRandom().nextInt(3) + 2, true, false));
					}
					break;
				default:
					break;
				}
			}
		});

		CommandRegistrationCallback.EVENT.register((ctxt, d) -> {
			ctxt.register(CommandManager.literal("nearestcity")
					.requires(stack -> stack.hasPermissionLevel(2))
					.executes(context -> {
						Entity entity = context.getSource().getEntity();

						if (entity != null && entity instanceof PlayerEntity) {
							ServerPlayerEntity pe = context.getSource().getPlayer();
							BlockPos pos = pe.getBlockPos();
							int px = pos.getX();
							int pz = pos.getZ();
							context.getSource().sendFeedback(new LiteralText(getKingdom((ServerWorld) pe.getEntityWorld(), px, pz).getCityCentre().toString()), false);
						}

						return 1;
					}));
		});

		CommandRegistrationCallback.EVENT.register((ctxt, d) -> {
			ctxt.register(CommandManager.literal("kingdom")
					.requires(stack -> stack.hasPermissionLevel(2))
					.executes(context -> {
						Entity entity = context.getSource().getEntity();

						if (entity != null && entity instanceof PlayerEntity) {
							ServerPlayerEntity pe = context.getSource().getPlayer();
							BlockPos pos = pe.getBlockPos();
							int px = pos.getX();
							int pz = pos.getZ();
							Kingdom kingdom = getKingdom((ServerWorld) pe.getEntityWorld(), px, pz);
							context.getSource().sendFeedback(new LiteralText(kingdom.toString() + " (" + String.valueOf(Curse.getCurse(pe.getServerWorld(), kingdom)) + ")"), false);
						}

						return 1;
					}));
		});

		WorldGenEvents.GENERATE_FEATURES_START.register((gen, region, structures) -> {
			WorldGen.genCity(region, region.getCenterChunkX() * 16, region.getCenterChunkZ() * 16);
			return false;
		});

		WorldGenEvents.GENERATE_FEATURES_END.register((gen, region, structures) -> {
			WorldGen.genGraveyard(region, region.getRandom(), region.getCenterChunkX() * 16, region.getCenterChunkZ() * 16);
		});

		WorldGenEvents.POPULATE_ENTITIES.register((world, biome, chunkX, chunkZ, random, spawnPosGetter) -> {
			int z = chunkZ << 4;
			int x = chunkX << 4;

			Kingdom kingdom = KingdomsAndCurses.getKingdom(world.toServerWorld(), x, z);

			if (biome.getCategory() != Biome.Category.OCEAN) {
				int dist = kingdom.getCityCentre().manhattan(x, z);

				if (dist < WorldGen.CITY_SIZE) {
					int target = random.nextInt(3) + 1;

					for (int i = 0; i < target; ++i) {
						int ex = x + random.nextInt(16);
						int ez = z + random.nextInt(16);

						VillagerEntity villager = EntityType.VILLAGER.create(world.toServerWorld());
						BlockPos pos = spawnPosGetter.getEntitySpawnPos(world, villager.getType(), ex, ez);
						villager.refreshPositionAndAngles(pos, random.nextFloat() * 360.0f, 0.0f);
						villager.headYaw = villager.yaw;
						villager.bodyYaw = villager.yaw;
						villager.initialize(world, world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null, null);
						world.spawnEntityAndPassengers(villager);
					}
				} else if (Curse.getCurse(world.toServerWorld(), kingdom) == Curse.NECROMANCY && dist > WorldGen.CITY_SIZE_OUTER + 16) {
					KingdomsAndCurses.spawnNecromancy(random, world, x, z, false, (ent, ex, ez) -> spawnPosGetter.getEntitySpawnPos(world, ent, ex, ez));
				}
			} else if (Curse.getCurse(world.toServerWorld(), kingdom) == Curse.NECROMANCY) {
				KingdomsAndCurses.spawnNecromancy(random, world, x, z, false, (ent, ex, ez) -> spawnPosGetter.getEntitySpawnPos(world, ent, ex, ez));
			}
		});

		// TODO
		// - Curses
	}

	public static void longRest(World world) {
		if (world.getGameRules().getBoolean(GameRules.NATURAL_REGENERATION)) {
			world.getPlayers().stream().filter(LivingEntity::isSleeping).forEach(playerEntity -> {
				if (playerEntity.getHealth() < playerEntity.getMaxHealth() && playerEntity.getHungerManager().getFoodLevel() > 0) {
					playerEntity.heal(playerEntity.getMaxHealth() / 2);
					playerEntity.addExhaustion(30.0f);
					stats(playerEntity).resetHealPoints();
				}
			});
		}
	}

	public static PlayerStats stats(PlayerEntity player) {
		return PLAYER_STATS.get(player);
	}

	public static ChunkStats stats(Chunk chunk) {
		return CHUNK_STATS.get(chunk);
	}

	// Kingdom
	public static void checkWorld(long seed) {
		if (seedCache == null || seedCache != seed) {
			kingdomIdMap = new Int2ObjectArrayMap<>();
			seedCache = seed;
		}
	}

	public static Kingdom getKingdom(ServerWorld world, int x, int z) {
		return getKingdom(world, x, z, ChunkStatus.LIQUID_CARVERS);
	}

	private static Kingdom getKingdom(ServerWorld world, int x, int z, ChunkStatus status) {
		checkWorld(world.getSeed());
		return kingdomById(world, stats(world.getChunk(x >> 4, z >> 4, status)).getKingdom(x & 0xF, z & 0xF, (int) world.getSeed()), x, z);
	}

	public static Kingdom calcKingdom(long seed, int x, int z) {
		checkWorld(seed);

		float sampleX = (float) x / Kingdom.SCALE;
		float sampleZ = (float) z / Kingdom.SCALE;
		Vec2f vec2f = Voronoi.sample(sampleX, sampleZ, (int) seed);
		return kingdomById(seed, vec2f);
	}

	private static Kingdom kingdomById(ServerWorld world, int kingdom, int x, int z) {
		checkWorld(world.getSeed());
		return kingdomIdMap.computeIfAbsent(kingdom, id -> new Kingdom(world.getSeed(), id, Voronoi.sample(x / Kingdom.SCALE, z / Kingdom.SCALE, (int) world.getSeed())));
	}

	static Kingdom kingdomById(long seed, Vec2f sample) {
		checkWorld(seed);
		return kingdomIdMap.computeIfAbsent(sample.id(), id -> new Kingdom(seed, id, sample));
	}

	// useful for limiting spawns
	static boolean isNecromancyChunk(int cx, int cz) {
		return ((cx * 136763 + cz * -8139467) & 0b1111) < 3;
	}

	public static void spawnNecromancy(Random random, ServerWorldAccess world, int x, int z, boolean nonpop, TriFunction<EntityType<?>, Integer, Integer, BlockPos> getEntitySpawn) {
		if (isNecromancyChunk(x >> 4, z >> 4)) {
			int hostileEntityCount = world.getEntitiesByClass(HostileEntity.class, new Box(x, 0, z, x + 16, 256, z + 16), me -> true).size();

			if (hostileEntityCount < 19) {
				int target = random.nextInt(3) + 1;

				for (int i = 0; i < target; ++i) {
					int ex = x + random.nextInt(16);
					int ez = z + random.nextInt(16);

					MobEntity entity = (random.nextInt(3) == 0 ? EntityType.ZOMBIE : EntityType.SKELETON).create(world.toServerWorld());
					BlockPos pos = getEntitySpawn.apply(entity.getType(), ex, ez);
					entity.refreshPositionAndAngles(pos, random.nextFloat() * 360.0f, 0.0f);
					entity.headYaw = entity.yaw;
					entity.bodyYaw = entity.yaw;

					if (nonpop) {
						PlayerEntity player = world.getClosestPlayer(entity, 50.0);

						if (player == null) {
							continue;
						}

						/*Vec3d playerPos = player.getPos();
					Vec3d entityPos = entity.getPos();
					double dx = playerPos.x - entityPos.x;
					double dz = playerPos.z - entityPos.z;

					double horizontalSqrDist = dx * dx + dz * dz;

					if (horizontalSqrDist < 40) {

					}*/
					}

					entity.initialize(world, world.getLocalDifficulty(pos), SpawnReason.NATURAL, null, null);

					if (world.getBlockState(pos.up()).isAir() && world.getBlockState(pos).isAir() && SpawnRestriction.canSpawn(entity.getType(), world, SpawnReason.NATURAL, pos, world.getRandom())) {
						world.spawnEntityAndPassengers(entity);
					}
				}
			}
		}
	}

	private static Long seedCache = null;
	private static Int2ObjectMap<Kingdom> kingdomIdMap = new Int2ObjectArrayMap<>();

	//	public static final SpawnEntry VILLAGER_ENTRY = new SpawnEntry(EntityType.VILLAGER, 100, 3, 4);

	public static final ComponentType<PlayerStats> PLAYER_STATS = ComponentRegistry.INSTANCE.registerIfAbsent(new Identifier("kingdom_curses", "player_stats"), PlayerStats.class);
	public static final ComponentType<ChunkStats> CHUNK_STATS = ComponentRegistry.INSTANCE.registerIfAbsent(new Identifier("kingdom_curses", "chunk_stats"), ChunkStats.class);

	public static final Logger LOGGER = LogManager.getLogger("Kingdoms and Curses");
}
