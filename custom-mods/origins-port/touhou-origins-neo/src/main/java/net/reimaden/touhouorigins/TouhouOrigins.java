package net.reimaden.touhouorigins;

import dev.overgrown.apoli.action.ActionTypes;
import dev.overgrown.apoli.condition.ConditionTypes;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.reimaden.touhouorigins.action.ChangeResourceWithStatusEffectsAction;
import net.reimaden.touhouorigins.action.StoreEnchantmentLevelAction;
import net.reimaden.touhouorigins.action.StoreOriginNameAction;
import net.reimaden.touhouorigins.action.TeleportToEntityAction;
import net.reimaden.touhouorigins.condition.CraftableItemCondition;
import net.reimaden.touhouorigins.power.ActionOnBlockPower;
import net.reimaden.touhouorigins.power.ConvertEntityPower;
import net.reimaden.touhouorigins.power.EnchantmentVulnerabilityPower;
import net.reimaden.touhouorigins.power.FlipGravityPower;
import net.reimaden.touhouorigins.power.FlipModelPower;
import net.reimaden.touhouorigins.power.ActionOnOriginGuiPower;
import net.reimaden.touhouorigins.event.BlessingsHandler;
import net.reimaden.touhouorigins.event.ShikigamiDeathHandler;
import net.reimaden.touhouorigins.action.RememberMasterAction;
import net.reimaden.touhouorigins.shikigami.KilledTargetCondition;
import net.reimaden.touhouorigins.shikigami.PactRelationCondition;
import net.reimaden.touhouorigins.shikigami.ShikigamiCommand;
import net.reimaden.touhouorigins.shikigami.ShikigamiPact;
import net.reimaden.touhouorigins.shikigami.ShikigamiPactHandler;
import net.reimaden.touhouorigins.power.ActionOnEntityDeathPower;
import net.reimaden.touhouorigins.condition.ComponentItemConditions;
import net.reimaden.touhouorigins.condition.NamedItemCondition;
import net.reimaden.touhouorigins.sound.ModSoundEvents;
import net.reimaden.touhouorigins.power.ModifyDivergencePower;
import net.reimaden.touhouorigins.power.ModifyPickupRangePower;
import net.neoforged.neoforge.common.NeoForge;
import net.reimaden.touhouorigins.behavior.ConvertEntityHandler;
import net.reimaden.touhouorigins.behavior.HostileTargetingHandler;
import net.reimaden.touhouorigins.power.ModifyBehaviorPower;
import net.reimaden.touhouorigins.power.ModifyFootstepSoundPower;
import net.reimaden.touhouorigins.power.PreventItemSlowdownPower;
import net.reimaden.touhouorigins.power.PreventTamingPower;
import net.reimaden.touhouorigins.power.SimplePowers;
import net.reimaden.touhouorigins.power.ModifyScalePower;
import net.reimaden.touhouorigins.scale.RelativeScaleModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeoForge entrypoint, replacing the Fabric {@code ModInitializer} pair.
 */
@Mod(TouhouOrigins.MOD_ID)
public final class TouhouOrigins {

    public static final String MOD_ID = "touhouorigins";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public TouhouOrigins(IEventBus modBus, ModContainer container) {
        // Apoli registers power types directly rather than through a DeferredRegister.
        PowerTypeRegistry.register(id("modify_scale"), new ModifyScalePower());
        PowerTypeRegistry.register(id("action_on_origin_gui"), new ActionOnOriginGuiPower());
        PowerTypeRegistry.register(id("modify_behavior"), new ModifyBehaviorPower());
        PowerTypeRegistry.register(id("modify_footstep_sound"), new ModifyFootstepSoundPower());
        PowerTypeRegistry.register(SimplePowers.PREVENT_SPRINTING_PARTICLES, new SimplePowers.FlagPower());
        PowerTypeRegistry.register(SimplePowers.DISABLE_SHIELDS, new SimplePowers.FlagPower());
        PowerTypeRegistry.register(SimplePowers.PREVENT_FLYING_KICK, new SimplePowers.FlagPower());
        PowerTypeRegistry.register(SimplePowers.REVERSE_INSTANT_EFFECTS, new SimplePowers.FlagPower());
        PowerTypeRegistry.register(PreventItemSlowdownPower.CANONICAL, new PreventItemSlowdownPower());
        PowerTypeRegistry.register(PreventTamingPower.CANONICAL, new PreventTamingPower());
        PowerTypeRegistry.register(ActionOnBlockPower.CANONICAL, new ActionOnBlockPower());
        PowerTypeRegistry.register(ConvertEntityPower.CANONICAL, new ConvertEntityPower());
        PowerTypeRegistry.register(FlipModelPower.CANONICAL, new FlipModelPower());
        PowerTypeRegistry.register(SimplePowers.ZOMBIE_ARMS, new SimplePowers.FlagPower());
        PowerTypeRegistry.register(ModifyDivergencePower.CANONICAL, new ModifyDivergencePower());
        PowerTypeRegistry.register(ModifyPickupRangePower.CANONICAL, new ModifyPickupRangePower());
        PowerTypeRegistry.register(ActionOnEntityDeathPower.CANONICAL, new ActionOnEntityDeathPower());
        PowerTypeRegistry.register(FlipGravityPower.SELF_ID, new FlipGravityPower());
        PowerTypeRegistry.register(FlipGravityPower.TURNABOUT_ID, new FlipGravityPower());

        PowerTypeRegistry.register(EnchantmentVulnerabilityPower.CANONICAL, new EnchantmentVulnerabilityPower());

        ConditionTypes.ITEM.register(id("craftable"), new CraftableItemCondition());
        ConditionTypes.ITEM.register(NamedItemCondition.CANONICAL, new NamedItemCondition());
        ConditionTypes.ITEM.register(ComponentItemConditions.PotionCondition.CANONICAL,
            new ComponentItemConditions.PotionCondition());
        ConditionTypes.ITEM.register(ComponentItemConditions.ChargedCondition.CANONICAL,
            new ComponentItemConditions.ChargedCondition());
        ActionTypes.BI_ENTITY.register(RememberMasterAction.CANONICAL, new RememberMasterAction());
        ConditionTypes.BI_ENTITY.register(PactRelationCondition.MASTER, new PactRelationCondition(true));
        ConditionTypes.BI_ENTITY.register(PactRelationCondition.SERVANT, new PactRelationCondition(false));
        ConditionTypes.BI_ENTITY.register(KilledTargetCondition.CANONICAL, new KilledTargetCondition());
        ActionTypes.ENTITY.register(StoreOriginNameAction.CANONICAL, new StoreOriginNameAction());
        ActionTypes.ENTITY.register(TeleportToEntityAction.CANONICAL, new TeleportToEntityAction());
        ActionTypes.ENTITY.register(StoreEnchantmentLevelAction.CANONICAL, new StoreEnchantmentLevelAction());
        ActionTypes.ENTITY.register(ChangeResourceWithStatusEffectsAction.CANONICAL,
            new ChangeResourceWithStatusEffectsAction());

        // Adds the hostile-targeting goal to every mob as it spawns; the goal itself is inert
        // unless a nearby player has modify_behavior set to hostile.
        NeoForge.EVENT_BUS.register(new HostileTargetingHandler());
        NeoForge.EVENT_BUS.register(new ConvertEntityHandler());
        NeoForge.EVENT_BUS.register(new BlessingsHandler());
        NeoForge.EVENT_BUS.register(new ShikigamiDeathHandler());
        NeoForge.EVENT_BUS.register(new ShikigamiPactHandler());
        NeoForge.EVENT_BUS.register(new ShikigamiCommand());

        // Pehkui syncs scale modifiers by id, so this must happen on both sides.
        // Must run before anything can play a sound: apoli:play_sound silently does nothing
        // when the id is not in the registry.
        ModSoundEvents.register(modBus);
        ShikigamiPact.register(modBus);

        RelativeScaleModifier.register();

        TouhouOriginsConfig.register(container);

        LOGGER.info("Touhou Origins (NeoForge port) loaded.");
    }
}
