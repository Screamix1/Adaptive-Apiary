package com.screamix.adaptiveapiary.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.screamix.adaptiveapiary.AdaptiveApiary;
import com.screamix.adaptiveapiary.menu.CentrifugeMenu;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CentrifugeScreen extends AbstractContainerScreen<CentrifugeMenu> {
    private static final ResourceLocation TEXTURE = AdaptiveApiary.id("textures/gui/centrifuge.png");

    private static final int ARROW_X = 62;
    private static final int ARROW_Y = 35;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 16;
    // чаша нарисована на фоне, крестовина 16x16 лежит в текстуре справа от панели
    private static final int ROTOR_X = 66;
    private static final int ROTOR_Y = 15;
    private static final int ROTOR_U = 176;
    private static final int ROTOR_V = 20;

    /** Скорость крестовины в работе, градусов в секунду. */
    private static final float ROTOR_SPEED = 420.0F;
    /** Обычное торможение, градусов в секунду за секунду (с 420 до нуля примерно за 0,7 секунды). */
    private static final float ROTOR_BRAKE = 600.0F;
    /** Между двумя сотами прогресс на тик падает в ноль: так долго не считаем это остановкой. */
    private static final long IDLE_GRACE_MS = 300;

    private float rotorAngle;
    private float rotorSpeed;
    /** Угол, на котором крестовина встанет; NaN, пока она не тормозит. */
    private float brakeTarget = Float.NaN;
    private long lastFrameMs = -1;
    private long lastWorkMs = Long.MIN_VALUE / 2;

    public CentrifugeScreen(CentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 176;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(ARROW_X, ARROW_Y, ARROW_WIDTH, ARROW_HEIGHT, mouseX, mouseY) && menu.progress() > 0) {
            graphics.renderTooltip(font, Component.translatable("gui.adaptiveapiary.centrifuge_progress", menu.progress()), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int progress = menu.progress() * ARROW_WIDTH / 100;
        if (progress > 0) {
            graphics.blit(TEXTURE, leftPos + ARROW_X, topPos + ARROW_Y, 176, 0, progress, ARROW_HEIGHT);
        }
        updateRotor(menu.progress() > 0);
        renderRotor(graphics);
    }

    /**
     * Угол крестовины считается по реальному времени кадра, а не по тикам, поэтому она крутится
     * ровно при любом FPS. Разгон и торможение плавные; останавливается крестовина всегда
     * ровно (на 0, 90, 180 или 270 градусов), чтобы в покое пиксели не стояли наискосок.
     */
    private void updateRotor(boolean working) {
        long now = Util.getMillis();
        float dt = lastFrameMs < 0 ? 0.0F : Math.min((now - lastFrameMs) / 1000.0F, 0.1F);
        lastFrameMs = now;
        if (working) {
            lastWorkMs = now;
        }
        boolean spinning = now - lastWorkMs <= IDLE_GRACE_MS;

        if (spinning) {
            brakeTarget = Float.NaN;
            // разгон примерно за полсекунды
            rotorSpeed += (ROTOR_SPEED - rotorSpeed) * Math.min(1.0F, dt * 6.0F);
            rotorAngle = (rotorAngle + rotorSpeed * dt) % 360.0F;
            return;
        }
        if (rotorSpeed <= 0.0F) {
            return;
        }
        if (Float.isNaN(brakeTarget)) {
            // ближайший прямой угол за обычным тормозным путём
            float distance = rotorSpeed * rotorSpeed / (2.0F * ROTOR_BRAKE);
            brakeTarget = (float) Math.ceil((rotorAngle + distance) / 90.0F) * 90.0F;
        }
        float remaining = brakeTarget - rotorAngle;
        if (remaining > 0.5F && rotorSpeed > 4.0F) {
            // равномерное замедление ровно до цели: v^2 = 2 * a * s
            float brake = rotorSpeed * rotorSpeed / (2.0F * remaining);
            rotorSpeed = Math.max(0.0F, rotorSpeed - brake * dt);
            rotorAngle = Math.min(brakeTarget, rotorAngle + rotorSpeed * dt);
            return;
        }
        rotorAngle = brakeTarget % 360.0F;
        rotorSpeed = 0.0F;
        brakeTarget = Float.NaN;
    }

    private void renderRotor(GuiGraphics graphics) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        // поворот вокруг центра чаши (между 8-м и 9-м пикселем спрайта)
        pose.translate(leftPos + ROTOR_X + 8.0F, topPos + ROTOR_Y + 8.0F, 0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(rotorAngle));
        pose.translate(-8.0F, -8.0F, 0.0F);
        graphics.blit(TEXTURE, 0, 0, ROTOR_U, ROTOR_V, 16, 16);
        pose.popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, (imageWidth - font.width(title)) / 2, titleLabelY, ApiaryScreen.TEXT_COLOR, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ApiaryScreen.TEXT_COLOR, false);
    }
}
