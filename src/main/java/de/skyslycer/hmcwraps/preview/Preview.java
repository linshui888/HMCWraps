package de.skyslycer.hmcwraps.preview;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import de.skyslycer.hmcwraps.HMCWraps;
import de.skyslycer.hmcwraps.Point;
import de.skyslycer.hmcwraps.util.VectorUtils;
import dev.triumphteam.gui.guis.PaginatedGui;
import io.github.retrooper.packetevents.utils.SpigotDataHelper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

public class Preview {

    private static final double POINTS_AMOUNT = 720d;
    private static final double RADIUS = 2d;

    private final int entityId = Integer.MAX_VALUE - HMCWraps.RANDOM.nextInt(10000);
    private final Player player;
    private final ItemStack item;
    private final PaginatedGui gui;
    private final Set<Point<Double>> locations = new LinkedHashSet<>();
    private BukkitTask task;

    public Preview(Player player, ItemStack item, PaginatedGui gui) {
        this.player = player;
        this.item = item;
        this.gui = gui;
    }

    public void preview(HMCWraps plugin) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            generateCircleLocations();
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerSpawnLivingEntity(
                    entityId,
                    UUID.randomUUID(),
                    EntityTypes.ARMOR_STAND,
                    VectorUtils.fromLocation(player.getLocation()),
                    0f,
                    0f,
                    0f,
                    VectorUtils.zeroVector(),
                    List.of(new EntityData(19, EntityDataTypes.ROTATION, new Vector3f(-90, 0, 0)),
                            new EntityData(5, EntityDataTypes.BOOLEAN, true),
                            new EntityData(0, EntityDataTypes.BYTE, (byte) 32)))
            );
            PacketEvents.getAPI().getPlayerManager()
                    .sendPacket(player, new WrapperPlayServerEntityEquipment(entityId, new Equipment(
                            EquipmentSlot.MAINHAND, SpigotDataHelper.fromBukkitItemStack(item))));

            player.getOpenInventory().close();

            task = Bukkit.getScheduler()
                    .runTaskTimerAsynchronously(plugin, new RotateRunnable(player, entityId, locations), 0, 1);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                task.cancel();
                PacketEvents.getAPI().getPlayerManager()
                        .sendPacket(player, new WrapperPlayServerDestroyEntities(entityId));
                gui.open(player);
            }, 20 * 2);
        });
    }

    private void generateCircleLocations() {
        for (int i = 0; i < POINTS_AMOUNT; i++) {
            double angle = Math.toRadians(((double) i / POINTS_AMOUNT) * 360d);
            locations.add(Point.build(Math.cos(angle) * RADIUS, Math.sin(angle) * RADIUS));
        }
    }

}