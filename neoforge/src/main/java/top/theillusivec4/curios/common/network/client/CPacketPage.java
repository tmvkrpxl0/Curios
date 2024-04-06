package top.theillusivec4.curios.common.network.client;

import javax.annotation.Nonnull;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import top.theillusivec4.curios.CuriosConstants;

public record CPacketPage(int windowId, boolean next) implements CustomPacketPayload {

  public static final ResourceLocation
      ID = new ResourceLocation(CuriosConstants.MOD_ID, "client_page");

  public CPacketPage(final FriendlyByteBuf buf) {
    this(buf.readInt(), buf.readBoolean());
  }

  @Override
  public void write(@Nonnull FriendlyByteBuf buf) {
    buf.writeInt(this.windowId());
    buf.writeBoolean(this.next());
  }

  @Nonnull
  @Override
  public ResourceLocation id() {
    return ID;
  }
}
