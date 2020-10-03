package valoeghese.kingdomcurses;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nerdhub.cardinal.components.api.ComponentRegistry;
import nerdhub.cardinal.components.api.ComponentType;
import nerdhub.cardinal.components.api.event.EntityComponentCallback;
import nerdhub.cardinal.components.api.util.EntityComponents;
import nerdhub.cardinal.components.api.util.RespawnCopyStrategy;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

public class KingdomsAndCurses implements ModInitializer {
	@Override
	public void onInitialize() {
		EntityComponents.setRespawnCopyStrategy(PLAYER_STATS, RespawnCopyStrategy.LOSSLESS_ONLY);
		EntityComponentCallback.event(PlayerEntity.class).register((player, components) -> components.put(PLAYER_STATS, new PlayerStats(player)));

		// TODO use custom callbacks that run afterwards, only on success
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			stats(player).attackedAt(world.getTime());
			return ActionResult.PASS;
		});

		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			stats(player).attackedAt(world.getTime());
			return ActionResult.PASS;
		});

		// TODO
		// - Kingdoms (based off of 2fc)
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

	public static final ComponentType<PlayerStats> PLAYER_STATS = ComponentRegistry.INSTANCE.registerIfAbsent(new Identifier("kingdom_curses", "player_stats"), PlayerStats.class);
	public static final Logger LOGGER = LogManager.getLogger("Kingdoms and Curses");
}
