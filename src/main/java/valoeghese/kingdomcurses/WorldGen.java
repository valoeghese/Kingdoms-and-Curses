package valoeghese.kingdomcurses;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.feature.Feature;
import valoeghese.kingdomcurses.kingdom.Kingdom;
import valoeghese.kingdomcurses.util.Noise;
import valoeghese.kingdomcurses.util.Vec2i;

public final class WorldGen {
	public static void genGraveyard(ChunkRegion world, Random rand, int startX, int startZ) {
		if (!world.getDimension().hasFixedTime()) {
			ServerWorld sworld = world.toServerWorld();

			if (Curse.getCurse(sworld, KingdomsAndCurses.getKingdom(sworld, startX, startZ)) == Curse.NECROMANCY) {
				if (rand.nextInt(512) == 0) {
					int remaining = 7;
					int rBound = 256 - remaining;

					BlockPos.Mutable mut = new BlockPos.Mutable();

					for (int xo = 0; xo < 16; ++xo) {
						int x = startX + xo;

						for (int zo = 0; zo < 16; ++zo) {
							if (rand.nextInt(Math.max(rBound--, 1)) == 0) {
								rBound++;
								int z = startZ + zo;
								int y = getHeightForGeneration(world, x, z);

								world.setBlockState(mut.set(x, y, z), Blocks.STONE_BRICKS.getDefaultState(), 3);
							}
						}
					}
				}
			}
		}
	}

	public static void genCity(ChunkRegion world, int startX, int startZ) {
		if (!world.getDimension().hasFixedTime()) {
			try {
				ChunkRandom rand = new ChunkRandom(world.getSeed());
				rand.setPopulationSeed(world.getSeed(), startX, startZ);

				int seed = (int) world.getSeed();
				int seaLevel = world.getSeaLevel() - 2;
				ServerWorld serverWorld = world.toServerWorld();
				KingdomsAndCurses.checkWorld(serverWorld);

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
						Kingdom kingdom = KingdomsAndCurses.getKingdom(serverWorld, x, z);

						Vec2i centre = kingdom.getCityCentre();
						int dist = centre.manhattan(x, z);

						if (dist > CITY_SIZE_OUTER) {
							int y = getHeightForGeneration(world, x, z) - 1;

							if (y > seaLevel /* + 1 */) { //TODO ?
								// Generate Kingdom Roads
								Vec2i south = KingdomsAndCurses.kingdomById(serverWorld, kingdom.neighbourKingdomVec(0, 1, seed)).getCityCentre();
								Vec2i east = KingdomsAndCurses.kingdomById(serverWorld, kingdom.neighbourKingdomVec(1, 0, seed)).getCityCentre();
								Vec2i north = KingdomsAndCurses.kingdomById(serverWorld, kingdom.neighbourKingdomVec(0, -1, seed)).getCityCentre();
								Vec2i west = KingdomsAndCurses.kingdomById(serverWorld, kingdom.neighbourKingdomVec(-1, 0, seed)).getCityCentre();

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
								Vec2i south = KingdomsAndCurses.kingdomById(serverWorld, kingdom.neighbourKingdomVec(0, 1, seed)).getCityCentre();
								Vec2i east = KingdomsAndCurses.kingdomById(serverWorld, kingdom.neighbourKingdomVec(1, 0, seed)).getCityCentre();
								Vec2i north = KingdomsAndCurses.kingdomById(serverWorld, kingdom.neighbourKingdomVec(0, -1, seed)).getCityCentre();
								Vec2i west = KingdomsAndCurses.kingdomById(serverWorld, kingdom.neighbourKingdomVec(-1, 0, seed)).getCityCentre();

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

	private static final Noise PATH_NOISE = new Noise(new Random(69420));
	public static final int CITY_SIZE = 115;
	public static final int CITY_SIZE_OUTER = CITY_SIZE + 5;
}
