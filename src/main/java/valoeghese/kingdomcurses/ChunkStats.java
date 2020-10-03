package valoeghese.kingdomcurses;

import nerdhub.cardinal.components.api.util.sync.ChunkSyncedComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.chunk.Chunk;
import valoeghese.kingdomcurses.kingdom.Kingdom;
import valoeghese.kingdomcurses.kingdom.Voronoi;

public class ChunkStats implements ChunkSyncedComponent<ChunkStats> {
	public ChunkStats(Chunk chunk) {
		this.parent = chunk;
	}

	private final Chunk parent;
	private int[] kingdoms;

	@Override
	public void fromTag(CompoundTag tag) {
		if (tag.contains("kingdoms")) {
			this.kingdoms = tag.getIntArray("kingdoms");
		}
	}

	@Override
	public CompoundTag toTag(CompoundTag tag) {
		if (this.kingdoms != null) {
			tag.putIntArray("kingdoms", this.kingdoms);
		}

		return tag;
	}

	public int getKingdom(int x, int z, int seed) {
		if (this.kingdoms == null) {
			this.kingdoms = new int[256];

			for (int kx = 0; kx < 16; ++kx) {
				float sampleX = (float) (this.parent.getPos().getStartX() + kx) / Kingdom.SCALE;

				for (int kz = 0; kz < 16; ++kz) {
					float sampleZ = (float) (this.parent.getPos().getStartZ() + kz) / Kingdom.SCALE;
					this.kingdoms[loc(kx, kz)] = Voronoi.sample(sampleX, sampleZ, seed).id();
				}
			}
		}

		return this.kingdoms[loc(x, z)];
	}

	@Override
	public Chunk getChunk() {
		return this.parent;
	}

	private static int loc(int x, int z) {
		return x * 16 + z;
	}
}
