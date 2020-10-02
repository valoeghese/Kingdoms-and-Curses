package valoeghese.kingdomcurses;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;

public class KingdomsAndCurses implements ModInitializer {
	@Override
	public void onInitialize() {
		// TODO
		// - Kingdoms (based off of 2fc)
		// - Curses
		// - Long/Short rest system
	}

	public static void longRest(World world) {
		world.getPlayers().stream().filter(LivingEntity::isSleeping).forEach(pe -> {
			if (pe.getHealth() < pe.getMaxHealth() && pe.getHungerManager().getFoodLevel() > 0) {
				pe.heal(pe.getMaxHealth() / 2);
				pe.addExhaustion(20.0f);
			}
		});
	}

	public static final Logger LOGGER = LogManager.getLogger("Kingdom Curses");
}
