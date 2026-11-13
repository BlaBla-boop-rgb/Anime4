package dev.animecosmetics;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

public final class Toggles {
   private static final boolean[] STATE = new boolean[Toggles.Feature.values().length];
   private static boolean loaded;

   private Toggles() {
   }

   private static Path file() {
      return FabricLoader.getInstance().getConfigDir().resolve("animecosmetics.properties");
   }

   private static void load() {
      if (!loaded) {
         loaded = true;
         Path f = file();
         if (Files.exists(f)) {
            try (InputStream in = Files.newInputStream(f)) {
               Properties p = new Properties();
               p.load(in);

               for (Toggles.Feature feature : Toggles.Feature.values()) {
                  String v = p.getProperty(feature.key);
                  if (v != null) {
                     STATE[feature.ordinal()] = Boolean.parseBoolean(v.trim());
                  }
               }
            } catch (IOException var10) {
               AnimeCosmetics.LOGGER.warn("Could not read config: {}", var10.toString());
            }
         }
      }
   }

   private static void save() {
      Properties p = new Properties();

      for (Toggles.Feature feature : Toggles.Feature.values()) {
         p.setProperty(feature.key, Boolean.toString(STATE[feature.ordinal()]));
      }

      try (OutputStream out = Files.newOutputStream(file())) {
         p.store(out, "Anime Cosmetics (client only)");
      } catch (IOException var7) {
         AnimeCosmetics.LOGGER.warn("Could not write config: {}", var7.toString());
      }
   }

   public static boolean get(Toggles.Feature feature) {
      load();
      return STATE[feature.ordinal()];
   }

   public static boolean toggle(Toggles.Feature feature) {
      load();
      STATE[feature.ordinal()] = !STATE[feature.ordinal()];
      save();
      return STATE[feature.ordinal()];
   }

   static {
      for (Toggles.Feature feature : Toggles.Feature.values()) {
         STATE[feature.ordinal()] = feature.defaultOn;
      }
   }

   public static enum Feature {
      SKIN("skin", true),
      WINGS("wings", true),
      CAPE("cape", true),
      FEATHERS("feathers", true),
      SKY("sky", false),
      SUN_EYE("sun_eye", false),
      RIFT("rift", false),
      RIFT_SKY("rift_sky", false);

      public final String key;
      public final boolean defaultOn;

      private Feature(String key, boolean defaultOn) {
         this.key = key;
         this.defaultOn = defaultOn;
      }
   }
}
