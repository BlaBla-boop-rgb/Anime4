package dev.animecosmetics.gui;

import dev.animecosmetics.Toggles;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

/** Menu (default key R) to switch every cosmetic on or off with a click. */
public final class CosmeticsScreen extends Screen {
   private static final int W = 220;
   private static final int ROW = 22;
   private static final int HEAD = 26;
   private final KeyMapping openKey;
   private final long opened = System.nanoTime();
   private int hovered = -1;

   public CosmeticsScreen(KeyMapping openKey) {
      super(Component.translatable("animecosmetics.gui.title"));
      this.openKey = openKey;
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static Toggles.Feature[] features() {
      return Toggles.Feature.values();
   }

   private int panelHeight() {
      return HEAD + features().length * ROW + 18;
   }

   private int x0() {
      return this.width / 2 - W / 2;
   }

   private int y0() {
      return this.height / 2 - this.panelHeight() / 2;
   }

   private int rowAt(double mx, double my) {
      int x0 = this.x0();
      int top = this.y0() + HEAD;
      if (mx < x0 + 4 || mx > x0 + W - 4 || my < top) {
         return -1;
      }

      int r = (int)((my - top) / ROW);
      return r < features().length ? r : -1;
   }

   public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
      g.fill(0, 0, this.width, this.height, ARGB.color(110, 12, 0, 4));
   }

   public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
      super.extractRenderState(g, mouseX, mouseY, partial);
      float t = (float)(System.nanoTime() - this.opened) / 1.0E9F;
      float open = Math.min(1.0F, t / 0.2F);
      open = open * open * (3.0F - 2.0F * open);
      int a = (int)(255.0F * open);
      this.hovered = this.rowAt(mouseX, mouseY);
      int x0 = this.x0();
      int y0 = this.y0();
      int h = this.panelHeight();
      int cx = this.width / 2;
      g.fill(x0 - 2, y0 - 2, x0 + W + 2, y0 + h + 2, ARGB.color(a, 120, 12, 24));
      g.fill(x0, y0, x0 + W, y0 + h, ARGB.color(a * 235 / 255, 18, 4, 8));
      g.fill(x0 + 10, y0 + 19, x0 + W - 10, y0 + 20, ARGB.color(a * 160 / 255, 150, 20, 36));
      g.centeredText(this.font, this.title, cx, y0 + 7, ARGB.color(a, 255, 120, 130));
      Toggles.Feature[] fs = features();

      for (int i = 0; i < fs.length; i++) {
         int ry = y0 + HEAD + i * ROW;
         boolean on = Toggles.get(fs[i]);
         if (i == this.hovered) {
            g.fill(x0 + 4, ry, x0 + W - 4, ry + ROW - 2, ARGB.color(a * 200 / 255, 150, 20, 36));
            g.fill(x0 + 4, ry, x0 + 6, ry + ROW - 2, ARGB.color(a, 255, 90, 100));
         }

         g.text(this.font, Component.translatable("animecosmetics.feature." + fs[i].key), x0 + 12, ry + 6, ARGB.color(a, 235, 220, 222));
         int px1 = x0 + W - 10;
         int px0 = px1 - 40;
         g.fill(px0, ry + 3, px1, ry + ROW - 5, on ? ARGB.color(a, 200, 30, 50) : ARGB.color(a, 60, 40, 44));
         Component state = Component.translatable(on ? "animecosmetics.state.on" : "animecosmetics.state.off");
         g.centeredText(this.font, state, (px0 + px1) / 2, ry + 6, on ? ARGB.color(a, 255, 255, 255) : ARGB.color(a, 170, 150, 154));
      }

      g.centeredText(this.font, Component.translatable("animecosmetics.gui.hint"), cx, y0 + h - 13, ARGB.color(a * 180 / 255, 170, 120, 128));
   }

   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      int r = this.rowAt(event.x(), event.y());
      if (r >= 0) {
         Toggles.toggle(features()[r]);
         AbstractWidget.playButtonClickSound(this.minecraft.getSoundManager());
         return true;
      }

      return super.mouseClicked(event, doubleClick);
   }

   public boolean keyPressed(KeyEvent event) {
      if (this.openKey.matches(event)) {
         this.onClose();
         return true;
      }

      return super.keyPressed(event);
   }
}
