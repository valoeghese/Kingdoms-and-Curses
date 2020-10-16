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
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.data.client.model.BlockStateVariantMap.TriFunction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.GameRules;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.feature.Feature;
import valoeghese.kingdomcurses.kingdom.Kingdom;
import valoeghese.kingdomcurses.kingdom.Voronoi;
import valoeghese.kingdomcurses.util.Noise;
import valoeghese.kingdomcurses.util.Vec2f;
import valoeghese.kingdomcurses.util.Vec2i;

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
						((LivingEntity) entity).applyStatusEffect(new StatusEffectInstance(StatusEffects.HEALTH_BOOST, Integer.MAX_VALUE, world.getRandom().nextInt(3) + 2, true, false));
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
			KingdomsAndCurses.genCity(region, region.getCenterChunkX() * 16, region.getCenterChunkZ() * 16);
			return false;
		});

		WorldGenEvents.POPULATE_ENTITIES.register((world, biome, chunkX, chunkZ, random, spawnPosGetter) -> {
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
						BlockPos pos = spawnPosGetter.getEntitySpawnPos(world, villager.getType(), ex, ez);
						villager.refreshPositionAndAngles(pos, random.nextFloat() * 360.0f, 0.0f);
						villager.headYaw = villager.yaw;
						villager.bodyYaw = villager.yaw;
						villager.initialize(world, world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null, null);
						world.spawnEntityAndPassengers(villager);
					}
				} else if (Curse.getCurse(world.toServerWorld(), kingdom) == Curse.NECROMANCY && dist > KingdomsAndCurses.CITY_SIZE_OUTER + 16) {
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
	private static void checkWorld(ServerWorld world) {
		if (worldCache != world) {
			kingdomIdMap = new Int2ObjectArrayMap<>();
			worldCache = world;
		}
	}

	public static Kingdom getKingdom(ServerWorld world, int x, int z) {
		checkWorld(world);
		return kingdomById(world, stats(world.getChunk(x >> 4, z >> 4, ChunkStatus.LIQUID_CARVERS)).getKingdom(x & 0xF, z & 0xF, (int) world.getSeed()), x, z);
	}

	public static Kingdom kingdomById(ServerWorld world, int kingdom, int x, int z) {
		checkWorld(world);
		return kingdomIdMap.computeIfAbsent(kingdom, id -> new Kingdom(world.getSeed(), id, Voronoi.sample(x / Kingdom.SCALE, z / Kingdom.SCALE, (int) world.getSeed())));
	}

	public static Kingdom kingdomById(ServerWorld world, Vec2f sample) {
		checkWorld(world);
		return kingdomIdMap.computeIfAbsent(sample.id(), id -> new Kingdom(world.getSeed(), id, sample));
	}

	private static int getHeightForGeneration(WorldAccess world, int x, int z) {
		BlockPos.Mutable pos = new BlockPos.Mutable(x, 0, z);

		for (int y = 256; y >= 0; --y) {
			pos.setY(y);

			if (world.testBlockState(pos, state -> {
				Block block = state.getBlock();
				return Feature.isSoil(block) || block == Blocks.ICE || block == Blocks.STONE || block == Blocks.GRANITE || block == Blocks.DIORITE || block == Blocks.ANDESITE || block == Blocks.SAND || block == Blocks.GRAVEL;
			})) {
				return y + 1;
			}
		}

		return 0;
	}

	public static void genCity(ChunkRegion world, int startX, int startZ) {
		if (!world.getDimension().hasFixedTime()) {
			try {
				ChunkRandom rand = new ChunkRandom(world.getSeed());
				rand.setPopulationSeed(world.getSeed(), startX, startZ);

				int seed = (int) world.getSeed();
				int seaLevel = world.getSeaLevel() - 2;
				ServerWorld serverWorld = world.toServerWorld();
				checkWorld(serverWorld);

				boolean roadsX = 0 == ((startX >> 5) & 0b1);
				boolean roadsZ = 0 == ((startZ >> 6) & 0b1);
				final int houseLimit = CITY_SIZE - 10;
				BlockPos.Mutable pos = new BlockPos.Mutable();

				for (int xo = 0; xo < 16; ++xo) {
					int x = startX + xo;
					pos.setX(x);

					for (int zo = 0; zo < 16; ++zo) {
						int z = startZ + zo;
						pos.setZ(z);

						// Determine Where in the kingdom we are
						Kingdom kingdom = getKingdom(serverWorld, x, z);

						Vec2i centre = kingdom.getCityCentre();
						int dist = centre.manhattan(x, z);

						if (dist > CITY_SIZE_OUTER) {
							int y = getHeightForGeneration(world, x, z);// - 1;

							if (y > seaLevel) {
								// Generate Kingdom Roads
								Vec2i south = kingdomById(serverWorld, kingdom.neighbourKingdomVec(0, 1, seed)).getCityCentre();
								Vec2i east = kingdomById(serverWorld, kingdom.neighbourKingdomVec(1, 0, seed)).getCityCentre();
								Vec2i north = kingdomById(serverWorld, kingdom.neighbourKingdomVec(0, -1, seed)).getCityCentre();
								Vec2i west = kingdomById(serverWorld, kingdom.neighbourKingdomVec(-1, 0, seed)).getCityCentre();

								// write road if at a road location
								if (isNearLineBetween(centre, south, x, z, 7) || isNearLineBetween(centre, east, x, z, 7)
										|| isNearLineBetween(centre, north, x, z, 7) || isNearLineBetween(centre, west, x, z, 7)) {
									pos.setY(y);
									world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);

									pos.setY(y - 1);
									world.setBlockState(pos, Blocks.GRASS_PATH.getDefaultState(), 3);
								} else {
									// generate paths at path locations
									double path = PATH_NOISE.sample((double) x / 400.0, (double) z / 400.0);

									if (path > 0 && path < 0.019) {
										pos.setY(y);
										world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);

										pos.setY(y - 1);
										world.setBlockState(pos, Blocks.GRASS_PATH.getDefaultState(), 3);
									}
								}
							}
						} else if (dist >= CITY_SIZE) {
							final int height = (dist == CITY_SIZE || dist == CITY_SIZE_OUTER) ? 9 : 8;
							int startY = getHeightForGeneration(world, x, z);

							if (startY > seaLevel) {
								Vec2i south = kingdomById(serverWorld, kingdom.neighbourKingdomVec(0, 1, seed)).getCityCentre();
								Vec2i east = kingdomById(serverWorld, kingdom.neighbourKingdomVec(1, 0, seed)).getCityCentre();
								Vec2i north = kingdomById(serverWorld, kingdom.neighbourKingdomVec(0, -1, seed)).getCityCentre();
								Vec2i west = kingdomById(serverWorld, kingdom.neighbourKingdomVec(-1, 0, seed)).getCityCentre();

								// write gates
								if (isNearLineBetween(centre, south, x, z, 7) || isNearLineBetween(centre, east, x, z, 7)
										|| isNearLineBetween(centre, north, x, z, 7) || isNearLineBetween(centre, west, x, z, 7)) {
									for (int yo = 4; yo < height; ++yo) {
										int y = startY + yo;
										pos.setY(y);

										if (!ServerWorld.isHeightInvalid(pos)) {
											world.setBlockState(pos, Blocks.STONE_BRICKS.getDefaultState(), 3);
										}
									}

									pos.setY(startY - 1);
									world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);

									pos.setY(startY - 2);
									world.setBlockState(pos, Blocks.GRASS_PATH.getDefaultState(), 3);
								} else { // write wall
									for (int yo = 0; yo < height; ++yo) {
										int y = startY + yo;
										pos.setY(y);

										if (!ServerWorld.isHeightInvalid(pos)) {
											world.setBlockState(pos, Blocks.STONE_BRICKS.getDefaultState(), 3);
										}
									}
								}
							}
						} else {
							int y = getHeightForGeneration(world, x, z) - 1;

							if (y > seaLevel) {
								// Generate Cities
								if (dist < houseLimit && xo == 8 && zo == 8) {
									switch (rand.nextInt(6)) {
									case 0:
									case 1:
									case 2:
									case 3:
										generateHouse(world, rand, x, y, z);
										break;
									case 4:
										BlockPos.Mutable pos2 = new BlockPos.Mutable();

										for (int xoo = -2; xoo <= 2; ++xoo) {
											pos2.setX(x + xo - 8 + xoo);
											boolean xedge = xoo == -2 || xoo == 2;

											for (int zoo = -2; zoo <= 2; ++zoo) { 
												pos2.setZ(z + zo - 8 + zoo);
												boolean zedge = zoo == -2 || zoo == 2;
												int height = 0;

												if (xedge || zedge) {
													height = 1;
												} else if (xoo == 0 && zoo == 0) {
													height = 2;
												}

												for (int yoo = 0; yoo <= height; ++yoo) {
													pos2.setY(y + yoo);
													world.setBlockState(pos2, Blocks.SMOOTH_STONE.getDefaultState(), 3);
												}

												if (height == 2) {
													pos2.setY(y + 3);
													world.setBlockState(pos2, Blocks.WATER.getDefaultState(), 3);
												}
											}
										}
										break;
									case 5: // nil
										break;
									}
								}

								if ((roadsX && xo < 2) || (roadsZ && zo < 3)) {
									// Generate City Roads
									pos.setY(y);
									world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);

									pos.setY(y - 1);
									world.setBlockState(pos, Blocks.GRASS_PATH.getDefaultState(), 3);
								}
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Exception generating cities", e);
			}
		}
	}

	private static void generateHouse(ChunkRegion world, ChunkRandom rand, int x, int y, int z) {
		x += rand.nextInt(3) - 1;
		z += rand.nextInt(3) - 1;

		final int houseHeight = 4 + rand.nextInt(3);
		final int wallHeight = houseHeight;

		// Generate City House
		// Floor and Walls
		BlockPos.Mutable pos2 = new BlockPos.Mutable();

		for (int xoo = -5; xoo < 5; ++xoo) {
			boolean xedge = xoo == -5 || xoo == 4;
			int xx = x + xoo;
			pos2.setX(xx);

			for (int zoo = -5; zoo < 5; ++zoo) {
				int zz = z + zoo;
				pos2.setZ(zz);
				pos2.setY(y);

				if (!ServerWorld.isHeightInvalid(pos2)) {
					world.setBlockState(pos2, Blocks.OAK_PLANKS.getDefaultState(), 3);
				}

				if (xedge || zoo == -5 || zoo == 4) {
					for (int yy = 0; yy < wallHeight; ++yy) {
						pos2.setY(y + yy);

						if (!ServerWorld.isHeightInvalid(pos2)) {
							if (y > 0 && y < 3) {
								if (xedge && zoo == 0) {
									continue;
								}
							}

							world.setBlockState(pos2, Blocks.BRICKS.getDefaultState(), 3);
						}
					}
				}
			}
		}

		// Roof
		for (int yy = -1; yy < 2; ++yy) {
			int width = 6 - yy;
			int finalY = y + yy + houseHeight;
			pos2.setY(finalY);

			int l = -width;
			int h = width - 1;

			for (int xoo = -width; xoo < width; ++xoo) {
				int finalX = x + xoo;
				pos2.setX(finalX);

				for (int zoo = -width; zoo < width; ++zoo) {
					int finalZ = z + zoo;
					pos2.setZ(finalZ);

					if (yy > -1 || zoo == l || zoo == h || xoo == l || xoo == h) {
						if (!ServerWorld.isHeightInvalid(pos2)) {
							world.setBlockState(pos2, Blocks.STONE_BRICKS.getDefaultState(), 3);
						}
					}
				}
			}
		}

		// Pillars
		for (int yy = 0; yy < houseHeight; ++yy) {
			if (y + yy < 256) {
				pos2.setY(y + yy);
				pos2.setX(x - 6);
				pos2.setZ(z - 6);

				world.setBlockState(pos2, Blocks.OAK_LOG.getDefaultState(), 3);

				pos2.setX(x + 5);
				pos2.setZ(z + 5);

				world.setBlockState(pos2, Blocks.OAK_LOG.getDefaultState(), 3);

				pos2.setZ(z - 6);

				world.setBlockState(pos2, Blocks.OAK_LOG.getDefaultState(), 3);

				pos2.setX(x - 6);
				pos2.setZ(z + 5);

				world.setBlockState(pos2, Blocks.OAK_LOG.getDefaultState(), 3);
			}
		}
	}

	private static boolean isNearLineBetween(Vec2i locA, Vec2i locB, int x, int y, int threshold) {
		float m = (float) (locB.getY() - locA.getY()) / (float) (locB.getX() - locA.getX());
		float targetY = m * x + locA.getY() - m * locA.getX();
		return Math.abs(y - targetY) < threshold;
	}

	private static ServerWorld worldCache;
	private static Int2ObjectMap<Kingdom> kingdomIdMap = new Int2ObjectArrayMap<>();

	//	public static final SpawnEntry VILLAGER_ENTRY = new SpawnEntry(EntityType.VILLAGER, 100, 3, 4);
	private static final Noise PATH_NOISE = new Noise(new Random(69420));
	public static final int CITY_SIZE = 115;
	public static final int CITY_SIZE_OUTER = CITY_SIZE + 5;

	public static final ComponentType<PlayerStats> PLAYER_STATS = ComponentRegistry.INSTANCE.registerIfAbsent(new Identifier("kingdom_curses", "player_stats"), PlayerStats.class);
	public static final ComponentType<ChunkStats> CHUNK_STATS = ComponentRegistry.INSTANCE.registerIfAbsent(new Identifier("kingdom_curses", "chunk_stats"), ChunkStats.class);

	public static final Logger LOGGER = LogManager.getLogger("Kingdoms and Curses");

	public static void spawnNecromancy(Random random, ServerWorldAccess world, int x, int z, boolean nonpop, TriFunction<EntityType<?>, Integer, Integer, BlockPos> getEntitySpawn) {
		int target = random.nextInt(3) + 1;

		if (world.getEntitiesByClass(MobEntity.class, new Box(x, 0, z, x + 16, 256, z + 16), me -> true).size() < 60) {
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

				if (SpawnRestriction.canSpawn(entity.getType(), world, SpawnReason.NATURAL, pos, world.getRandom())) {
					world.spawnEntityAndPassengers(entity);
				}
			}
		}
	}
}
