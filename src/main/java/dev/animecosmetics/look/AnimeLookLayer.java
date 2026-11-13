package dev.animecosmetics.look;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.animecosmetics.AnimeCosmetics;
import dev.animecosmetics.Toggles;
import dev.animecosmetics.fx.FxTypes;
import dev.animecosmetics.fx.LateGlow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Vector3f;

public final class AnimeLookLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
   private static final Identifier WING = AnimeCosmetics.id("textures/entity/custom_wing.png");
   private static final Identifier WING_EYE = AnimeCosmetics.id("textures/entity/custom_wing_eye.png");
   private static final Identifier WING_GLOW = AnimeCosmetics.id("textures/entity/custom_wing_glow.png");
   private static final Identifier WING_AURA = AnimeCosmetics.id("textures/entity/custom_wing_aura.png");
   private static final Identifier WING_AURA_GUI = AnimeCosmetics.id("textures/entity/custom_wing_aura_gui.png");
   private static final Identifier PIN = AnimeCosmetics.id("textures/entity/custom_pin.png");
   private static final float PIN_X = -0.25F;
   private static final float PIN_Y = -0.3875F;
   private static final float PIN_Z = -0.296875F;
   private static final float PIN_ROLL = 64.0F;
   private static final float PIN_YAW = 12.0F;
   private static final int FULL_BRIGHT = 15728880;
   private static final int WHITE = -1;
   private float dist;

   public AnimeLookLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
      super(parent);
   }

   public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
      Minecraft mc = Minecraft.getInstance();
      if (!state.isInvisible && mc.player != null && state.id == mc.player.getId()) {
         boolean wings = Toggles.get(Toggles.Feature.WINGS);
         boolean cape = Toggles.get(Toggles.Feature.CAPE);
         boolean outfit = Toggles.get(Toggles.Feature.SKIN);
         boolean feathers = Toggles.get(Toggles.Feature.FEATHERS);
         EyeFeathers.setActive(state.id, feathers);
         if (wings || cape || outfit || feathers || EyeFeathers.has(state.id)) {
            this.dist = poseStack.last().pose().getTranslation(new Vector3f()).length();
            int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
            WingPhysics.Pose drift = WingPhysics.sample(state);
            PlayerModel model = (PlayerModel)this.getParentModel();
            if (feathers || EyeFeathers.has(state.id)) {
               EyeFeathers.submit(poseStack, collector, state, model.body, drift, -1, yRot, xRot);
            }

            poseStack.pushPose();
            poseStack.translate(drift.modelOffset().x(), drift.modelOffset().y(), drift.modelOffset().z());
            poseStack.rotateDegrees(Axis.YP, drift.yawDeg());
            if (wings) {
               poseStack.pushPose();
               this.submitWings(poseStack, collector, lightCoords, state, overlay);
               poseStack.popPose();
            }

            if (outfit) {
               this.submitPins(poseStack, collector, state, drift, overlay);
            }

            if (outfit || cape) {
               SkirtPhysics.Pose phys = SkirtPhysics.sample(state);
               BodyFrame frame = BodyFrame.of(state, model.body, model.leftLeg);
               if (outfit) {
                  this.submitDress(poseStack, collector, state, drift, overlay, phys, frame);
               }

               if (cape) {
                  this.submitVeil(poseStack, collector, state, drift, overlay, phys, frame);
               }
            }

            poseStack.popPose();
         }
      }
   }

   private void submitWings(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, int overlay) {
      WingMesh gw = WingMesh.customWings();
      collector.submitCustomGeometry(
         poseStack, RenderTypes.armorCutoutNoCull(WING), (pose, buf) -> gw.render("wing", pose, buf, lightCoords, overlay, -1, false)
      );
      collector.order(2)
         .submitCustomGeometry(
            poseStack, RenderTypes.eyes(WING_GLOW), (pose, buf) -> gw.render("wing_glow", pose, buf, lightCoords, OverlayTexture.NO_OVERLAY, -1, false)
         );
      collector.submitCustomGeometry(
         poseStack, RenderTypes.armorCutoutNoCull(WING_EYE), (pose, buf) -> gw.render("eye_core", pose, buf, 15728880, OverlayTexture.NO_OVERLAY, -1, false)
      );
      if (this.dist < 20.0F) {
         int shellTint = ARGB.colorFromFloat(1.0F, 1.0F, 1.0F, 1.0F);
         collector.order(3)
            .submitCustomGeometry(
               poseStack,
               LateGlow.late(RenderTypes.entityTranslucentCull(WING_EYE)),
               (pose, buf) -> gw.render("eye_shell", pose, buf, 15728880, OverlayTexture.NO_OVERLAY, shellTint, false)
            );
      }

      float time = state.ageInTicks / 20.0F;
      boolean auraGui = LookKey.inGui;
      RenderType auraType = auraGui ? FxTypes.guiSoft(WING_AURA_GUI) : FxTypes.glowAdd(FxTypes.shadersOn() ? WING_AURA_GUI : WING_AURA);
      boolean auraAlpha = auraGui || FxTypes.shadersOn();
      boolean auraBoth = !auraGui && !FxTypes.noCull(auraType);
      if (this.dist < 64.0F) {
         collector.order(5).submitCustomGeometry(poseStack, auraType, (pose, buf) -> {
            gw.renderAuraSoft(pose, buf, time, false, 1.0F, auraBoth, auraAlpha);
            gw.renderAuraSoft(pose, buf, time, true, 1.0F, auraBoth, auraAlpha);
         });
      }

      int seed = state.id;
      if (this.dist < 20.0F) {
         collector.order(6).submitCustomGeometry(poseStack, LateGlow.late(RenderTypes.entityTranslucentCull(WingGlimmer.TEXTURE)), (pose, buf) -> {
            WingGlimmer.render(pose, buf, time, seed, false);
            WingGlimmer.render(pose, buf, time, seed, true);
         });
      }
   }

   private void submitPins(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state, WingPhysics.Pose drift, int overlay) {
      WingMesh pins = WingMesh.pins();
      int light = state.lightCoords;

      for (int side = 0; side < 2; side++) {
         boolean mirror = side == 1;
         poseStack.pushPose();
         poseStack.rotateDegrees(Axis.YP, -drift.yawDeg());
         poseStack.translate(-drift.modelOffset().x(), -drift.modelOffset().y(), -drift.modelOffset().z());
         ((PlayerModel)this.getParentModel()).head.translateAndRotate(poseStack);
         float sx = mirror ? -1.0F : 1.0F;
         poseStack.translate(-0.25F * sx, -0.3875F, -0.296875F);
         poseStack.rotateDegrees(Axis.YP, 12.0F * sx);
         poseStack.rotateDegrees(Axis.ZP, -64.0F * sx);
         collector.submitCustomGeometry(poseStack, RenderTypes.armorCutoutNoCull(PIN), (pose, buf) -> {
            pins.render("pin", pose, buf, light, overlay, -1, mirror);
            pins.render("seed", pose, buf, light, overlay, -1, mirror);
         });
         poseStack.popPose();
      }
   }

   private void submitDress(
      PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state, WingPhysics.Pose drift, int overlay, SkirtPhysics.Pose phys, BodyFrame frame
   ) {
      int light = state.lightCoords;
      poseStack.pushPose();
      poseStack.rotateDegrees(Axis.YP, -drift.yawDeg());
      poseStack.translate(-drift.modelOffset().x(), -drift.modelOffset().y(), -drift.modelOffset().z());
      ((PlayerModel)this.getParentModel()).body.translateAndRotate(poseStack);
      float[][] dress = CustomDress.simulate(state, phys, frame, drift);
      collector.submitCustomGeometry(
         poseStack, RenderTypes.entityCutout(CustomDress.TEXTURE), (pose, buf) -> CustomDress.render(dress, pose, buf, light, overlay, -1)
      );
      if (this.dist < 20.0F) {
         collector.submitCustomGeometry(
            poseStack, RenderTypes.entityCutout(CustomDress.CLASP_TEXTURE), (pose, buf) -> CustomDress.drawClasps(pose, buf, light, overlay, -1)
         );
      }

      poseStack.popPose();
   }

   private void submitVeil(
      PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state, WingPhysics.Pose drift, int overlay, SkirtPhysics.Pose phys, BodyFrame frame
   ) {
      int light = state.lightCoords;
      PlayerModel model = (PlayerModel)this.getParentModel();
      poseStack.pushPose();
      poseStack.rotateDegrees(Axis.YP, -drift.yawDeg());
      poseStack.translate(-drift.modelOffset().x(), -drift.modelOffset().y(), -drift.modelOffset().z());
      model.body.translateAndRotate(poseStack);
      float[][] veil = CustomVeil.simulate(state, phys, frame, model.body, model.head);
      collector.order(1)
         .submitCustomGeometry(
            poseStack, RenderTypes.entityTranslucent(CustomVeil.TEXTURE), (pose, buf) -> CustomVeil.drawVeil(veil, pose, buf, light, overlay, -1)
         );
      poseStack.popPose();
      if (this.dist < 20.0F) {
         poseStack.pushPose();
         poseStack.rotateDegrees(Axis.YP, -drift.yawDeg());
         poseStack.translate(-drift.modelOffset().x(), -drift.modelOffset().y(), -drift.modelOffset().z());
         model.head.translateAndRotate(poseStack);
         collector.submitCustomGeometry(
            poseStack, RenderTypes.entityCutout(CustomVeil.CLIP_TEXTURE), (pose, buf) -> CustomVeil.drawClip(pose, buf, light, overlay, -1)
         );
         poseStack.popPose();
      }
   }
}
