package dev.animecosmetics.look;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public record BodyFrame(Matrix3f a, Matrix3f at, Vector3f c, Matrix3f q, Matrix3f qt, Vector3f u0, float legZ) {
   private static final Vector3f T_MODEL = new Vector3f(0.0F, -24.016F, 0.0F);
   private static final BodyFrame IDENTITY = new BodyFrame(new Matrix3f(), new Matrix3f(), new Vector3f(), new Matrix3f(), new Matrix3f(), new Vector3f(), 0.0F);

   public static BodyFrame of(AvatarRenderState state, ModelPart body, ModelPart leg) {
      Matrix3f p = new Matrix3f();
      Vector3f d = new Vector3f();
      boolean rotated = false;
      if (state.isFallFlying) {
         float sc = state.fallFlyingScale();
         if (!state.isAutoSpinAttack) {
            p.rotateX((float)Math.toRadians(sc * (-90.0F - state.xRot)));
         }

         if (state.shouldApplyFlyingYRot) {
            p.rotateY(state.flyingYRot);
         }

         rotated = true;
      } else if (state.swimAmount > 0.0F) {
         float target = state.isInWater ? -90.0F - state.xRot : -90.0F;
         p.rotateX((float)Math.toRadians(state.swimAmount * target));
         if (state.isVisuallySwimming) {
            d.set(0.0F, -16.0F, 4.8F);
         }

         rotated = true;
      }

      boolean bodyMoved = body.x != 0.0F || body.y != 0.0F || body.z != 0.0F || body.xRot != 0.0F || body.yRot != 0.0F || body.zRot != 0.0F;
      if (!rotated && !bodyMoved && leg.z == 0.0F) {
         return IDENTITY;
      } else {
         Matrix3f s = new Matrix3f().scaling(-1.0F, -1.0F, 1.0F);
         Matrix3f q = new Matrix3f(s).mul(p).mul(s);
         Vector3f spd = new Vector3f(d);
         p.transform(spd);
         s.transform(spd);
         Vector3f u0 = new Vector3f(T_MODEL);
         q.transform(u0);
         u0.add(spd).sub(T_MODEL);
         Matrix3f rb = new Matrix3f().rotationZYX(body.zRot, body.yRot, body.xRot);
         Matrix3f a = new Matrix3f(q).mul(rb);
         Vector3f c = new Vector3f(body.x, body.y, body.z);
         q.transform(c);
         c.add(u0);
         return new BodyFrame(a, new Matrix3f(a).transpose(), c, q, new Matrix3f(q).transpose(), u0, leg.z);
      }
   }
}
