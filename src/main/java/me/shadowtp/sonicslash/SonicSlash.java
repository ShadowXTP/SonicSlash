
package me.shadowtp.sonicslash;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.waterbending.blood.Bloodbending;
import me.simplicitee.project.addons.util.SoundAbility;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.bukkit.Sound.*;
import static org.bukkit.potion.PotionEffectType.*;

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

    private double radius;

    public double bspeed = ConfigManager.getConfig().getDouble("ExtraAbilities.ShadowTP.SonicSlash.Speed");
    public double cspeed = ConfigManager.getConfig().getDouble("ExtraAbilities.ShadowTP.SonicSlash.ControlledSpeed");

    public int lduration = ConfigManager.getConfig().getInt("ExtraAbilities.ShadowTP.SonicSlash.LevitationDuration");

    public int dduration = ConfigManager.getConfig().getInt("ExtraAbilities.ShadowTP.SonicSlash.DarknessDuration");


    public SonicSlash(Player player) {
        super(player);
        location = player.getEyeLocation();
        direction = player.getLocation().getDirection();
        direction.multiply(bspeed);

        cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.ShadowTP.SonicSlash.Cooldown");
        radius = ConfigManager.getConfig().getLong("ExtraAbilities.ShadowTP.SonicSlash.Radius");
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
            /*
            Entity nearestEntity = getNearestEntity();
            if (nearestEntity != null && !hurt.contains(nearestEntity)) {
                Vector entityDirection = nearestEntity.getLocation().toVector().subtract(location.toVector()).normalize();
                direction = entityDirection.multiply(0.8);
            }
            */
            direction = player.getEyeLocation().getDirection();
            direction.multiply(cspeed);
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
            loc.getWorld().spawnParticle(Particle.valueOf("GLOW"), loc, 3, 0.3, 0.2, 0.2, 0, null);
    }

    private void spawnAir(Location loc) {
        playAirbendingParticles(loc, 2, 0.3, 0.2, 0.2);
    }

    private void spawnSlash(Location loc) {
        loc.getWorld().spawnParticle(Particle.valueOf("SWEEP_ATTACK"), loc, 1, 0.3, 0.2, 0.2, 0, null);
    }

    public void playEchoSound(Location loc) {
            player.getWorld().playSound(loc, ENTITY_IRON_GOLEM_STEP,  4 , 1);
            player.getWorld().playSound(loc, BLOCK_AMETHYST_BLOCK_FALL, 2, 1.6f);
    }



    public void affectTargets() {
        //line below = hitbox keeth
        List<Entity> targets = GeneralMethods.getEntitiesAroundPoint(location, radius);
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
                LivingEntity livingEntity = (LivingEntity) target;
                livingEntity.addPotionEffect(new PotionEffect(DARKNESS, dduration, 25));
                livingEntity.addPotionEffect(new PotionEffect(LEVITATION, lduration, 1));
                livingEntity.addPotionEffect(new PotionEffect(LEVITATION, lduration, 1));
                DamageHandler.damageEntity(target, TRACKERDAMAGE, this);
                target.setVelocity(direction);
                //Player targetPlayer = (Player) target;
                hurt.add(target);
            }
        }
    }

    public void setTrackingMode(boolean trackingMode) {
        if (bPlayer.canBendIgnoreCooldowns(this)) {
            this.trackingMode = trackingMode;
        }
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

        ConfigManager.getConfig().addDefault("ExtraAbilities.ShadowTP.SonicSlash.Cooldown", 3000);
        ConfigManager.getConfig().addDefault("ExtraAbilities.ShadowTP.SonicSlash.Duration", 1000);
        ConfigManager.getConfig().addDefault("ExtraAbilities.ShadowTP.SonicSlash.Damage", 2);
        ConfigManager.getConfig().addDefault("ExtraAbilities.ShadowTP.SonicSlash.Radius", 1);
        ConfigManager.getConfig().addDefault("ExtraAbilities.ShadowTP.SonicSlash.Speed", 1.2);
        ConfigManager.getConfig().addDefault("ExtraAbilities.ShadowTP.SonicSlash.TrackingRange", 15);
        ConfigManager.getConfig().addDefault("ExtraAbilities.ShadowTP.SonicSlash.TrackerDamage", 0.01);
        ConfigManager.getConfig().addDefault("ExtraAbilities.ShadowTP.SonicSlash.ControlledSpeed", 0.8);
        ConfigManager.getConfig().addDefault("ExtraAbilities.ShadowTP.SonicSlash.LevitationDuration", 30);
        ConfigManager.getConfig().addDefault("ExtraAbilities.ShadowTP.SonicSlash.DarknessDuration", 80);
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
        return "A single slash of air capable of delivering devastating damage or disorientate enemies";
    }

    @Override
    public String getVersion(){
        return "1.5";
    }

    @Override
    public String getInstructions(){
        return "Left Click to fire a Slash, Hold Sneak to Disorientate and Control" ;

    }
}
