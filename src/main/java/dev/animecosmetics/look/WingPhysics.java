package dev.animecosmetics.look;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.joml.Vector3d;
import org.joml.Vector3f;

public final class WingPhysics {
   private static final double OMEGA = 4.6;
   private static final double ZETA = 0.62;
   private static final double GAIN = 0.4;
   private static final double IMPULSE_SOFT = 10.0;
   private static final double L_STIFF = 0.4;
   private static final double L_MAX = 0.9;
   private static final double OMEGA_YAW = 5.0;
   private static final double ZETA_YAW = 0.7;
   private static final double GAIN_YAW = 0.35;
   private static final double YAW_SOFT = 360.0;
   private static final double YAW_MAX = 22.0;
   private static final double SMOOTH_T = 0.14;
   private static final double BOB_AMP = 0.035;
   private static final double BOB_HZ = 0.21;
   private static final double SWAY_DEG = 1.6;
   private static final double SWAY_HZ = 0.13;
   private static final double TELEPORT = 8.0;
   private static final double MAX_STEP = 0.004166666666666667;
   private static final Map<Integer, WingPhysics.Body> BODIES = new HashMap<>();

   public static void clear() {
      BODIES.clear();
   }

   private WingPhysics() {
   }

   public static WingPhysics.Pose sample(AvatarRenderState state) {
      double t = state.ageInTicks / 20.0;
      WingPhysics.Body b = BODIES.computeIfAbsent(LookKey.of(state.id), k -> new WingPhysics.Body());
      double yaw = state.bodyRot;
      if (!b.started || t < b.t - 1.0) {
         b.started = true;
         b.t = t;
         b.pos.set(state.x, state.y, state.z);
         b.vel.zero();
         b.x.zero();
         b.v.zero();
         b.yaw = yaw;
         b.yawRate = 0.0;
         b.th = 0.0;
         b.thv = 0.0;
      }

      double dt = t - b.t;
      if (dt > 0.5) {
         b.pos.set(state.x, state.y, state.z);
         b.vel.zero();
      }

      if (dt > 1.0E-5) {
         step(b, state.x, state.y, state.z, yaw, Math.min(dt, 0.25));
         b.t = t;
      }

      b.lastSeen = t;
      prune(t);
      b.last = pose(b, state.bodyRot, t, state.id);
      return b.last;
   }

   public static WingPhysics.Pose last(int id) {
      WingPhysics.Body b = BODIES.get(id);
      return b == null ? null : b.last;
   }

   private static void step(WingPhysics.Body b, double px, double py, double pz, double yawNow, double dt) {
      Vector3d np = new Vector3d(px, py, pz);
      Vector3d d = new Vector3d(np).sub(b.pos);
      b.pos.set(np);
      if (d.length() > 8.0) {
         b.vel.zero();
      } else {
         Vector3d nv = new Vector3d(d).div(dt);
         double a = 1.0 - Math.exp(-dt / 0.14);
         Vector3d sv = new Vector3d(b.vel).lerp(nv, a);
         Vector3d dv = new Vector3d(sv).sub(b.vel);
         b.vel.set(sv);
         double m = dv.length();
         if (m > 1.0E-9) {
            double soft = 10.0 * Math.tanh(m / 10.0);
            b.v.sub(dv.mul(0.4 * soft / m));
         }
      }

      double dyaw = wrap(yawNow - b.yaw);
      b.yaw += dyaw;
      double rate = b.yawRate + (dyaw / dt - b.yawRate) * (1.0 - Math.exp(-dt / 0.14));
      double drate = rate - b.yawRate;
      b.yawRate = rate;
      double sr = 360.0 * Math.tanh(Math.abs(drate) / 360.0);
      b.thv = b.thv - Math.signum(drate) * sr * 0.35;
      int n = (int)Math.ceil(dt / 0.004166666666666667);
      double h = dt / n;

      for (int i = 0; i < n; i++) {
         double r = b.x.length() / 0.4;
         double k = 21.159999999999997 * (1.0 + r * r);
         double c = 5.704;
         b.v.x = b.v.x + (-k * b.x.x - c * b.v.x) * h;
         b.v.y = b.v.y + (-k * b.x.y - c * b.v.y) * h;
         b.v.z = b.v.z + (-k * b.x.z - c * b.v.z) * h;
         b.x.fma(h, b.v);
         double ry = b.th / 22.0;
         double ky = 25.0 * (1.0 + ry * ry);
         b.thv = b.thv + (-ky * b.th - 7.0 * b.thv) * h;
         b.th = b.th + b.thv * h;
      }
   }

   private static WingPhysics.Pose pose(WingPhysics.Body b, float bodyRot, double t, int id) {
      double len = b.x.length();
      Vector3d shown = new Vector3d(b.x);
      if (len > 1.0E-9) {
         shown.mul(0.9 * Math.tanh(len / 0.9) / len);
      }

      double phase = id * 0.6180339887 % 1.0 * Math.PI * 2.0;
      shown.y = shown.y + 0.035 * Math.sin(1.319468914507713 * t + phase);
      Vector3f m = new Vector3f((float)shown.x, (float)shown.y, (float)shown.z);
      m.rotateY((float)Math.toRadians(-(180.0 - bodyRot)));
      m.set(-m.x, -m.y, m.z);
      float yaw = (float)(22.0 * Math.tanh(b.th / 22.0) + 1.6 * Math.sin(0.8168140899333463 * t + phase * 1.7));
      return new WingPhysics.Pose(m, yaw);
   }

   private static double wrap(double deg) {
      deg %= 360.0;
      if (deg >= 180.0) {
         deg -= 360.0;
      } else if (deg < -180.0) {
         deg += 360.0;
      }

      return deg;
   }

   private static void prune(double now) {
      if (BODIES.size() >= 16) {
         Iterator<WingPhysics.Body> it = BODIES.values().iterator();

         while (it.hasNext()) {
            WingPhysics.Body b = it.next();
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
      final Vector3d x = new Vector3d();
      final Vector3d v = new Vector3d();
      double yaw;
      double yawRate;
      double th;
      double thv;
      boolean started;
      double lastSeen;
      WingPhysics.Pose last;
   }

   public record Pose(Vector3f modelOffset, float yawDeg) {
      static final WingPhysics.Pose REST = new WingPhysics.Pose(new Vector3f(), 0.0F);
   }
}
