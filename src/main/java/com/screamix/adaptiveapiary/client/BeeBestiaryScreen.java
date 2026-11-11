package com.screamix.adaptiveapiary.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.screamix.adaptiveapiary.AdaptiveApiary;
import com.screamix.adaptiveapiary.genetics.BeeGenome;
import com.screamix.adaptiveapiary.genetics.BeeSpecies;
import com.screamix.adaptiveapiary.genetics.SpeciesRegistry;
import com.screamix.adaptiveapiary.item.BeeItem;
import com.screamix.adaptiveapiary.item.BeeType;
import com.screamix.adaptiveapiary.menu.BeeBestiaryMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class BeeBestiaryScreen extends AbstractContainerScreen<BeeBestiaryMenu> {
    private static final ResourceLocation BOOK = AdaptiveApiary.id("textures/gui/bee_bestiary.png");
    private static final String LANG = "gui.adaptiveapiary.bestiary.";
    private static final int INK = 0x4A3416;
    private static final int PAGE_SIZE = 5;
    private static final int LEFT_CENTER = 65;
    private static final int RIGHT_CENTER = 191;
    private static final int PRODUCT_Y = 98;
    private static final int PRODUCT_STEP = 20;

    private final List<BeeSpecies> entries = new ArrayList<>();
    private final List<BookButton> rows = new ArrayList<>(PAGE_SIZE);
    private BookButton previous;
    private BookButton next;
    private int page;
    private int selection;
    private ItemStack portrait = ItemStack.EMPTY;
    private List<ItemStack> products = List.of();

    public BeeBestiaryScreen(BeeBestiaryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 168;
        for (ResourceLocation id : menu.discoveries()) {
            BeeSpecies species = SpeciesRegistry.get(id);
            if (species.id().equals(id)) {
                entries.add(species);
            }
        }
        entries.sort(Comparator.comparing(BeeSpecies::isOreSpecies)
                .thenComparing(species -> species.displayName().getString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(BeeSpecies::id));
        if (!entries.isEmpty()) {
            select(0);
        }
    }

    @Override
    protected void init() {
        super.init();
        rows.clear();
        for (int row = 0; row < PAGE_SIZE; row++) {
            int index = row;
            rows.add(addRenderableWidget(new BookButton(12, 38 + 19 * row, 106, 17,
                    Component.empty(), button -> select(page * PAGE_SIZE + index))));
        }
        previous = addRenderableWidget(new BookButton(12, 143, 18, 15,
                Component.literal("<"), button -> turn(-1)));
        previous.setTooltip(Tooltip.create(Component.translatable(LANG + "previous")));
        next = addRenderableWidget(new BookButton(100, 143, 18, 15,
                Component.literal(">"), button -> turn(1)));
        next.setTooltip(Tooltip.create(Component.translatable(LANG + "next")));

        addSocial(147, "Telegram", "telegram", "https://t.me/screamixcode");
        addSocial(178, "CurseForge", "curseforge", "https://www.curseforge.com/members/screamix/projects");
        addSocial(209, "GitHub", "github", "https://github.com/Screamix1");
        updatePage();
    }

    private void addSocial(int x, String name, String icon, String url) {
        addRenderableWidget(new SocialButton(leftPos + x, topPos + 135, Component.literal(name),
                AdaptiveApiary.id("textures/gui/social_" + icon + ".png"), ConfirmLinkScreen.confirmLink(this, url)));
    }

    private int pageCount() {
        return Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private void turn(int direction) {
        page += direction;
        select(page * PAGE_SIZE);
        updatePage();
    }

    private void select(int index) {
        selection = index;
        BeeSpecies species = entries.get(index);
        portrait = BeeItem.create(BeeType.PRINCESS, BeeGenome.pure(species));
        products = species.products().stream().map(product -> product.stack().get()).filter(stack -> !stack.isEmpty()).toList();
        if (!rows.isEmpty()) {
            for (int row = 0; row < PAGE_SIZE; row++) {
                rows.get(row).selected = row == selection % PAGE_SIZE;
            }
        }
    }

    private void updatePage() {
        for (int row = 0; row < PAGE_SIZE; row++) {
            int index = page * PAGE_SIZE + row;
            BookButton button = rows.get(row);
            button.visible = index < entries.size();
            button.selected = index == selection;
            if (button.visible) {
                Component name = entries.get(index).displayName();
                button.setMessage(name);
                button.setTooltip(font.width(name) > button.getWidth() - 8 ? Tooltip.create(name) : null);
            }
        }
        previous.active = page > 0;
        next.active = page + 1 < pageCount();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BOOK, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawCentered(graphics, title, LEFT_CENTER, 14);
        drawCentered(graphics, Component.translatable(LANG + "discovered", entries.size()), LEFT_CENTER, 26);
        drawCentered(graphics, Component.translatable(LANG + "page", page + 1, pageCount()), LEFT_CENTER, 147);
        drawCentered(graphics, Component.translatable(LANG + "links"), RIGHT_CENTER, 123);

        if (entries.isEmpty()) {
            graphics.drawWordWrap(font, Component.translatable(LANG + "empty"), 18, 54, 94, INK);
            graphics.blit(BOOK, RIGHT_CENTER - 16, 43, 0, 168, 32, 32);
            return;
        }

        BeeSpecies species = entries.get(selection);
        drawPageText(graphics, species.displayName(), 14);
        graphics.pose().pushPose();
        graphics.pose().translate(RIGHT_CENTER - 16, 34, 0);
        graphics.pose().scale(2, 2, 1);
        graphics.renderItem(portrait, 0, 0);
        graphics.pose().popPose();

        drawPageText(graphics, Component.translatable("tooltip.adaptiveapiary.flowers", species.flowers().displayName()), 68);
        drawCentered(graphics, Component.translatable(LANG + "products"), RIGHT_CENTER, 88);
        for (int index = 0; index < products.size(); index++) {
            graphics.renderItem(products.get(index), productX(index), PRODUCT_Y);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        for (int index = 0; index < products.size(); index++) {
            if (isHovering(productX(index), PRODUCT_Y, 16, 16, mouseX, mouseY)) {
                graphics.renderTooltip(font, products.get(index), mouseX, mouseY);
                break;
            }
        }
    }

    private int productX(int index) {
        return RIGHT_CENTER - 8 + (2 * index - products.size() + 1) * PRODUCT_STEP / 2;
    }

    private void drawPageText(GuiGraphics graphics, Component text, int y) {
        List<FormattedCharSequence> lines = font.split(text, 106);
        for (int line = 0; line < Math.min(lines.size(), 2); line++) {
            FormattedCharSequence content = lines.get(line);
            graphics.drawString(font, content, RIGHT_CENTER - font.width(content) / 2, y + line * font.lineHeight, INK, false);
        }
    }

    private void drawCentered(GuiGraphics graphics, Component text, int x, int y) {
        graphics.drawString(font, text, x - font.width(text) / 2, y, INK, false);
    }

    private class BookButton extends Button {
        private boolean selected;

        BookButton(int x, int y, int width, int height, Component label, OnPress action) {
            super(leftPos + x, topPos + y, width, height, label, action, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean highlighted = active && (isHoveredOrFocused() || selected);
            int x = getX();
            int y = getY();
            graphics.fill(x, y, x + width, y + height, highlighted ? 0xFF946025 : 0xFFD0AD73);
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, highlighted ? 0xFFF9DB92 : 0xFFF8E8BD);
            Component label = getMessage();
            if (font.width(label) > width - 8) {
                label = Component.literal(font.plainSubstrByWidth(label.getString(), width - 8 - font.width("…")) + "…");
            }
            graphics.drawString(font, label, x + (width - font.width(label)) / 2, y + (height - 8) / 2,
                    active ? INK : 0x987A50, false);
        }
    }

    private static class SocialButton extends Button {
        private final ResourceLocation icon;

        SocialButton(int x, int y, Component name, ResourceLocation icon, OnPress action) {
            super(x, y, 26, 24, name, action, DEFAULT_NARRATION);
            this.icon = icon;
            setTooltip(Tooltip.create(name));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            boolean highlighted = isHoveredOrFocused();
            graphics.fill(x, y, x + width, y + height, highlighted ? 0xFFF5A623 : 0xFF5E3A18);
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1,
                    highlighted ? 0xFF6B4A22 : 0xFF4A3218);

            RenderSystem.enableBlend();
            int tint = highlighted ? 0xFFE07A : 0xD8C08A;
            graphics.setColor((tint >> 16 & 255) / 255F, (tint >> 8 & 255) / 255F, (tint & 255) / 255F, 1);
            graphics.blit(icon, x + 5, y + 4, 0, 0, 16, 16, 16, 16);
            graphics.setColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
        }
    }
}
