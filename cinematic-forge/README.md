# Cinematic (Forge 1.20.1) — ORIGINAL

Mod **client uniquement**, séparé des autres mods du dépôt : rend Minecraft comme un film.
Inutile sur le serveur ; les joueurs sans le mod peuvent jouer avec ceux qui l'ont.

## Touches (modifiables dans Options > Commandes > Cinématique)

| Touche | Effet |
|---|---|
| F7 | Rendu film on/off |
| F8 | Caméra cinématique on/off (le réalisateur te filme) |
| F9 | Plan suivant (pendant la caméra cinématique) |
| F10 | Profondeur de champ on/off |

## Ce qui est fait

- **Bandes noires 2.39:1** qui glissent à l'écran, fondu au noir à l'arrivée dans un monde et aux changements de plan.
- **Étalonnage teal & orange** + courbe filmique (ACES), **grain** animé à 24 i/s, **vignette**,
  **aberration chromatique** sur les bords, **bloom** léger.
- **Profondeur de champ** : mise au point auto sur le centre de l'écran (ou sur ton perso en caméra cinématique),
  arrière-plan flou.
- **Caméra fluide** en vue 1re personne (seule la vue est lissée, la visée reste exacte), **roulis** dans les virages
  et en strafe, secousse quand tu prends des dégâts, léger effet **caméra à l'épaule**, **FOV** qui s'adoucit.
- **Caméra cinématique** avec 6 plans qui s'enchaînent tout seuls (toutes les 9 s par défaut) :
  orbite, contre-plongée héroïque, grue qui s'élève, caméra sur pied fixe (téléobjectif), par-dessus l'épaule,
  travelling de face. La caméra évite les murs, le HUD est masqué.

Réglages : `config/cinematic-client.toml` (intensité de chaque effet, vitesse du lissage, durée des plans…).

## Limites

- Les shaders du mod passent après le monde et avant la main : la main en 1re personne n'est pas étalonnée.
- Incompatible avec un shader pack OptiFine/Oculus actif (ils remplacent le rendu).
- Construit et compilé par la CI (`.github/workflows/cinematic-forge.yml`) ; non testé en jeu à la main.
