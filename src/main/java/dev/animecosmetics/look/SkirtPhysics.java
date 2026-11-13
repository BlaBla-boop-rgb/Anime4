package dev.animecosmetics.look;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3d;
import org.joml.Vector3f;

public final class SkirtPhysics {
   private static final double GAIN = 0.55;
   private static final double GAIN_WATER = 0.95;
   private static final double DRAG_MAX = 3.0;
   private static final double LIFT_MAX = 1.1;
   private static final double LIFT_V = 5.0;
   private static final double LIFT_SIDE = 0.35;
   private static final double BUOYANCY = 0.6;
   private static final double OMEGA = 3.2;
   private static final double ZETA = 0.42;
   private static final double OMEGA_REST = 7.0;
   private static final double ZETA_REST = 0.72;
   private static final double REST_V = 0.7;
   private static final double OMEGA_WATER = 2.2;
   private static final double ZETA_WATER = 0.95;
   private static final double SMOOTH_T = 0.25;
   private static final double STOP_T = 0.07;
   private static final double GROUND_T = 0.55;
   private static final double LAND_T = 0.18;
   private static final double LAND_KILL_T = 0.06;
   private static final double WATER_T = 0.35;
   private static final double GROUND_PROBE = 6.0;
   private static final double FLUTTER_V0 = 1.0;
   private static final double FLUTTER_MAX = 0.28;
   private static final double FLUTTER_IDLE = 0.07;
   private static final double FLUTTER_WATER = 0.2;
   private static final double TELEPORT = 8.0;
   private static final double MAX_STEP = 0.004166666666666667;
   private static final Map<Integer, SkirtPhysics.Body> BODIES = new HashMap<>();
   private static SkirtPhysics.Body lastBody;
   private static SkirtPhysics.Pose lastPose;

   public static void clear() {
      BODIES.clear();
      lastBody = null;
      lastPose = null;
   }

   private SkirtPhysics() {
   }

   public static SkirtPhysics.Pose sample(AvatarRenderState state) {
      double t = state.ageInTicks / 20.0;
      SkirtPhysics.Body b = BODIES.computeIfAbsent(LookKey.of(state.id), k -> new SkirtPhysics.Body());
      if (!b.started || t < b.t - 1.0) {
         b.started = true;
         b.t = t;
         b.pos.set(state.x, state.y, state.z);
         b.vel.zero();
         b.d.zero();
         b.dv.zero();
         b.ground = 0.0;
         b.water = state.isInWater ? 1.0 : 0.0;
      }

      double dt = t - b.t;
      if (dt > 1.0E-5) {
         if (dt > 0.5) {
            b.pos.set(state.x, state.y, state.z);
            b.vel.zero();
         }

         step(b, state, Math.min(dt, 0.25));
         b.t = t;
      }

      b.lastSeen = t;
      prune(t);
      Vector3f m = new Vector3f((float)b.d.x, (float)b.d.y, (float)b.d.z);
      m.rotateY((float)Math.toRadians(-(180.0 - state.bodyRot)));
      m.set(-m.x, -m.y, m.z);
      double speed = b.vel.length();
      double flutterAir = 0.07 + 0.28 * Math.tanh(Math.max(0.0, speed - 1.0) / 6.0);
      double flutterWater = 0.2 + 0.14 * Math.tanh(speed / 4.0);
      float flutter = (float)(flutterAir + (flutterWater - flutterAir) * b.water);
      lastBody = b;
      lastPose = new SkirtPhysics.Pose(m, (float)(24.0 + b.ground * 16.0), flutter, (float)t, (float)b.water, b.field);
      return lastPose;
   }

   public static String debug() {
      SkirtPhysics.Body b = lastBody;
      SkirtPhysics.Pose p = lastPose;
      return b != null && p != null
         ? String.format(
            Locale.ROOT,
            "skirt: vel=%.2f,%.2f,%.2f dragW=%.2f,%.2f,%.2f dragM=%.2f,%.2f,%.2f ground=%.2f water=%.2f onGround=%b",
            b.vel.x,
            b.vel.y,
            b.vel.z,
            b.d.x,
            b.d.y,
            b.d.z,
            p.drag().x,
            p.drag().y,
            p.drag().z,
            b.ground,
            b.water,
            b.onGround
         )
         : "skirt: -";
   }

   private static void step(SkirtPhysics.Body b, AvatarRenderState state, double dt) {
      Vector3d np = new Vector3d(state.x, state.y, state.z);
      Vector3d delta = new Vector3d(np).sub(b.pos);
      b.pos.set(np);
      if (delta.length() > 8.0) {
         b.vel.zero();
      } else {
         Vector3d nv = new Vector3d(delta).div(dt);
         double tau = nv.lengthSquared() < b.vel.lengthSquared() ? 0.07 : 0.25;
         b.vel.lerp(nv, 1.0 - Math.exp(-dt / tau));
      }

      Entity e = entity(state);
      b.onGround = e == null || e.onGround();
      double waterTarget = state.isInWater ? 1.0 : 0.0;
      b.water = b.water + (waterTarget - b.water) * (1.0 - Math.exp(-dt / 0.35));
      double w = b.water;
      double gain = 0.55 + 0.3999999999999999 * w;
      Vector3d target = new Vector3d(b.vel).mul(-gain);
      double yaw = Math.toRadians(state.bodyRot);
      double fx = -Math.sin(yaw);
      double fz = Math.cos(yaw);
      double vf = b.vel.x * fx + b.vel.z * fz;
      double vh = Math.hypot(b.vel.x, b.vel.z);
      double vs = Math.sqrt(Math.max(0.0, vh * vh - vf * vf));
      double lift = 1.1 * Math.tanh(Math.max(0.0, vf) / 5.0) + 0.385 * Math.tanh(vs / 5.0);
      target.y += lift * (1.0 - w) + 0.6 * w;
      double len = target.length();
      if (len > 1.0E-9) {
         target.mul(3.0 * Math.tanh(len / 3.0) / len);
      }

      double rest = 1.0 - Math.tanh(Math.hypot(target.x, target.z) / 0.7 + Math.abs(target.y - 0.6 * w) / 0.7);
      double omegaAir = 3.2 + 3.8 * rest;
      double zetaAir = 0.42 + 0.3 * rest;
      double omega = omegaAir + (2.2 - omegaAir) * w;
      double zeta = zetaAir + (0.95 - zetaAir) * w;
      int n = (int)Math.ceil(dt / 0.004166666666666667);
      double h = dt / n;
      double k = omega * omega;
      double c = 2.0 * zeta * omega;
      double landKill = Math.exp(-h / 0.06);

      for (int i = 0; i < n; i++) {
         b.dv.x = b.dv.x + (k * (target.x - b.d.x) - c * b.dv.x) * h;
         b.dv.y = b.dv.y + (k * (target.y - b.d.y) - c * b.dv.y) * h;
         b.dv.z = b.dv.z + (k * (target.z - b.d.z) - c * b.dv.z) * h;
         b.d.fma(h, b.dv);
         if (b.onGround) {
            b.d.y *= landKill;
            b.dv.y *= landKill;
         }
      }

      double g = groundDistance(state, e);
      b.ground = b.ground + (g - b.ground) * (1.0 - Math.exp(-dt / (g < b.ground ? 0.18 : 0.55)));
      sampleField(b, state, g);
   }

   private static Entity entity(AvatarRenderState state) {
      Level level = Minecraft.getInstance().level;
      return level == null ? null : level.getEntity(state.id);
   }

   private static void sampleField(SkirtPhysics.Body b, AvatarRenderState state, double rawCenter) {
      SkirtPhysics.Ground f = b.field;
      double off = state.isCrouching ? state.scale * -2.0 / 16.0 : 0.0;
      f.feetX = state.x;
      f.feetY = state.y + off;
      f.feetZ = state.z;
      f.cx = (int)Math.floor(state.x);
      f.cz = (int)Math.floor(state.z);
      f.smooth = b.ground - rawCenter;
      double a = Math.toRadians(180.0 - state.bodyRot);
      f.yawCos = (float)Math.cos(a);
      f.yawSin = (float)Math.sin(a);
      f.centerPx = (float)(24.0 + (b.ground + off) * 16.0);
      Level level = Minecraft.getInstance().level;
      if (level == null) {
         Arrays.fill(f.top, Float.NaN);
      } else {
         int y1 = (int)Math.floor(state.y + 1.0);
         int y0 = (int)Math.floor(state.y - 6.0);
         MutableBlockPos pos = new MutableBlockPos();

         for (int i = 0; i < 9; i++) {
            for (int k = 0; k < 9; k++) {
               int x = f.cx - 4 + i;
               int z = f.cz - 4 + k;
               float top = Float.NaN;

               for (int y = y1; y >= y0; y--) {
                  pos.set(x, y, z);
                  VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
                  if (!shape.isEmpty()) {
                     double t = y + shape.max(Axis.Y);
                     if (t <= state.y + 1.0) {
                        top = (float)t;
                        break;
                     }
                  }
               }

               f.top[i * 9 + k] = top;
            }
         }
      }
   }

   private static double groundDistance(AvatarRenderState state, Entity e) {
      Level level = Minecraft.getInstance().level;
      if (level != null && e != null && !e.onGround()) {
         double feet = state.y;
         int x = (int)Math.floor(state.x);
         int z = (int)Math.floor(state.z);
         int y0 = (int)Math.floor(feet);
         MutableBlockPos pos = new MutableBlockPos();

         for (int y = y0; y > y0 - 6.0 - 1.0; y--) {
            pos.set(x, y, z);
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
               double top = y + shape.max(Axis.Y);
               if (top <= feet) {
                  return Math.min(6.0, feet - top);
               }
            }
         }

         return 6.0;
      } else {
         return 0.0;
      }
   }

   private static void prune(double now) {
      if (BODIES.size() >= 16) {
         Iterator<SkirtPhysics.Body> it = BODIES.values().iterator();

         while (it.hasNext()) {
            SkirtPhysics.Body b = it.next();
            if (now - b.lastSeen > 10.0 || b.lastSeen - now > 10.0) {
               it.remove();
            }
         }
      }
   }

   private static final class Body {
      double t;
      final Vector3d pos = new Vector3d();
      final Vector3d vel = new Vector3d();
      final Vector3d d = new Vector3d();
      final Vector3d dv = new Vector3d();
      double ground;
      final SkirtPhysics.Ground field = new SkirtPhysics.Ground();
      double water;
      boolean onGround;
      boolean started;
      double lastSeen;
   }

   public static final class Ground {
      static final int R = 4;
      static final int N = 9;
      final float[] top = new float[81];
      int cx;
      int cz;
      double feetX;
      double feetY;
      double feetZ;
      double smooth;
      float yawCos;
      float yawSin;
      float centerPx;

      public float px(float ux, float uz) {
         float mx = -ux / 16.0F;
         float mz = uz / 16.0F;
         float wx = mx * this.yawCos + mz * this.yawSin;
         float wz = -mx * this.yawSin + mz * this.yawCos;
         int bx = (int)Math.floor(this.feetX + wx) - this.cx + 4;
         int bz = (int)Math.floor(this.feetZ + wz) - this.cz + 4;
         if (bx >= 0 && bz >= 0 && bx < 9 && bz < 9) {
            float t = this.top[bx * 9 + bz];
            return Float.isNaN(t) ? this.centerPx : (float)(24.0 + (this.feetY - t + this.smooth) * 16.0);
         } else {
            return this.centerPx;
         }
      }

      public float rawPx(float ux, float uz) {
         float mx = -ux / 16.0F;
         float mz = uz / 16.0F;
         float wx = mx * this.yawCos + mz * this.yawSin;
         float wz = -mx * this.yawSin + mz * this.yawCos;
         int bx = (int)Math.floor(this.feetX + wx) - this.cx + 4;
         int bz = (int)Math.floor(this.feetZ + wz) - this.cz + 4;
         float t = bx >= 0 && bz >= 0 && bx < 9 && bz < 9 ? this.top[bx * 9 + bz] : Float.NaN;
         return Float.isNaN(t) ? 120.0F : (float)(24.0 + (this.feetY - t) * 16.0);
      }
   }

   public record Pose(Vector3f drag, float groundPx, float flutter, float time, float water, SkirtPhysics.Ground ground) {
   }
}
