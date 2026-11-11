# Époque RP — Forge 1.20.1

The same mod as `../era-phones` (NeoForge 1.21.1), ported to Minecraft 1.20.1 and Forge 47.

- Resources: made from era-phones' by `tools/forge_resources.py` (run it after era-phones' generator).
- Java: ported from era-phones; `compat/` gives the 1.21 names the code uses (packets over a Forge channel, item
  data kept in the item's tag).
- Build: `./gradlew build` (Gradle 8.8, ForgeGradle 6, Java 17); CI: `.github/workflows/era-phones-forge.yml`.
