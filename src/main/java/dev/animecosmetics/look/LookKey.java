package dev.animecosmetics.look;

public final class LookKey {
   public static boolean inGui;

   private LookKey() {
   }

   public static int of(int entityId) {
      return inGui ? ~entityId : entityId;
   }
}
