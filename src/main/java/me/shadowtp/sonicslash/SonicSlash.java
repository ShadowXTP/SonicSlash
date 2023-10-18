
package me.shadowtp.sonicslash;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.DamageHandler;
import me.simplicitee.project.addons.util.SoundAbility;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.bukkit.Sound.ENTITY_IRON_GOLEM_STEP;
import static org.bukkit.Sound.ENTITY_SHULKER_BULLET_HURT;

public class SonicSlash extends SoundAbility implements AddonAbility {
    public boolean sneaking;
    private SonicSlashListener listener;
    private Permission perm;

    private final double DAMAGE = ConfigManager.getConfig().getDouble("ExtraAbilities.ShadowTP.SonicSlash.Damage", 3);
    private final double TRACKERDAMAGE = ConfigManager.getConfig().getDouble("ExtraAbilities.ShadowTP.SonicSlash.TrackerDamage", 1);

    private double distanceTravelled;
    private Set<Entity> hurt;

    private final long LIFETIME = ConfigManager.getConfig().getLong("ExtraAbilities.ShadowTP.SonicSlash.Duration"); // Lifetime of the ability
    private long startTime;
    private boolean trackingMode = false;
    private Location location;
    private Vector direction;

    private double trackingrange = ConfigManager.getConfig().getDouble("ExtraAbilities.ShadowTP.SonicSlash.TrackingRange");

    public long sneakStartTime;
    private static final long REQUIRED_SNEAK_DURATION = 1000; // 1000 milliseconds (1 second)

    private long cooldown;

    public SonicSlash(Player player) {
        super(player);
        location = player.getEyeLocation();
        direction = player.getLocation().getDirection();
        direction.multiply(0.8);

        cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.ShadowTP.SonicSlash.Cooldown");
        bPlayer.addCooldown(this);
        distanceTravelled = 0;
        hurt = new HashSet<>();
        startTime = System.currentTimeMillis(); // Initialize the start time
        start();



    }

    @Override
    public void progress() {
        if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
            remove();
            return;
        }


        if (location.getBlock().getType().isSolid() || System.currentTimeMillis() - startTime > LIFETIME) {
            remove();
            return;
        }

        if (trackingMode) {
            Entity nearestEntity = getNearestEntity();
            if (nearestEntity != null && !hurt.contains(nearestEntity)) {
                Vector entityDirection = nearestEntity.getLocation().toVector().subtract(location.toVector()).normalize();
                direction = entityDirection.multiply(0.8);
            }
            location.add(direction);
            spawnSonicExplosionParticles(location);
            if (ThreadLocalRandom.current().nextInt(3) == 0){
                playEchoSound(location);
            }
        } else {

            location.add(direction);
            spawnAir(location);
            spawnSlash(location);
        }

        affectTargets();

        if (ThreadLocalRandom.current().nextInt(6) == 0) {
            playAirbendingSound(location);
        }

        distanceTravelled += direction.length();
        //remove();
    }

    private void spawnSonicExplosionParticles(Location loc) {
        loc.getWorld().spawnParticle(Particle.valueOf("SONIC_BOOM"), loc, 1, 0.3, 0.2, 0.2, 0, null);
    }

    private void spawnAir(Location loc) {
        playAirbendingParticles(loc, 2, 0.3, 0.2, 0.2);
    }

    private void spawnSlash(Location loc) {
        loc.getWorld().spawnParticle(Particle.valueOf("SWEEP_ATTACK"), loc, 1, 0.3, 0.2, 0.2, 0, null);
    }

    public void playEchoSound(Location loc) {
        player.getWorld().playSound(loc, ENTITY_IRON_GOLEM_STEP,  2 , 0);
        player.getWorld().playSound(loc, ENTITY_SHULKER_BULLET_HURT, 2, 1.6f);
    }


    public void affectTargets() {
        List<Entity> targets = GeneralMethods.getEntitiesAroundPoint(location, 1.5);
        for (Entity target : targets) {
            if (target.getUniqueId() == player.getUniqueId()) {
                continue;
            }

            target.setFireTicks(0);
            if (!hurt.contains(target) && trackingMode == false) {
                DamageHandler.damageEntity(target, DAMAGE, this);
                hurt.add(target);
                target.setVelocity(direction);
            }else if (!hurt.contains(target) && trackingMode == true) {
                DamageHandler.damageEntity(target, TRACKERDAMAGE, this);
                hurt.add(target);
                target.setVelocity(direction);
            }
        }
    }

    public void setTrackingMode(boolean trackingMode) {
        this.trackingMode = trackingMode;
    }

    public Entity getNearestEntity() {
        List<Entity> nearbyEntities = location.getWorld().getEntities();
        Entity nearestEntity = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity && entity != player) {
                double distance = location.distance(entity.getLocation());

                // Check if the distance is within the specified range (10 blocks).
                if (distance <= trackingrange && distance < nearestDistance) {
                    nearestEntity = entity;
                    nearestDistance = distance;
                }
            }
        }

        return nearestEntity;
    }


    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public void remove() {
        super.remove();
        hurt.clear();
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public void load() {
        listener = new SonicSlashListener();
        ProjectKorra.plugin.getServer().getPluginManager().registerEvents(listener, ProjectKorra.plugin);
        perm = new Permission("bending.ability.SonicSlash");
        perm.setDefault(PermissionDefault.OP);
        ProjectKorra.plugin.getServer().getPluginManager().addPermission(perm);
        System.out.println("SoncSlash Addon loaded");

        ConfigManager.getConfig().addDefault("ExtraAbilities.ShadowTP.SonicSlash.Cooldown", 3000);
        ConfigManager.getConfig().addDefault("ExtraAbilities.ShadowTP.SonicSlash.Duration", 1000);
        ConfigManager.getConfig().addDefault("ExtraAbilities.ShadowTP.SonicSlash.Damage", 2);
        ConfigManager.getConfig().addDefault("ExtraAbilities.ShadowTP.SonicSlash.TrackingRange", 15);
        ConfigManager.getConfig().addDefault("ExtraAbilities.ShadowTP.SonicSlash.TrackerDamage", 1);

        ConfigManager.defaultConfig.save();

    }

    @Override
    public void stop() {
        HandlerList.unregisterAll(listener);
        ProjectKorra.plugin.getServer().getPluginManager().removePermission(perm);
    }

    @Override
    public String getName() {
        return "SonicSlash";
    }

    @Override
    public String getAuthor() {
        return "ShadowTP";
    }

    @Override
    public String getDescription() {
        return "A homing current of sound, tapping shift turns the blast into a homing attack that does less damage";
    }

    @Override
    public String getVersion(){
        return "1.3";
    }

    @Override
    public String getInstructions(){
        return "Left Click to fire a slash, Tap Sneak to home in on the closest entity";
    }
}
