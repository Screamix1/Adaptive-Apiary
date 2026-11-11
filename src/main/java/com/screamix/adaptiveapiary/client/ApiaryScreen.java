package com.screamix.adaptiveapiary.client;

import com.screamix.adaptiveapiary.AdaptiveApiary;
import com.screamix.adaptiveapiary.blockentity.ApiaryStatus;
import com.screamix.adaptiveapiary.menu.ApiaryMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ApiaryScreen extends AbstractContainerScreen<ApiaryMenu> {
    private static final ResourceLocation TEXTURE = AdaptiveApiary.id("textures/gui/apiary.png");
    static final int TEXT_COLOR = 0x4A3416;

    // полоса жизни королевы (вертикальная) и полоса прогресса (горизонтальная)
    private static final int LIFE_X = 16;
    private static final int LIFE_Y = 26;
    private static final int LIFE_WIDTH = 4;
    private static final int LIFE_HEIGHT = 42;
    private static final int PROGRESS_X = 29;
    private static final int PROGRESS_Y = 46;
    private static final int PROGRESS_WIDTH = 18;
    private static final int PROGRESS_HEIGHT = 3;
    private static final int STATUS_Y = 80;

    public ApiaryScreen(ApiaryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(LIFE_X - 1, LIFE_Y - 1, LIFE_WIDTH + 2, LIFE_HEIGHT + 2, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.adaptiveapiary.life", menu.life()), mouseX, mouseY);
        } else if (isHovering(PROGRESS_X - 1, PROGRESS_Y - 1, PROGRESS_WIDTH + 2, PROGRESS_HEIGHT + 2, mouseX, mouseY)) {
            String key = menu.status() == ApiaryStatus.BREEDING ? "gui.adaptiveapiary.breeding" : "gui.adaptiveapiary.cycle";
            graphics.renderTooltip(font, Component.translatable(key, menu.progress()), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int life = menu.life() * LIFE_HEIGHT / 100;
        if (life > 0) {
            graphics.blit(TEXTURE, leftPos + LIFE_X, topPos + LIFE_Y + LIFE_HEIGHT - life,
                    176, LIFE_HEIGHT - life, LIFE_WIDTH, life);
        }
        int progress = menu.progress() * PROGRESS_WIDTH / 100;
        if (progress > 0) {
            graphics.blit(TEXTURE, leftPos + PROGRESS_X, topPos + PROGRESS_Y, 176, 48, progress, PROGRESS_HEIGHT);
        }
        // точка статуса: зелёная - всё хорошо, красная - чего-то не хватает, серая - пусто
        ApiaryStatus status = menu.status();
        int dot = status == ApiaryStatus.EMPTY ? 2 : status.isProblem() ? 1 : 0;
        graphics.blit(TEXTURE, leftPos + 8, topPos + STATUS_Y, 176 + dot * 8, 56, 7, 7);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, (imageWidth - font.width(title)) / 2, titleLabelY, TEXT_COLOR, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_COLOR, false);
        ApiaryStatus status = menu.status();
        graphics.drawString(font, status.message(), 18, STATUS_Y, status.isProblem() ? 0x9A2418 : 0x2E6B1E, false);
    }
}
