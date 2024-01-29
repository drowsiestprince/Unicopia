package com.minelittlepony.unicopia.client.gui.spellbook.element;

import java.util.Optional;

import com.minelittlepony.common.client.gui.IViewRoot;
import com.minelittlepony.common.client.gui.ScrollContainer;
import com.minelittlepony.common.client.gui.dimension.Bounds;
import com.minelittlepony.unicopia.client.gui.spellbook.SpellbookScreen;
import com.minelittlepony.unicopia.client.gui.spellbook.element.DynamicContent.Page;

import net.minecraft.client.gui.DrawContext;

class Panel extends ScrollContainer {
    private final DynamicContent content;

    Panel(DynamicContent content) {
        this.content = content;
    }

    private Optional<Page> page = Optional.empty();

    public void init(SpellbookScreen screen, int pageIndex) {
        verticalScrollbar.layoutToEnd = true;
        getContentPadding().top = 15;
        page = content.getPage(pageIndex);

        margin.left = screen.getX() + 30;
        margin.top = screen.getY() + 15;
        margin.right = screen.width - screen.getBackgroundWidth() - screen.getX() + 20;
        margin.bottom = screen.height - screen.getBackgroundHeight() - screen.getY() + 40;

        if (pageIndex % 2 == 1) {
            margin.left += screen.getBackgroundWidth() / 2 - 10;
        } else {
            margin.right += screen.getBackgroundWidth() / 2;
        }
        init(() -> {});
        screen.addDrawable(this);
        ((IViewRoot)screen).getChildElements().add(this);
    }

    @Override
    protected void renderContents(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        page.ifPresent(p -> {
            int oldHeight = p.getBounds().height;
            p.draw(context, mouseX, mouseY, this);
            if (p.getBounds().height != oldHeight) {
                verticalScrollbar.reposition();
            }
        });
        super.renderContents(context, mouseX, mouseY, partialTicks);
    }

    @Override
    public Bounds getContentBounds() {
        return page == null ? Bounds.empty() : page.map(page -> {
            return new Bounds(0, 0, 1, page.getBounds().height);
        }).orElse(Bounds.empty());
    }

    @Override
    protected void drawBackground(DrawContext context, int mouseX, int mouseY, float partialTicks) { }

    @Override
    protected void drawDecorations(DrawContext context, int mouseX, int mouseY, float partialTicks) { }
}