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

package top.theillusivec4.curios.mixin.core;

import com.google.common.collect.Multimap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.common.capability.CurioItemCapability;
import top.theillusivec4.curios.mixin.CuriosImplMixinHooks;

@Mixin(value = CuriosApi.class, remap = false)
public class MixinCuriosApi {

  @Inject(at = @At("HEAD"), method = "registerCurio", cancellable = true)
  private static void curios$registerCurio(Item item, ICurioItem icurio, CallbackInfo ci) {
    CuriosImplMixinHooks.registerCurio(item, icurio);
    ci.cancel();
  }

  @Inject(at = @At("HEAD"), method = "getSlots(Z)Ljava/util/Map;", cancellable = true)
  private static void curios$getSlots(boolean isClient,
                                      CallbackInfoReturnable<Map<String, ISlotType>> cir) {
    cir.setReturnValue(CuriosImplMixinHooks.getSlots(isClient));
  }

  @Inject(at = @At("HEAD"), method = "getEntitySlots(Lnet/minecraft/world/entity/EntityType;Z)Ljava/util/Map;", cancellable = true)
  private static void curios$getEntitySlots(EntityType<?> type, boolean isClient,
                                            CallbackInfoReturnable<Map<String, ISlotType>> cir) {
    cir.setReturnValue(CuriosImplMixinHooks.getEntitySlots(type, isClient));
  }

  @Inject(at = @At("HEAD"), method = "getItemStackSlots(Lnet/minecraft/world/item/ItemStack;Z)Ljava/util/Map;", cancellable = true)
  private static void curios$getItemStackSlots(ItemStack stack, boolean isClient,
                                               CallbackInfoReturnable<Map<String, ISlotType>> cir) {

    cir.setReturnValue(CuriosImplMixinHooks.getItemStackSlots(stack, isClient));
  }

  @Inject(at = @At("HEAD"), method = "getItemStackSlots(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)Ljava/util/Map;", cancellable = true)
  private static void curios$getItemStackSlots(ItemStack stack, LivingEntity livingEntity,
                                               CallbackInfoReturnable<Map<String, ISlotType>> cir) {
    cir.setReturnValue(CuriosImplMixinHooks.getItemStackSlots(stack, livingEntity));
  }

  @Inject(at = @At("HEAD"), method = "getCurio", cancellable = true)
  private static void curios$getCurio(ItemStack stack,
                                      CallbackInfoReturnable<LazyOptional<ICurio>> cir) {
    cir.setReturnValue(CuriosImplMixinHooks.getCurio(stack));
  }

  @Inject(at = @At("HEAD"), method = "createCurioProvider", cancellable = true)
  private static void curios$createCurio(ICurio curio,
                                         CallbackInfoReturnable<ICapabilityProvider> cir) {
    cir.setReturnValue(CurioItemCapability.createProvider(curio));
  }

  @Inject(at = @At("HEAD"), method = "getCuriosInventory", cancellable = true)
  private static void curios$getCuriosInventory(LivingEntity livingEntity,
                                                CallbackInfoReturnable<LazyOptional<ICuriosItemHandler>> cir) {
    cir.setReturnValue(CuriosImplMixinHooks.getCuriosInventory(livingEntity));
  }

  @Inject(at = @At("HEAD"), method = "isStackValid", cancellable = true)
  private static void curios$isStackValid(SlotContext slotContext, ItemStack stack,
                                          CallbackInfoReturnable<Boolean> cir) {
    cir.setReturnValue(CuriosImplMixinHooks.isStackValid(slotContext, stack));
  }

  @Inject(at = @At("HEAD"), method = "getAttributeModifiers", cancellable = true)
  private static void curios$getAttributeModifiers(SlotContext slotContext, UUID uuid,
                                                   ItemStack stack,
                                                   CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir) {
    cir.setReturnValue(CuriosImplMixinHooks.getAttributeModifiers(slotContext, uuid, stack));
  }

  @Inject(at = @At("HEAD"), method = "addSlotModifier(Lcom/google/common/collect/Multimap;Ljava/lang/String;Ljava/util/UUID;DLnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;)V", cancellable = true)
  private static void curios$addSlotModifier(Multimap<Attribute, AttributeModifier> map,
                                             String identifier,
                                             UUID uuid, double amount,
                                             AttributeModifier.Operation operation,
                                             CallbackInfo ci) {
    CuriosImplMixinHooks.addSlotModifier(map, identifier, uuid, amount, operation);
    ci.cancel();
  }

  @Inject(at = @At("HEAD"), method = "addSlotModifier(Lnet/minecraft/world/item/ItemStack;Ljava/lang/String;Ljava/lang/String;Ljava/util/UUID;DLnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;Ljava/lang/String;)V", cancellable = true)
  private static void curios$addSlotModifier(ItemStack stack, String identifier, String name,
                                             UUID uuid, double amount,
                                             AttributeModifier.Operation operation, String slot,
                                             CallbackInfo ci) {
    CuriosImplMixinHooks.addSlotModifier(stack, identifier, name, uuid, amount, operation, slot);
    ci.cancel();
  }

  @Inject(at = @At("HEAD"), method = "addModifier", cancellable = true)
  private static void curios$addModifier(ItemStack stack, Attribute attribute, String name,
                                         UUID uuid, double amount,
                                         AttributeModifier.Operation operation, String slot,
                                         CallbackInfo ci) {
    CuriosImplMixinHooks.addModifier(stack, attribute, name, uuid, amount, operation, slot);
    ci.cancel();
  }

  @Inject(at = @At("HEAD"), method = "registerCurioPredicate", cancellable = true)
  private static void curios$registerSlotResultPredicate(ResourceLocation resourceLocation,
                                                         Predicate<SlotResult> validator,
                                                         CallbackInfo ci) {
    CuriosImplMixinHooks.registerCurioPredicate(resourceLocation, validator);
    ci.cancel();
  }

  @Inject(at = @At("HEAD"), method = "getCurioPredicate", cancellable = true)
  private static void curios$getSlotResultPredicate(ResourceLocation resourceLocation,
                                                    CallbackInfoReturnable<Optional<Predicate<SlotResult>>> ci) {
    ci.setReturnValue(CuriosImplMixinHooks.getCurioPredicate(resourceLocation));
  }

  @Inject(at = @At("HEAD"), method = "testCurioPredicates", cancellable = true)
  private static void curios$evaluateSlotResultPredicates(Set<ResourceLocation> predicates,
                                                          SlotResult slotResult,
                                                          CallbackInfoReturnable<Boolean> ci) {
    ci.setReturnValue(CuriosImplMixinHooks.testCurioPredicates(predicates, slotResult));
  }

  @Inject(at = @At("HEAD"), method = "getSlotUuid", cancellable = true)
  private static void curios$getUuid(SlotContext slotContext, CallbackInfoReturnable<UUID> ci) {
    ci.setReturnValue(CuriosImplMixinHooks.getSlotUuid(slotContext));
  }

  @Inject(at = @At("HEAD"), method = "broadcastCurioBreakEvent", cancellable = true)
  private static void curios$broadcastCurioBreakEvent(SlotContext slotContext, CallbackInfo ci) {
    CuriosImplMixinHooks.broadcastCurioBreakEvent(slotContext);
    ci.cancel();
  }
}
