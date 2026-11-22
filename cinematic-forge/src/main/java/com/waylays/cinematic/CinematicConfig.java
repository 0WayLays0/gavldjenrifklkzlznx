package com.waylays.cinematic;

import net.minecraftforge.common.ForgeConfigSpec;

/** config/cinematic-client.toml */
public final class CinematicConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue LOOK_ON_AT_START;
    public static final ForgeConfigSpec.BooleanValue LETTERBOX;
    public static final ForgeConfigSpec.BooleanValue DEPTH_OF_FIELD;
    public static final ForgeConfigSpec.DoubleValue DOF_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue GRADE;
    public static final ForgeConfigSpec.DoubleValue GRAIN;
    public static final ForgeConfigSpec.DoubleValue VIGNETTE;
    public static final ForgeConfigSpec.DoubleValue ABERRATION;
    public static final ForgeConfigSpec.DoubleValue BLOOM;
    public static final ForgeConfigSpec.BooleanValue SMOOTH_CAMERA;
    public static final ForgeConfigSpec.DoubleValue CAMERA_SMOOTHING;
    public static final ForgeConfigSpec.BooleanValue TILT;
    public static final ForgeConfigSpec.BooleanValue HANDHELD;
    public static final ForgeConfigSpec.BooleanValue SMOOTH_FOV;
    public static final ForgeConfigSpec.BooleanValue INTRO_FADE;
    public static final ForgeConfigSpec.DoubleValue SHOT_SECONDS;
    public static final ForgeConfigSpec.BooleanValue SHOT_HIDES_HUD;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("look");
        LOOK_ON_AT_START = b.comment("Film look active when the game starts (F7 toggles it)").define("enabledAtStart", true);
        LETTERBOX = b.comment("Black 2.39:1 bars in normal play (always on in the cinematic camera)").define("letterbox", true);
        DEPTH_OF_FIELD = b.comment("Lens blur around the focus point (F10 toggles it)").define("depthOfField", true);
        DOF_STRENGTH = b.defineInRange("depthOfFieldStrength", 1.0, 0.0, 3.0);
        GRADE = b.comment("Teal & orange colour grading, 0 = off").defineInRange("colorGrade", 1.0, 0.0, 1.0);
        GRAIN = b.defineInRange("filmGrain", 0.045, 0.0, 0.3);
        VIGNETTE = b.defineInRange("vignette", 0.45, 0.0, 1.0);
        ABERRATION = b.defineInRange("chromaticAberration", 1.0, 0.0, 4.0);
        BLOOM = b.defineInRange("bloom", 0.35, 0.0, 2.0);
        b.pop();
        b.push("camera");
        SMOOTH_CAMERA = b.comment("Smooths the view (the aim itself stays exact)").define("smoothCamera", true);
        CAMERA_SMOOTHING = b.comment("Higher = snappier").defineInRange("smoothingSpeed", 16.0, 2.0, 60.0);
        TILT = b.comment("Camera rolls into turns and kicks when hurt").define("tilt", true);
        HANDHELD = b.comment("Very light handheld breathing").define("handheld", true);
        SMOOTH_FOV = b.define("smoothFov", true);
        INTRO_FADE = b.comment("Fade in from black when joining a world").define("introFade", true);
        b.pop();
        b.push("cinematicCamera");
        SHOT_SECONDS = b.comment("Seconds before an automatic cut to the next shot (0 = no auto cut)").defineInRange("shotSeconds", 9.0, 0.0, 120.0);
        SHOT_HIDES_HUD = b.define("hideHud", true);
        b.pop();
        SPEC = b.build();
    }

    private CinematicConfig() {}
}
