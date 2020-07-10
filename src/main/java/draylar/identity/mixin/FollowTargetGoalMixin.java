package draylar.identity.mixin;

import draylar.identity.Identity;
import draylar.identity.registry.Components;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.FollowTargetGoal;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FollowTargetGoal.class)
public abstract class FollowTargetGoalMixin extends TrackTargetGoal {

    @Shadow protected LivingEntity targetEntity;

    public FollowTargetGoalMixin(MobEntity mob, boolean checkVisibility) {
        super(mob, checkVisibility);
    }

    @Inject(
            method = "start",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ignoreMorphedPlayers(CallbackInfo ci) {
        if (Identity.CONFIG.hostilesIgnoreHostileIdentityPlayer && this.mob instanceof HostileEntity && this.targetEntity instanceof PlayerEntity) {
            PlayerEntity targetPlayer = (PlayerEntity) this.targetEntity;
            LivingEntity identity = Components.CURRENT_IDENTITY.get(targetPlayer).getIdentity();
            boolean hasHostility = Components.HOSTILITY.get(targetPlayer).hasHostility();

            // only cancel if the player does not have hostility
            if(!hasHostility) {
                // withers should ignore undead
                if (this.mob instanceof WitherEntity && identity.getGroup().equals(EntityGroup.UNDEAD)) {
                    super.stop();
                    ci.cancel();
                }

                // hostile mobs (besides wither) should not target players morphed as hostile mobs
                else if (!(this.mob instanceof WitherEntity) && identity instanceof HostileEntity) {
                    super.stop();
                    ci.cancel();
                }
            }
        }
    }

    @Override
    public boolean shouldContinue() {
        // check cancelling for hostiles
        if(Identity.CONFIG.hostilesIgnoreHostileIdentityPlayer && Identity.CONFIG.hostilesForgetNewHostileIdentityPlayer && this.mob instanceof HostileEntity && this.targetEntity instanceof PlayerEntity) {
            PlayerEntity targetPlayer = (PlayerEntity) this.targetEntity;
            LivingEntity identity = Components.CURRENT_IDENTITY.get(targetPlayer).getIdentity();
            boolean hasHostility = Components.HOSTILITY.get(targetPlayer).hasHostility();

            // only cancel if the player does not have hostility
            if(!hasHostility) {
                // withers should ignore undead
                if (this.mob instanceof WitherEntity && identity.getGroup().equals(EntityGroup.UNDEAD)) {
                    return false;
                }

                // hostile mobs (besides wither) should not target players morphed as hostile mobs
                else if (!(this.mob instanceof WitherEntity) && identity instanceof HostileEntity) {
                    return false;
                }
            }
        }

        return super.shouldContinue();
    }
}
