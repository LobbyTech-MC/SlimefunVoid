package software.bigbade.slimefunvoid.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.cscorelib2.inventory.ChestMenu;
import me.mrCookieSlime.Slimefun.cscorelib2.item.CustomItem;
import software.bigbade.slimefunvoid.SlimefunVoid;
import software.bigbade.slimefunvoid.items.Items;
import software.bigbade.slimefunvoid.menus.VoidMenu;
import software.bigbade.slimefunvoid.menus.ritals.RitualMenu;
import software.bigbade.slimefunvoid.utils.RecipeItems;

public class VoidPortal extends SlimefunItem {
    private ChestMenu menu = new ChestMenu(SlimefunVoid.getInstance(), "&5Void Portal");

    private VoidMenu voidMenu = new VoidMenu();

    private RitualMenu ritualMenu = new RitualMenu();

    @Getter
    private List<VoidPortalData> altars = new ArrayList<>();

    protected static final int[] X_OFFSETS = new int[]{-2, 0, 2, 3, 2, 0, -2, -3};
    protected static final int[] Z_OFFSETS = new int[]{2, 3, 2, 0, -2, -3, -2, 0};

    public VoidPortal(Category category) {
        super(category, Items.VOID_PORTAL, RecipeType.ANCIENT_ALTAR, new ItemStack[]{
                Items.VOID_ALTAR, RecipeItems.END_CRYSTAL, Items.VOID_ALTAR,
                RecipeItems.ENDER_EYE, Items.VOID_RESEARCH_BENCH, RecipeItems.ENDER_EYE,
                RecipeItems.OBSIDIAN, Items.VOID_ALTAR, RecipeItems.OBSIDIAN});

        initMenu();

        addItemHandler(new BlockPlaceHandler(true) {
			
			@Override
			public void onPlayerPlace(BlockPlaceEvent e) {
		        BlockStorage.addBlockInfo(e.getBlock(), "owner", e.getPlayer().getUniqueId().toString());
		        altars.add(new VoidPortalData(e.getBlock().getLocation()));
			}
		});

        BlockUseHandler blockUseHandler = this::onBlockUse;
        addItemHandler(blockUseHandler);

        addItemHandler(new BlockBreakHandler(false, false) {
			
			@Override
			public void onPlayerBreak(BlockBreakEvent e, ItemStack item, List<ItemStack> drops) {
		        VoidPortalData data = getPortalData(e.getBlock().getLocation());
		        if (data == null) {
		            return;
		        }
		        if (data.getAltars().size() > 0) {
		            e.getPlayer().sendMessage(ChatColor.RED + "You must break all altars before you break the portal.");
		            e.setCancelled(true);
		        }
				
			}
		});
    }

    private void initMenu() {
        for (int i = 0; i < 27; i++) {
            if (i > 11 && i < 15 && i % 2 == 0)
                continue;
            menu.addItem(i, VoidMenu.PURPLE_GLASS, (player, slot, item, cursor, action) -> false);
        }

        menu.addItem(12, new CustomItem(Material.END_PORTAL_FRAME, ChatColor.GREEN + "Enter The Void"), (player, slot, item, cursor, action) -> {
            PersistentDataContainer data = player.getPersistentDataContainer();
            if (data.has(VoidMenu.VOID_COOLDOWN, PersistentDataType.LONG)) {
                Long start = data.get(VoidMenu.VOID_COOLDOWN, PersistentDataType.LONG);
                Objects.requireNonNull(start);
                long cooldown = getCooldownTime(start, 300);
                if (cooldown < 0)
                    voidMenu.open(player);
                else
                    player.sendMessage(ChatColor.RED + "You cannot enter the void for " + cooldown + " more seconds!");
                return false;
            }
            voidMenu.open(player);
            return false;
        });

        menu.addItem(14, new CustomItem(Material.STICK, ChatColor.GREEN + "Conduct a Ritual"), (player, slot, item, cursor, action) -> {
            Block altar = player.getTargetBlockExact(7, FluidCollisionMode.NEVER);
            if (altar == null || !altar.getType().equals(Material.END_PORTAL_FRAME)) {
                return false;
            }
            ritualMenu.open(getPortalData(altar.getLocation()), player);
            return false;
        });

        menu.addMenuOpeningHandler(this::onMenuOpen);
    }

    private void onMenuOpen(@Nonnull Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        if (data.has(VoidMenu.VOID_COOLDOWN, PersistentDataType.LONG)) {
            Long start = data.get(VoidMenu.VOID_COOLDOWN, PersistentDataType.LONG);
            Objects.requireNonNull(start);
            long cooldown = getCooldownTime(start, 300);
            if (cooldown > 0) {
                menu.replaceExistingItem(12, new CustomItem(Material.END_PORTAL_FRAME, ChatColor.GREEN + "Enter The Void " + ChatColor.WHITE + "(" + cooldown + "s)"));
            }
        }
    }

    public static long getCooldownTime(long start, long cooldown) {
        return cooldown - (System.currentTimeMillis() - start) / 1000;
    }

    private void onBlockUse(PlayerRightClickEvent event) {
        Block block = VoidAltar.prepareMenuOpen(event);
        if (block == null)
            return;
        String owner = BlockStorage.getLocationInfo(block.getLocation(), "owner");
        if (getPortalData(block.getLocation()) == null)
            altars.add(new VoidPortalData(block.getLocation()));
        if (owner.equals(event.getPlayer().getUniqueId().toString())) {
            menu.open(event.getPlayer());
        }
    }

    public static void checkAltars(VoidPortalData data) {
        for (int i = 0; i < X_OFFSETS.length; i++) {
            Location newLocation = data.getPortal().clone().add(X_OFFSETS[i], 0, Z_OFFSETS[i]);
            Block block = newLocation.getBlock();
            if (block.getType().equals(Material.END_STONE_BRICKS) && BlockStorage.check(newLocation, Items.VOID_ALTAR.getItemId()) && !data.getAltars().contains(newLocation)) {
                data.getAltars().add(newLocation);
            }
        }
    }

    @Nullable
    public VoidPortalData getPortalData(Location location) {
        for (VoidPortalData altar : altars) {
            if (altar.getPortal().equals(location))
                return altar;
        }
        return null;
    }

    @RequiredArgsConstructor
    public static class VoidPortalData {
        @Getter
        @Setter
        private boolean inUse = false;

        @Getter
        private List<Location> altars = new ArrayList<>();

        @Getter
        private final Location portal;

		public VoidPortalData(@Nonnull Location location) {
			this.portal = location;
		}

		public Location getPortal() {
			return portal;
		}

		public List<Location> getAltars() {
			return altars;
		}

		public boolean isInUse() {
			return inUse;
		}
    }

	public List<VoidPortalData> getAltars() {
		// TODO Auto-generated method stub
		return altars;
	}
}
