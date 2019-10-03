/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org>
 */
package com.github.liachmodded.mc160901.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecraftClient.class)
public abstract class ClientMixin {

  @Shadow public ClientPlayerInteractionManager interactionManager;
  @Shadow private int itemUseCooldown;
  @Shadow public ClientPlayerEntity player;
  @Shadow public HitResult hitResult;
  @Shadow @Final private static Logger LOGGER;
  @Shadow @Final public GameRenderer gameRenderer;
  @Shadow public ClientWorld world;

  /**
   * @reason Fix this mess
   * @author liach
   */
  @Overwrite
  private void doItemUse() {
    if (!this.interactionManager.isBreakingBlock()) {
      this.itemUseCooldown = 4;
      if (!this.player.isRiding()) {
        if (this.hitResult == null) {
          LOGGER.warn("Null returned as 'hitResult', this shouldn't happen!");
        }

        for(Hand hand_1 : Hand.values()) {
          ItemStack itemStack_1 = this.player.getStackInHand(hand_1);
          if (this.hitResult != null) {
            switch(this.hitResult.getType()) {
              case ENTITY:
                EntityHitResult entityHitResult_1 = (EntityHitResult)this.hitResult;
                Entity entity_1 = entityHitResult_1.getEntity();

                // Patch start - don't return if both are pass
                ActionResult result1 = this.interactionManager.interactEntityAtLocation(this.player, entity_1, entityHitResult_1, hand_1);
                ActionResult result2 = ActionResult.PASS;
                if (result1 == ActionResult.SUCCESS || (result2 = this.interactionManager.interactEntity(this.player, entity_1, hand_1)) == ActionResult.SUCCESS) {
                  this.player.swingHand(hand_1);
                }

                if (result1 == ActionResult.PASS && result2 == ActionResult.PASS) {
                  break;
                }
                // Patch end
                return;
              case BLOCK:
                BlockHitResult blockHitResult_1 = (BlockHitResult)this.hitResult;
                int int_1 = itemStack_1.getCount();
                ActionResult actionResult_1 = this.interactionManager.interactBlock(this.player, this.world, hand_1, blockHitResult_1);
                if (actionResult_1 == ActionResult.SUCCESS) {
                  this.player.swingHand(hand_1);
                  if (!itemStack_1.isEmpty() && (itemStack_1.getCount() != int_1 || this.interactionManager.hasCreativeInventory())) {
                    this.gameRenderer.firstPersonRenderer.resetEquipProgress(hand_1);
                  }

                  return;
                }

                if (actionResult_1 == ActionResult.FAIL) {
                  return;
                }
            }
          }

          if (!itemStack_1.isEmpty()) {
            TypedActionResult<ItemStack> typedActionResult_1 = this.interactionManager.interactItem(this.player, this.world, hand_1);
            if (typedActionResult_1.getResult() == ActionResult.SUCCESS) {
              if (typedActionResult_1.shouldSwingArm()) {
                this.player.swingHand(hand_1);
              }

              this.gameRenderer.firstPersonRenderer.resetEquipProgress(hand_1);
              return;
            }
          }
        }

      }
    }
  }

}
