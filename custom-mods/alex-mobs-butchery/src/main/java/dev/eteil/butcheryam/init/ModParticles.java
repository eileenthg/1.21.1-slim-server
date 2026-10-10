package dev.eteil.butcheryam.init;

import dev.eteil.butcheryam.ButcheryAlexsMobs;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> REGISTRY =
            DeferredRegister.create(Registries.PARTICLE_TYPE, ButcheryAlexsMobs.MODID);

    // Anonymous subclasses: SimpleParticleType's constructor is protected.
    public static final Supplier<SimpleParticleType> FEATHERS =
            REGISTRY.register("feathers", () -> new SimpleParticleType(false) {});
    public static final Supplier<SimpleParticleType> BLOOD =
            REGISTRY.register("bloodparticle", () -> new SimpleParticleType(false) {});

    private ModParticles() {}
}
