package top.theillusivec4.curios.common.network.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.event.network.CustomPayloadEvent;
import top.theillusivec4.curios.common.inventory.container.CuriosContainerV2;

public class CPacketPage {

  private final int windowId;
  private final boolean next;

  public CPacketPage(int windowId, boolean next) {
    this.windowId = windowId;
    this.next = next;
  }

  public static void encode(CPacketPage msg, FriendlyByteBuf buf) {
    buf.writeInt(msg.windowId);
    buf.writeBoolean(msg.next);
  }

  public static CPacketPage decode(FriendlyByteBuf buf) {
    return new CPacketPage(buf.readInt(), buf.readBoolean());
  }

  public static void handle(CPacketPage msg, CustomPayloadEvent.Context ctx) {
    ctx.enqueueWork(() -> {
      ServerPlayer sender = ctx.getSender();

      if (sender != null) {
        AbstractContainerMenu container = sender.containerMenu;

        if (container instanceof CuriosContainerV2 && container.containerId == msg.windowId) {

          if (msg.next) {
            ((CuriosContainerV2) container).nextPage();
          } else {
            ((CuriosContainerV2) container).prevPage();
          }
        }
      }
    });
    ctx.setPacketHandled(true);
  }
}
