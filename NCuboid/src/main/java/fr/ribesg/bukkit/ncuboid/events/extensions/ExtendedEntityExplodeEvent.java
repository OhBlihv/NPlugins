package fr.ribesg.bukkit.ncuboid.events.extensions;

import fr.ribesg.bukkit.ncuboid.beans.RegionDb;
import fr.ribesg.bukkit.ncuboid.beans.GeneralRegion;
import fr.ribesg.bukkit.ncuboid.events.AbstractExtendedEvent;
import org.bukkit.block.Block;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.HashMap;
import java.util.Map;

public class ExtendedEntityExplodeEvent extends AbstractExtendedEvent {

	private final GeneralRegion             entityRegion;
	private final Map<Block, GeneralRegion> blockRegionsMap;

	public ExtendedEntityExplodeEvent(final RegionDb db, final EntityExplodeEvent event) {
		super(event);
		blockRegionsMap = new HashMap<>();
		for (final Block b : event.blockList()) {
			final GeneralRegion cuboid = db.getPriorByLocation(b.getLocation());
			if (cuboid != null) {
				blockRegionsMap.put(b, cuboid);
			}
		}
		entityRegion = blockRegionsMap.get(event.getLocation().getBlock());
	}

	public Map<Block, GeneralRegion> getBlockRegionsMap() {
		return blockRegionsMap;
	}

	public GeneralRegion getEntityRegion() {
		return entityRegion;
	}
}
