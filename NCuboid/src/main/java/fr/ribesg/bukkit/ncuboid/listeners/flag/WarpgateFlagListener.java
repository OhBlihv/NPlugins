package fr.ribesg.bukkit.ncuboid.listeners.flag;

import fr.ribesg.bukkit.ncuboid.NCuboid;
import fr.ribesg.bukkit.ncuboid.beans.Flag;
import fr.ribesg.bukkit.ncuboid.beans.FlagAtt;
import fr.ribesg.bukkit.ncuboid.events.extensions.ExtendedPlayerMoveEvent;
import fr.ribesg.bukkit.ncuboid.listeners.AbstractListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;

public class WarpgateFlagListener extends AbstractListener {

	public WarpgateFlagListener(final NCuboid instance) {
		super(instance);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerMoveBlock(final ExtendedPlayerMoveEvent ext) {
		final PlayerMoveEvent event = (PlayerMoveEvent) ext.getBaseEvent();
		if (!ext.isCustomCancelled()) {
			if (ext.getToCuboid() != null && ext.getToCuboid().getFlag(Flag.WARPGATE)) {
				event.getPlayer().teleport(ext.getToCuboid().getLocFlagAtt(FlagAtt.EXTERNAL_POINT));
				event.setCancelled(true);
			}
		}
	}
}