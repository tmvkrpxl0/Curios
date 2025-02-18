/*
 * Copyright (c) 2018-2023 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 *
 * Curios is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Curios is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Curios.  If not, see <https://www.gnu.org/licenses/>.
 */

package top.theillusivec4.curios.common.inventory.container;

import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ICuriosMenu;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import top.theillusivec4.curios.common.CuriosRegistry;
import top.theillusivec4.curios.common.inventory.CosmeticCurioSlot;
import top.theillusivec4.curios.common.inventory.CurioSlot;
import top.theillusivec4.curios.common.network.NetworkHandler;
import top.theillusivec4.curios.common.network.client.CPacketScroll;
import top.theillusivec4.curios.common.network.server.SPacketScroll;

public class CuriosContainer extends RecipeBookMenu<CraftingContainer> implements ICuriosMenu {

  private static final ResourceLocation[] ARMOR_SLOT_TEXTURES = new ResourceLocation[] {
      InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
      InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET};
  private static final EquipmentSlot[] VALID_EQUIPMENT_SLOTS = new EquipmentSlot[] {
      EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
      EquipmentSlot.FEET};

  public final ICuriosItemHandler curiosHandler;
  public final Player player;

  private final boolean isLocalWorld;

  private final CraftingContainer craftMatrix = new TransientCraftingContainer(this, 2, 2);
  private final ResultContainer craftResult = new ResultContainer();
  public int lastScrollIndex;
  private boolean cosmeticColumn;
  private boolean skip = false;

  public CuriosContainer(int windowId, Inventory playerInventory, FriendlyByteBuf packetBuffer) {
    this(windowId, playerInventory);
  }

  public CuriosContainer(int windowId, Inventory playerInventory) {
    this(windowId, playerInventory, false);
  }

  public CuriosContainer(int windowId, Inventory playerInventory, boolean skip) {
    super(CuriosRegistry.CURIO_MENU.get(), windowId);
    this.player = playerInventory.player;
    this.isLocalWorld = this.player.level().isClientSide;
    this.curiosHandler = CuriosApi.getCuriosInventory(this.player).orElse(null);

    if (skip) {
      this.skip = true;
      return;
    }
    this.addSlot(
        new ResultSlot(playerInventory.player, this.craftMatrix, this.craftResult, 0, 154,
            28));

    for (int i = 0; i < 2; ++i) {

      for (int j = 0; j < 2; ++j) {
        this.addSlot(new Slot(this.craftMatrix, j + i * 2, 98 + j * 18, 18 + i * 18));
      }
    }

    for (int k = 0; k < 4; ++k) {
      final EquipmentSlot equipmentslottype = VALID_EQUIPMENT_SLOTS[k];
      this.addSlot(new Slot(playerInventory, 36 + (3 - k), 8, 8 + k * 18) {
        @Override
        public void set(@Nonnull ItemStack stack) {
          ItemStack itemstack = this.getItem();
          super.set(stack);
          CuriosContainer.this.player.onEquipItem(equipmentslottype, itemstack, stack);
        }

        @Override
        public int getMaxStackSize() {
          return 1;
        }

        @Override
        public boolean mayPlace(@Nonnull ItemStack stack) {
          return stack.canEquip(equipmentslottype, CuriosContainer.this.player);
        }

        @Override
        public boolean mayPickup(@Nonnull Player playerIn) {
          ItemStack itemstack = this.getItem();
          return (itemstack.isEmpty() || playerIn.isCreative() || !EnchantmentHelper
              .hasBindingCurse(itemstack)) && super.mayPickup(playerIn);
        }


        @OnlyIn(Dist.CLIENT)
        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
          return Pair.of(InventoryMenu.BLOCK_ATLAS,
              ARMOR_SLOT_TEXTURES[equipmentslottype.getIndex()]);
        }
      });
    }

    for (int l = 0; l < 3; ++l) {

      for (int j1 = 0; j1 < 9; ++j1) {
        this.addSlot(new Slot(playerInventory, j1 + (l + 1) * 9, 8 + j1 * 18, 84 + l * 18));
      }
    }

    for (int i1 = 0; i1 < 9; ++i1) {
      this.addSlot(new Slot(playerInventory, i1, 8 + i1 * 18, 142));
    }
    this.addSlot(new Slot(playerInventory, 40, 77, 62) {
      @OnlyIn(Dist.CLIENT)
      @Override
      public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return Pair
            .of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
      }
    });

    if (this.curiosHandler != null) {
      Map<String, ICurioStacksHandler> curioMap = this.curiosHandler.getCurios();
      int slots = 0;
      int yOffset = 12;

      for (String identifier : curioMap.keySet()) {
        ICurioStacksHandler stacksHandler = curioMap.get(identifier);
        IDynamicStackHandler stackHandler = stacksHandler.getStacks();

        if (stacksHandler.isVisible()) {

          for (int i = 0; i < stackHandler.getSlots() && slots < 8; i++) {
            this.addSlot(new CurioSlot(this.player, stackHandler, i, identifier, -18, yOffset,
                stacksHandler.getRenders(), stacksHandler.canToggleRendering()));
            yOffset += 18;
            slots++;
          }
        }
      }
      yOffset = 12;
      slots = 0;

      for (String identifier : curioMap.keySet()) {
        ICurioStacksHandler stacksHandler = curioMap.get(identifier);
        IDynamicStackHandler stackHandler = stacksHandler.getStacks();

        if (stacksHandler.isVisible()) {

          for (int i = 0; i < stackHandler.getSlots() && slots < 8; i++) {

            if (stacksHandler.hasCosmetic()) {
              IDynamicStackHandler cosmeticHandler = stacksHandler.getCosmeticStacks();
              this.cosmeticColumn = true;
              this.addSlot(
                  new CosmeticCurioSlot(this.player, cosmeticHandler, i, identifier, -37, yOffset));
            }
            yOffset += 18;
            slots++;
          }
        }
      }
    }
    this.scrollToIndex(0);
  }

  public boolean hasCosmeticColumn() {
    return this.cosmeticColumn;
  }

  public void resetSlots() {
    this.scrollToIndex(this.lastScrollIndex);
  }

  public void scrollToIndex(int indexIn) {

    if (this.curiosHandler != null) {
      Map<String, ICurioStacksHandler> curioMap = this.curiosHandler.getCurios();
      int slots = 0;
      int yOffset = 12;
      int index = 0;
      int startingIndex = indexIn;
      this.slots.subList(46, this.slots.size()).clear();
      this.lastSlots.subList(46, this.lastSlots.size()).clear();
      this.remoteSlots.subList(46, this.remoteSlots.size()).clear();

      for (String identifier : curioMap.keySet()) {
        ICurioStacksHandler stacksHandler = curioMap.get(identifier);
        IDynamicStackHandler stackHandler = stacksHandler.getStacks();

        if (stacksHandler.isVisible()) {

          for (int i = 0; i < stackHandler.getSlots() && slots < 8; i++) {

            if (index >= startingIndex) {
              slots++;
            }
            index++;
          }
        }
      }
      startingIndex = Math.min(startingIndex, Math.max(0, index - 8));
      index = 0;
      slots = 0;

      for (String identifier : curioMap.keySet()) {
        ICurioStacksHandler stacksHandler = curioMap.get(identifier);
        IDynamicStackHandler stackHandler = stacksHandler.getStacks();

        if (stacksHandler.isVisible()) {

          for (int i = 0; i < stackHandler.getSlots() && slots < 8; i++) {

            if (index >= startingIndex) {
              this.addSlot(new CurioSlot(this.player, stackHandler, i, identifier, -18, yOffset,
                  stacksHandler.getRenders(), stacksHandler.canToggleRendering()));
              yOffset += 18;
              slots++;
            }
            index++;
          }
        }
      }
      index = 0;
      slots = 0;
      yOffset = 12;

      for (String identifier : curioMap.keySet()) {
        ICurioStacksHandler stacksHandler = curioMap.get(identifier);
        IDynamicStackHandler stackHandler = stacksHandler.getStacks();

        if (stacksHandler.isVisible()) {

          for (int i = 0; i < stackHandler.getSlots() && slots < 8; i++) {

            if (index >= startingIndex) {

              if (stacksHandler.hasCosmetic()) {
                IDynamicStackHandler cosmeticHandler = stacksHandler.getCosmeticStacks();
                this.cosmeticColumn = true;
                this.addSlot(
                    new CosmeticCurioSlot(this.player, cosmeticHandler, i, identifier, -37,
                        yOffset));
              }
              yOffset += 18;
              slots++;
            }
            index++;
          }
        }
      }

      if (!this.isLocalWorld) {
        NetworkHandler.INSTANCE.send(new SPacketScroll(this.containerId, indexIn),
            PacketDistributor.PLAYER.with((ServerPlayer) this.player));
      }
      this.lastScrollIndex = indexIn;
    }
  }

  public void scrollTo(float pos) {

    if (this.curiosHandler != null) {
      int k = (this.curiosHandler.getVisibleSlots() - 8);
      int j = (int) (pos * k + 0.5D);

      if (j < 0) {
        j = 0;
      }

      if (j == this.lastScrollIndex) {
        return;
      }

      if (this.isLocalWorld) {
        NetworkHandler.INSTANCE.send(new CPacketScroll(this.containerId, j),
            PacketDistributor.SERVER.noArg());
      }
    }
  }

  @Override
  public void slotsChanged(@Nonnull Container inventoryIn) {

    if (!this.player.level().isClientSide) {
      ServerPlayer serverplayer = (ServerPlayer) this.player;
      ItemStack itemstack = ItemStack.EMPTY;
      Optional<RecipeHolder<CraftingRecipe>> optional =
          Objects.requireNonNull(this.player.level().getServer()).getRecipeManager()
              .getRecipeFor(RecipeType.CRAFTING, this.craftMatrix, this.player.level());

      if (optional.isPresent()) {
        RecipeHolder<CraftingRecipe> recipeholder = optional.get();
        CraftingRecipe craftingrecipe = recipeholder.value();

        if (this.craftResult.setRecipeUsed(this.player.level(), serverplayer, recipeholder)) {
          ItemStack itemstack1 =
              craftingrecipe.assemble(this.craftMatrix, this.player.level().registryAccess());

          if (itemstack1.isItemEnabled(this.player.level().enabledFeatures())) {
            itemstack = itemstack1;
          }
        }
      }
      this.craftResult.setItem(0, itemstack);
      this.setRemoteSlot(0, itemstack);
      serverplayer.connection.send(
          new ClientboundContainerSetSlotPacket(this.containerId, this.incrementStateId(), 0,
              itemstack));
    }
  }

  @Override
  public void removed(@Nonnull Player playerIn) {
    super.removed(playerIn);
    if (this.skip) {
      return;
    }
    this.craftResult.clearContent();

    if (!playerIn.level().isClientSide) {
      this.clearContainer(playerIn, this.craftMatrix);
    }
  }

  public boolean canScroll() {

    if (this.curiosHandler != null) {
      return this.curiosHandler.getVisibleSlots() > 8;
    }
    return false;
  }

  @Override
  public void setItem(int pSlotId, int pStateId, @Nonnull ItemStack pStack) {

    if (this.skip) {
      super.setItem(pSlotId, pStateId, pStack);
      return;
    }

    if (this.slots.size() > pSlotId) {
      super.setItem(pSlotId, pStateId, pStack);
    }
  }

  @Override
  public boolean stillValid(@Nonnull Player player) {
    return true;
  }

  @Nonnull
  @Override
  public ItemStack quickMoveStack(@Nonnull Player playerIn, int index) {
    ItemStack itemstack = ItemStack.EMPTY;
    Slot slot = this.slots.get(index);

    if (slot.hasItem()) {
      ItemStack itemstack1 = slot.getItem();
      itemstack = itemstack1.copy();
      EquipmentSlot entityequipmentslot = Mob.getEquipmentSlotForItem(itemstack);
      if (index == 0) {

        if (!this.moveItemStackTo(itemstack1, 9, 45, true)) {
          return ItemStack.EMPTY;
        }
        slot.onQuickCraft(itemstack1, itemstack);
      } else if (index < 5) {

        if (!this.moveItemStackTo(itemstack1, 9, 45, false)) {
          return ItemStack.EMPTY;
        }
      } else if (index < 9) {

        if (!this.moveItemStackTo(itemstack1, 9, 45, false)) {
          return ItemStack.EMPTY;
        }
      } else if (entityequipmentslot.getType() == EquipmentSlot.Type.ARMOR
          && !this.slots.get(8 - entityequipmentslot.getIndex()).hasItem()) {
        int i = 8 - entityequipmentslot.getIndex();

        if (!this.moveItemStackTo(itemstack1, i, i + 1, false)) {
          return ItemStack.EMPTY;
        }
      } else if (index < 46 &&
          !CuriosApi.getItemStackSlots(itemstack, playerIn.level()).isEmpty()) {

        if (!this.moveItemStackTo(itemstack1, 46, this.slots.size(), false)) {
          return ItemStack.EMPTY;
        }
      } else if (entityequipmentslot == EquipmentSlot.OFFHAND && !(this.slots.get(45))
          .hasItem()) {

        if (!this.moveItemStackTo(itemstack1, 45, 46, false)) {
          return ItemStack.EMPTY;
        }
      } else if (index < 36) {
        if (!this.moveItemStackTo(itemstack1, 36, 45, false)) {
          return ItemStack.EMPTY;
        }
      } else if (index < 45) {
        if (!this.moveItemStackTo(itemstack1, 9, 36, false)) {
          return ItemStack.EMPTY;
        }
      } else if (!this.moveItemStackTo(itemstack1, 9, 45, false)) {
        return ItemStack.EMPTY;
      }

      if (itemstack1.isEmpty()) {
        slot.set(ItemStack.EMPTY);
      } else {
        slot.setChanged();
      }

      if (itemstack1.getCount() == itemstack.getCount()) {
        return ItemStack.EMPTY;
      }
      slot.onTake(playerIn, itemstack1);

      if (index == 0) {
        playerIn.drop(itemstack1, false);
      }
    }

    return itemstack;
  }

  @Nonnull
  @Override
  public RecipeBookType getRecipeBookType() {
    return RecipeBookType.CRAFTING;
  }

  @Override
  public boolean shouldMoveToInventory(int index) {
    return index != this.getResultSlotIndex();
  }

  @Override
  public void fillCraftSlotsStackedContents(@Nonnull StackedContents itemHelperIn) {
    this.craftMatrix.fillStackedContents(itemHelperIn);
  }

  @Override
  public void clearCraftingContent() {
    this.craftMatrix.clearContent();
    this.craftResult.clearContent();
  }

  @Override
  public boolean recipeMatches(RecipeHolder<? extends Recipe<CraftingContainer>> recipeHolder) {
    return recipeHolder.value().matches(this.craftMatrix, this.player.level());
  }

  @Override
  public int getResultSlotIndex() {
    return 0;
  }

  @Override
  public int getGridWidth() {
    return this.craftMatrix.getWidth();
  }

  @Override
  public int getGridHeight() {
    return this.craftMatrix.getHeight();
  }

  @Override
  public int getSize() {
    return 5;
  }
}
