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

package top.theillusivec4.curios.mixin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotAttribute;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.common.data.CuriosEntityManager;
import top.theillusivec4.curios.common.data.CuriosSlotManager;
import top.theillusivec4.curios.common.network.NetworkHandler;
import top.theillusivec4.curios.common.network.server.SPacketBreak;
import top.theillusivec4.curios.common.slottype.SlotType;

public class CuriosImplMixinHooks {

  private static final Map<Item, ICurioItem> REGISTRY = new ConcurrentHashMap<>();

  public static void registerCurio(Item item, ICurioItem icurio) {
    REGISTRY.put(item, icurio);
  }

  public static Optional<ICurioItem> getCurioFromRegistry(Item item) {
    return Optional.ofNullable(REGISTRY.get(item));
  }

  public static Map<String, ISlotType> getSlots(boolean isClient) {
    CuriosSlotManager slotManager = isClient ? CuriosSlotManager.CLIENT : CuriosSlotManager.SERVER;
    return slotManager.getSlots();
  }

  public static Map<String, ISlotType> getEntitySlots(EntityType<?> type, boolean isClient) {
    CuriosEntityManager entityManager =
        isClient ? CuriosEntityManager.CLIENT : CuriosEntityManager.SERVER;
    return entityManager.getEntitySlots(type);
  }

  public static Map<String, ISlotType> getItemStackSlots(ItemStack stack, boolean isClient) {
    return filteredSlots(slotType -> {
      SlotContext slotContext = new SlotContext(slotType.getIdentifier(), null, 0, false, true);
      SlotResult slotResult = new SlotResult(slotContext, stack);
      return CuriosApi.testCurioPredicates(slotType.getValidators(), slotResult);
    }, CuriosApi.getSlots(isClient));
  }

  public static Map<String, ISlotType> getItemStackSlots(ItemStack stack,
                                                         LivingEntity livingEntity) {
    return filteredSlots(slotType -> {
      SlotContext slotContext =
          new SlotContext(slotType.getIdentifier(), livingEntity, 0, false, true);
      SlotResult slotResult = new SlotResult(slotContext, stack);
      return CuriosApi.testCurioPredicates(slotType.getValidators(), slotResult);
    }, CuriosApi.getEntitySlots(livingEntity));
  }

  private static Map<String, ISlotType> filteredSlots(Predicate<ISlotType> filter,
                                                      Map<String, ISlotType> map) {
    Map<String, ISlotType> result = new HashMap<>();

    for (Map.Entry<String, ISlotType> entry : map.entrySet()) {
      ISlotType slotType = entry.getValue();

      if (filter.test(slotType)) {
        result.put(entry.getKey(), slotType);
      }
    }
    return result;
  }

  public static LazyOptional<ICurio> getCurio(ItemStack stack) {
    return stack.getCapability(CuriosCapability.ITEM);
  }

  public static LazyOptional<ICuriosItemHandler> getCuriosInventory(LivingEntity livingEntity) {

    if (livingEntity != null) {
      return livingEntity.getCapability(CuriosCapability.INVENTORY);
    } else {
      return LazyOptional.empty();
    }
  }

  public static boolean isStackValid(SlotContext slotContext, ItemStack stack) {
    String id = slotContext.identifier();
    Set<String> slots = getItemStackSlots(stack, slotContext.entity()).keySet();
    return (!slots.isEmpty() && id.equals("curio")) || slots.contains(id) ||
        slots.contains("curio");
  }

  public static Multimap<Attribute, AttributeModifier> getAttributeModifiers(
      SlotContext slotContext, UUID uuid, ItemStack stack) {
    Multimap<Attribute, AttributeModifier> multimap = HashMultimap.create();

    if (stack.getTag() != null && stack.getTag().contains("CurioAttributeModifiers", 9)) {
      ListTag listnbt = stack.getTag().getList("CurioAttributeModifiers", 10);
      String identifier = slotContext.identifier();

      for (int i = 0; i < listnbt.size(); ++i) {
        CompoundTag compoundnbt = listnbt.getCompound(i);

        if (compoundnbt.getString("Slot").equals(identifier)) {
          ResourceLocation rl = ResourceLocation.tryParse(compoundnbt.getString("AttributeName"));
          UUID id = uuid;

          if (rl != null) {

            if (compoundnbt.contains("UUID")) {
              id = compoundnbt.getUUID("UUID");
            }

            if (id.getLeastSignificantBits() != 0L && id.getMostSignificantBits() != 0L) {
              AttributeModifier.Operation operation =
                  AttributeModifier.Operation.fromValue(compoundnbt.getInt("Operation"));
              double amount = compoundnbt.getDouble("Amount");
              String name = compoundnbt.getString("Name");

              if (rl.getNamespace().equals("curios")) {
                String identifier1 = rl.getPath();
                LivingEntity livingEntity = slotContext.entity();
                boolean clientSide = livingEntity == null || livingEntity.level().isClientSide();

                if (CuriosApi.getSlot(identifier1, clientSide).isPresent()) {
                  CuriosApi.addSlotModifier(multimap, identifier1, id, amount, operation);
                }
              } else {
                Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(rl);

                if (attribute != null) {
                  multimap.put(attribute, new AttributeModifier(id, name, amount, operation));
                }
              }
            }
          }
        }
      }
    } else {
      multimap = getCurio(stack).map(curio -> curio.getAttributeModifiers(slotContext, uuid))
          .orElse(multimap);
    }
    CurioAttributeModifierEvent evt =
        new CurioAttributeModifierEvent(stack, slotContext, uuid, multimap);
    MinecraftForge.EVENT_BUS.post(evt);
    return LinkedHashMultimap.create(evt.getModifiers());
  }

  public static void addSlotModifier(Multimap<Attribute, AttributeModifier> map, String identifier,
                                     UUID uuid, double amount,
                                     AttributeModifier.Operation operation) {
    map.put(SlotAttribute.getOrCreate(identifier),
        new AttributeModifier(uuid, identifier, amount, operation));
  }

  public static void addSlotModifier(ItemStack stack, String identifier, String name, UUID uuid,
                                     double amount, AttributeModifier.Operation operation,
                                     String slot) {
    addModifier(stack, SlotAttribute.getOrCreate(identifier), name, uuid, amount, operation, slot);
  }

  public static void addModifier(ItemStack stack, Attribute attribute, String name, UUID uuid,
                                 double amount, AttributeModifier.Operation operation,
                                 String slot) {
    CompoundTag tag = stack.getOrCreateTag();

    if (!tag.contains("CurioAttributeModifiers", 9)) {
      tag.put("CurioAttributeModifiers", new ListTag());
    }
    ListTag listtag = tag.getList("CurioAttributeModifiers", 10);
    CompoundTag compoundtag = new CompoundTag();
    compoundtag.putString("Name", name);
    compoundtag.putDouble("Amount", amount);
    compoundtag.putInt("Operation", operation.toValue());

    if (uuid != null) {
      compoundtag.putUUID("UUID", uuid);
    }
    String id = "";

    if (attribute instanceof SlotAttribute wrapper) {
      id = "curios:" + wrapper.getIdentifier();
    } else {
      ResourceLocation rl = ForgeRegistries.ATTRIBUTES.getKey(attribute);

      if (rl != null) {
        id = rl.toString();
      }
    }

    if (!id.isEmpty()) {
      compoundtag.putString("AttributeName", id);
    }
    compoundtag.putString("Slot", slot);
    listtag.add(compoundtag);
  }

  public static void broadcastCurioBreakEvent(SlotContext slotContext) {
    NetworkHandler.INSTANCE.send(
        new SPacketBreak(slotContext.entity().getId(), slotContext.identifier(),
            slotContext.index()),
        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(slotContext.entity()));
  }

  private static final Map<String, UUID> UUIDS = new HashMap<>();

  public static UUID getSlotUuid(SlotContext slotContext) {
    String key = slotContext.identifier() + slotContext.index();
    return UUIDS.computeIfAbsent(key, (k) -> UUID.nameUUIDFromBytes(k.getBytes()));
  }


  private static final Map<ResourceLocation, Predicate<SlotResult>> SLOT_RESULT_PREDICATES =
      new HashMap<>();

  public static void registerCurioPredicate(ResourceLocation resourceLocation,
                                            Predicate<SlotResult> validator) {
    SLOT_RESULT_PREDICATES.putIfAbsent(resourceLocation, validator);
  }

  public static Optional<Predicate<SlotResult>> getCurioPredicate(ResourceLocation resourceLocation) {
    return Optional.ofNullable(SLOT_RESULT_PREDICATES.get(resourceLocation));
  }

  public static boolean testCurioPredicates(Set<ResourceLocation> predicates, SlotResult slotResult) {

    for (ResourceLocation id : predicates) {

      if (CuriosApi.getCurioPredicate(id).map(
          slotResultPredicate -> slotResultPredicate.test(slotResult)).orElse(false)) {
        return true;
      }
    }
    return false;
  }

  static {
    registerCurioPredicate(new ResourceLocation(CuriosApi.MODID, "all"), (slotResult) -> true);
    registerCurioPredicate(new ResourceLocation(CuriosApi.MODID, "none"),
        (slotResult) -> false);
    registerCurioPredicate(new ResourceLocation(CuriosApi.MODID, "tag"), (slotResult) -> {
      String id = slotResult.slotContext().identifier();
      TagKey<Item> tag1 = ItemTags.create(new ResourceLocation(CuriosApi.MODID, id));
      TagKey<Item> tag2 = ItemTags.create(new ResourceLocation(CuriosApi.MODID, "curio"));
      ItemStack stack = slotResult.stack();
      return stack.is(tag1) || stack.is(tag2);
    });
  }
}
