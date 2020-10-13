package valoeghese.kingdomcurses;

import net.minecraft.server.world.ServerWorld;
import valoeghese.kingdomcurses.kingdom.Kingdom;

public enum Curse {
	/**
	 * - *Graveyards with even more stronger undead (grave block)
	 * - More undead spawn (inject to spawn rate and entity spawn) & undead are stronger
	 * - *Perform a holy ritual to dispel
	 */
	NECROMANCY,
	/**
	 * - *Sorcerer's castle
	 * - *Powerful boss monsters every now and then with magic weapons & attacks
	 * - *Kill the sorcerer to dispel
	 */
	SORCERER,
	/**
	 * - *After feature generation, the world gets some decay. Cobblestone -> mossy. Building blocks -> less. Trees -> rotten.
	 * - *Slow decay somehow - mobs spawning weaker, zombies spreading poison.
	 * - *Cast regeneration nature spell to dispel
	 */
	DECAY;
	
	public static Curse getCurse(ServerWorld world, Kingdom kingdom) {
		switch (kingdom.getCityCentre().hashCode() & 0b111) {
		case 0:
		case 1:
		case 2:
			return Curse.NECROMANCY;
		case 3:
		case 4:
			return Curse.SORCERER;
		case 5:
		case 6:
		case 7:
		default:
			return Curse.DECAY;
		}
	}
}
