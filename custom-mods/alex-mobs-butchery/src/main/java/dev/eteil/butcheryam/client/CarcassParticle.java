package dev.eteil.butcheryam.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Feather and blood particles, with the motion parameters of the originals. */
public class CarcassParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;

    private CarcassParticle(ClientLevel level, double x, double y, double z,
                            double vx, double vy, double vz, SpriteSet sprites,
                            float sizeScale, int lifetime, float gravity, double speed) {
        super(level, x, y, z);
        this.spriteSet = sprites;
        this.quadSize *= sizeScale;
        this.lifetime = lifetime;
        this.gravity = gravity;
        this.hasPhysics = true;
        this.xd = vx * speed;
        this.yd = vy * speed;
        this.zd = vz * speed;
        setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteFromAge(spriteSet);
    }

    /** Feathers drift outwards and settle. */
    public record FeathersProvider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            int lifetime = Math.max(1, 7 + level.random.nextInt(6) - 3);
            return new CarcassParticle(level, x, y, z, vx, vy, vz, sprites, 0.8F, lifetime, 0.3F, 0.2D);
        }
    }

    /** Blood is heavy and falls almost immediately. */
    public record BloodProvider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            return new CarcassParticle(level, x, y, z, vx, vy, vz, sprites, 0.2F, 7, 15.0F, 0.0D);
        }
    }
}
