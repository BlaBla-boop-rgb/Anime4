package dev.animecosmetics;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AnimeCosmetics {
   public static final String MOD_ID = "animecosmetics";
   public static final Logger LOGGER = LoggerFactory.getLogger("animecosmetics");

   private AnimeCosmetics() {
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("animecosmetics", path);
   }
}
