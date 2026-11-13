package dev.animecosmetics.fx;

final class DeathEyeShape {
   static final float[] TOP = new float[]{
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      0.0061F,
      0.0195F,
      0.0421F,
      0.0634F,
      0.0829F,
      0.0998F,
      0.1194F,
      0.1411F,
      0.1645F,
      0.1888F,
      0.214F,
      0.24F,
      0.2678F,
      0.2973F,
      0.3273F,
      0.3563F,
      0.3832F,
      0.408F,
      0.4297F,
      0.4484F,
      0.4648F,
      0.48F,
      0.4944F,
      0.5074F,
      0.5182F,
      0.5265F,
      0.5317F,
      0.5352F,
      0.5365F,
      0.5356F,
      0.5317F,
      0.5252F,
      0.5161F,
      0.5048F,
      0.4922F,
      0.4787F,
      0.464F,
      0.447F,
      0.428F,
      0.4071F,
      0.3854F,
      0.362F,
      0.3368F,
      0.309F,
      0.2804F,
      0.2517F,
      0.2253F,
      0.201F,
      0.1788F,
      0.158F,
      0.1389F,
      0.1215F,
      0.1059F,
      0.0911F,
      0.0716F,
      0.0482F,
      0.0226F,
      0.0069F,
      0.0F,
      0.0F
   };
   static final float[] BOT = new float[]{
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      -0.0043F,
      -0.0117F,
      -0.0208F,
      -0.0213F,
      -0.0135F,
      0.0022F,
      0.0208F,
      0.0425F,
      0.0677F,
      0.0951F,
      0.1241F,
      0.1541F,
      0.1849F,
      0.2157F,
      0.2452F,
      0.2734F,
      0.2999F,
      0.3247F,
      0.3468F,
      0.3672F,
      0.3859F,
      0.4028F,
      0.4175F,
      0.4297F,
      0.4392F,
      0.4449F,
      0.4479F,
      0.4488F,
      0.4488F,
      0.447F,
      0.4431F,
      0.4366F,
      0.428F,
      0.4171F,
      0.4041F,
      0.3885F,
      0.3702F,
      0.3503F,
      0.329F,
      0.3069F,
      0.283F,
      0.2574F,
      0.2305F,
      0.2036F,
      0.1771F,
      0.1515F,
      0.1259F,
      0.1011F,
      0.0768F,
      0.0547F,
      0.0343F,
      0.0169F,
      0.0017F,
      -0.0109F,
      -0.0174F,
      -0.0169F,
      -0.0095F,
      -0.0035F,
      0.0F,
      0.0F
   };
   static final float BASE_CX = -0.209F;
   static final float BASE_CY = -0.1045F;
   static final float BASE_H = 5.3504F;
   static final float BASE_HH = 2.6752F;
   static final float IRIS_CX = -0.3239F;
   static final float IRIS_CY = -0.1149F;
   static final float IRIS_H = 2.09F;
   static final float IRIS_R = 1.7312F;
   static final float PETALS_CX = -0.4075F;
   static final float PETALS_CY = -0.0836F;
   static final float PETALS_H = 1.3376F;
   static final float CORE_CX = -0.3344F;
   static final float CORE_CY = -0.0731F;
   static final float CORE_H = 0.6688F;
   static final float CORE_R = 0.5073F;
   static final float[] SPIKES = new float[]{-1.2043F, -0.6109F, -0.1571F, -0.0175F, 0.192F, 0.4014F, 0.925F, 1.2392F, 1.4835F, 2.2864F};

   private DeathEyeShape() {
   }

   static float top(float x) {
      return at(TOP, x);
   }

   static float bot(float x) {
      return at(BOT, x);
   }

   private static float at(float[] a, float x) {
      float f = Math.max(0.0F, Math.min((float)(a.length - 1), (x + 1.0F) / 2.0F * (a.length - 1)));
      int i = Math.min(a.length - 2, (int)f);
      return a[i] + (a[i + 1] - a[i]) * (f - i);
   }
}
