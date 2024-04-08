package top.theillusivec4.curios.client.gui;

import javax.annotation.Nonnull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.PacketDistributor;
import top.theillusivec4.curios.CuriosConstants;
import top.theillusivec4.curios.common.network.NetworkHandler;
import top.theillusivec4.curios.common.network.client.CPacketPage;

public class PageButton extends Button {
  private final CuriosScreenV2 parentGui;
  private final Type type;
  private static final ResourceLocation CURIO_INVENTORY =
      new ResourceLocation(CuriosConstants.MOD_ID,
          "textures/gui/curios/inventory_revamp.png");

  public PageButton(CuriosScreenV2 parentGui, int xIn, int yIn, int widthIn, int heightIn,
                    Type type) {
    super(xIn, yIn, widthIn, heightIn, CommonComponents.EMPTY,
        (button) -> NetworkHandler.INSTANCE.send(
            new CPacketPage(parentGui.getMenu().containerId, type == Type.NEXT),
            PacketDistributor.SERVER.noArg()), DEFAULT_NARRATION);
    this.parentGui = parentGui;
    this.type = type;
  }

  @Override
  public void renderWidget(@Nonnull GuiGraphics guiGraphics, int x, int y, float partialTicks) {
    int xText = type == Type.NEXT ? 43 : 32;
    int yText = 25;

    if (type == Type.NEXT) {
      this.setX(this.parentGui.getGuiLeft() + this.parentGui.panelWidth - 28 + 11);
      this.active = this.parentGui.getMenu().currentPage + 1 < this.parentGui.getMenu().totalPages;
    } else {
      this.setX(this.parentGui.getGuiLeft() + this.parentGui.panelWidth - 28);
      this.active = this.parentGui.getMenu().currentPage > 0;
    }

    if (!this.isActive()) {
      yText += 12;
    } else if (this.isHoveredOrFocused()) {
      xText += 22;
    }

    if (this.isHovered()) {
      guiGraphics.renderTooltip(Minecraft.getInstance().font,
          Component.translatable("gui.curios.page", this.parentGui.getMenu().currentPage + 1,
              this.parentGui.getMenu().totalPages), x, y);
    }
    guiGraphics.blit(CURIO_INVENTORY, this.getX(), this.getY(), xText, yText, this.width,
        this.height);
  }

  public enum Type {
    NEXT,
    PREVIOUS
  }
}
