package valoeghese.kingdomcurses;

import nerdhub.cardinal.components.api.util.sync.EntitySyncedComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;

public class PlayerStats implements EntitySyncedComponent {
	public PlayerStats(PlayerEntity player) {
		this.parent = player;
	}

	private float healpoints = 8.0f;
	private long sprintTimeTarget = 0;
	private long attackTimeTarget = 0;
	private boolean sprintUnlock = true;
	private final PlayerEntity parent;

	// Checks

	public boolean allowNaturalHeal(long tickTime) {
		return this.sprintUnlock && this.healpoints > 0.0f && tickTime > this.sprintTimeTarget && tickTime > this.attackTimeTarget;
	}

	// Updating Stuff

	public float expendHealPoints(float heal) {
		float result = Math.min(this.healpoints, heal);
		this.healpoints -= result;
		this.sync();
		return result;
	}

	public void setUnlockVonSprint(long tickTime, boolean unlock) {
		if (this.sprintUnlock != unlock) {
			if (this.sprintUnlock = unlock) {
				this.sprintTimeTarget = tickTime + 600; // 20ticks * 30seconds
			}

			this.sync();
		}
	}

	public void attackedAt(long tickTime) {
		this.attackTimeTarget = tickTime + 600;
		this.sync();
	}

	public void resetHealPoints() {
		this.healpoints = 8.0f;
		this.sync();
	}

	// Overrides

	@Override
	public void fromTag(CompoundTag tag) {
		try {
			this.healpoints = tag.getFloat("healpoints");
			this.sprintUnlock = tag.getBoolean("sprintUnlock");
			this.sprintTimeTarget = tag.getLong("sprintTimeTarget");
			this.attackTimeTarget = tag.getLong("attackTimeTarget");
		} catch (Exception e) {
			System.err.println("The following error may or may not be a problem. If this occurs updating a world to newer versions of the mod, it's probably fine.");
			e.printStackTrace();
		}
	}

	@Override
	public CompoundTag toTag(CompoundTag tag) {
		tag.putFloat("healpoints", this.healpoints);
		tag.putBoolean("sprintUnlock", this.sprintUnlock);
		tag.putLong("attackTimeTarget", this.attackTimeTarget);
		tag.putLong("sprintTimeTarget", this.sprintTimeTarget);
		return tag;
	}

	@Override
	public Entity getEntity() {
		return this.parent;
	}
}
