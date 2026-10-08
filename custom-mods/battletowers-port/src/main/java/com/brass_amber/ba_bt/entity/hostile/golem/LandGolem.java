package com.brass_amber.ba_bt.entity.hostile.golem;

import com.brass_amber.ba_bt.entity.ai.goal.LandGolemFireballAttackGoal;
import com.brass_amber.ba_bt.entity.ai.goal.GolemStompAttackGoal;


import com.brass_amber.ba_bt.util.GolemType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;

import javax.annotation.Nullable;

import static com.brass_amber.ba_bt.BattleTowersConfig.landGolemHP;
import static com.brass_amber.ba_bt.sound.BTMusic.LAND_GOLEM_FIGHT_MUSIC;

public class LandGolem extends AbstractGolem {

	protected static final EntityDataAccessor<Boolean> DATA_IS_CHARGING = SynchedEntityData.defineId(LandGolem.class, EntityDataSerializers.BOOLEAN);

	public LandGolem(EntityType<? extends LandGolem> type, Level levelIn) {
		super(type, levelIn, BossEvent.BossBarColor.BLUE);
		this.setGolemName(GolemType.LAND.getDisplayName());
		this.setBossBarName();

		// Sets the experience points to drop. Reference taken from the EnderDragon.
		this.xpReward = 315;
		this.golemType = GolemType.LAND;

		this.BOSS_MUSIC = LAND_GOLEM_FIGHT_MUSIC;

		// Reference for disregarding lava taken from ZombiefiedPiglin
		this.setPathfindingMalus(PathType.LAVA, 8.0F);

		// Reference for disregarding fire taken from Blaze
		this.setPathfindingMalus(PathType.DANGER_FIRE, 0.0F);
		this.setPathfindingMalus(PathType.DAMAGE_FIRE, 0.0F);

		this.setPathfindingMalus(PathType.WATER, 0.0F);
	}

	public static AttributeSupplier.Builder createBattleGolemAttributes() {
		return AbstractGolem.createBattleGolemAttributes().add(Attributes.MAX_HEALTH, 200).add(Attributes.MOVEMENT_SPEED, 0.3D).add(Attributes.KNOCKBACK_RESISTANCE, 2.0D).add(Attributes.ATTACK_DAMAGE, 10.0D).add(Attributes.FOLLOW_RANGE, 60.0D).add(Attributes.ARMOR, 4);
	}

	@Override
	protected void addBehaviorGoals() {
		this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.2D, true) {
			@Override
			public boolean canUse() {
				return !LandGolem.this.isDormant() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
//				BrassAmberBattleTowers.LOGGER.debug("Melee canContinueToUse():" +getTarget());
				return !LandGolem.this.isDormant() && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(1, new GolemStompAttackGoal(this, 4.0F, 6));
		this.goalSelector.addGoal(6, new LandGolemFireballAttackGoal(this));
	}

	public void setCharging(boolean setCharging) {
		this.entityData.set(DATA_IS_CHARGING, setCharging);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_IS_CHARGING, false);
	}
}