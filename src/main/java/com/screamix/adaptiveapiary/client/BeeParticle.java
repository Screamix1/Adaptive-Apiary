package com.screamix.adaptiveapiary.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.screamix.adaptiveapiary.genetics.BeeSpecies;
import com.screamix.adaptiveapiary.genetics.SpeciesRegistry;
import com.screamix.adaptiveapiary.particle.BeeParticleOptions;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Пчела, которая кружит вокруг улья: летит по неровной окружности, покачивается вверх-вниз,
 * машет крыльями (два кадра) и плавно появляется и исчезает.
 *
 * <p>Картинка та же, что у трутня в инвентаре, и красится так же: три слоя рисуются друг на друге -
 * тело во втором цвете вида, контур в основном цвете и неокрашенные голова, полосы и крылья.
 * Пчела всегда смотрит туда, куда летит.
 */
public class BeeParticle extends TextureSheetParticle {
    // порядок картинок в particles/bee.json
    private static final int OUTLINE_0 = 0;
    private static final int BODY = 1;
    private static final int EXTRA_0 = 2;
    private static final int OUTLINE_1 = 3;
    private static final int EXTRA_1 = 4;
    private static final int SPRITE_COUNT = 5;

    private final SpriteSet sprites;
    private final float[] outlineColor;
    private final float[] bodyColor;
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final double radius;
    private final double height;
    private final double speed;
    private double angle;
    /** Картинка смотрит влево; true - отражена, пчела летит вправо по экрану. */
    private boolean flipped;

    protected BeeParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, int outline, int body) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.outlineColor = rgb(outline);
        this.bodyColor = rgb(body);
        this.centerX = x;
        this.centerY = y;
        this.centerZ = z;
        this.radius = 0.7 + random.nextDouble() * 0.8;
        this.height = random.nextDouble() * 0.9 - 0.15;
        this.speed = (0.05 + random.nextDouble() * 0.07) * (random.nextBoolean() ? 1 : -1);
        this.angle = random.nextDouble() * Math.PI * 2;
        this.lifetime = 90 + random.nextInt(90);
        this.quadSize = 0.08F + random.nextFloat() * 0.03F;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.alpha = 0.0F;
        setSprite(sprite(BODY));
        place();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
    }

    private static float[] rgb(int color) {
        return new float[] {((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F};
    }

    private TextureAtlasSprite sprite(int index) {
        return sprites.get(index, SPRITE_COUNT - 1);
    }

    private void place() {
        double wobble = Math.sin(age * 0.35 + angle * 3.0) * 0.12;
        double r = radius + Math.sin(age * 0.11 + radius) * 0.18;
        setPos(centerX + Math.cos(angle) * r, centerY + height + wobble, centerZ + Math.sin(angle) * r);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        // иногда пчела резко меняет направление, как настоящая
        if (random.nextInt(60) == 0) {
            angle += (random.nextDouble() - 0.5) * 0.8;
        }
        angle += speed;
        place();
        alpha = Math.min(1.0F, Math.min(age / 8.0F, (lifetime - age) / 8.0F));
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        updateFacing(camera);
        boolean wingsUp = (age / 2) % 2 == 0;
        layer(buffer, camera, partialTicks, sprite(BODY), bodyColor);
        layer(buffer, camera, partialTicks, sprite(wingsUp ? OUTLINE_0 : OUTLINE_1), outlineColor);
        layer(buffer, camera, partialTicks, sprite(wingsUp ? EXTRA_0 : EXTRA_1), null);
    }

    private void layer(VertexConsumer buffer, Camera camera, float partialTicks, TextureAtlasSprite layerSprite, float[] color) {
        setSprite(layerSprite);
        if (color == null) {
            setColor(1.0F, 1.0F, 1.0F);
        } else {
            setColor(color[0], color[1], color[2]);
        }
        super.render(buffer, camera, partialTicks);
    }

    /** Куда пчела летит относительно экрана: проекция её движения на "влево" камеры. */
    private void updateFacing(Camera camera) {
        double yaw = Math.toRadians(camera.getYRot());
        double towardsLeft = (x - xo) * Math.cos(yaw) + (z - zo) * Math.sin(yaw);
        if (towardsLeft > 0.002) {
            flipped = false;
        } else if (towardsLeft < -0.002) {
            flipped = true;
        }
    }

    @Override
    protected float getU0() {
        return flipped ? super.getU1() : super.getU0();
    }

    @Override
    protected float getU1() {
        return flipped ? super.getU0() : super.getU1();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<BeeParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(BeeParticleOptions options, ClientLevel level, double x, double y, double z,
                double dx, double dy, double dz) {
            BeeSpecies species = SpeciesRegistry.get(options.species());
            return new BeeParticle(level, x, y, z, sprites, BeeColors.outline(species), BeeColors.body(species));
        }
    }
}
