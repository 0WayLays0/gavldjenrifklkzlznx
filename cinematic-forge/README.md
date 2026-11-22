# Cinematic (Forge 1.20.1) — ORIGINAL

Un mod pour faire des **machinimas** dans Minecraft.

- Tu joues toi-même chaque rôle.
- Des acteurs (des PNJ avec le skin de ton choix) rejouent ce que tu as enregistré.
- Tu retouches leurs gestes membre par membre.
- Tu places tes caméras et tu filmes la scène.

Le mod donne aussi à tout le jeu un **rendu de film** : bandes noires, étalonnage, grain et profondeur de champ.

- **Le studio** (acteurs, enregistrement, scènes) a besoin du mod **sur le serveur et sur les clients**.
  En solo, rien à faire : ton monde est son propre serveur.
- **Le rendu film et la caméra auto (F7 à F10)** marchent tout seuls côté client.

## Les touches

Elles se changent dans Options > Commandes > Cinematic.

| Touche | Effet |
|---|---|
| **F6** | Ouvrir le studio |
| **N** | Caméra libre : tu voles, ton perso reste sur place |
| **K** | Ajouter un point de caméra depuis la vue actuelle, au moment du curseur |
| **P** | Lire / arrêter la scène (pendant un enregistrement : couper) |
| **← / →** | Avancer / reculer dans le temps (Maj : 1 s, Ctrl : 1 tick) |
| **B** | Gestes, pendant l'enregistrement |
| F7 | Rendu film on/off |
| F8 | Caméra automatique (filme ton perso, ou l'acteur choisi dans le studio) |
| F9 | Plan suivant de la caméra automatique |
| F10 | Profondeur de champ on/off |

## Faire un film, pas à pas

1. **F6**, puis crée une **scène** (« + Scène »), puis un **acteur** (« + Acteur »). L'acteur apparaît là où tu es.
2. **Skin** : glisse un fichier PNG sur la case de l'acteur dans le menu.
   - Tailles acceptées : de **64×64 à 2048×2048**, carré et multiple de 64 (64, 128, 256…).
   - Choisis « Bras : fins / larges » selon ton skin.
   - Tes skins sont aussi gardés dans `.minecraft/cinematic/skins` : le bouton « Skin suivant du dossier » les fait défiler.
   - Le skin est envoyé au serveur, qui le renvoie à tous les joueurs.
3. **● Enregistrer une prise** : compte à rebours 3-2-1, puis joue ton rôle. Tout est enregistré :
   - déplacements, regard, saut, accroupi, sprint, nage, élytre ;
   - coups d'épée, arc bandé, bouclier levé, objets tenus et armure ;
   - les blocs que tu poses et casses ;
   - les gestes : touche **B** pendant la prise (saluer, pointer, bras croisés, mains en l'air, facepalm, garde-à-vous,
     réfléchir, applaudir, s'asseoir, danser, genou à terre, hausser les épaules).

   **P** pour couper. Les autres acteurs de la scène rejouent leur prise **en même temps** : tu joues avec eux, prise après prise.
4. **Modifier les poses** : choisis un moment sur la timeline et un membre (tête, corps, bras, jambes). Règle les
   3 angles, puis « Poser la clé ». Le membre passe en douceur d'une clé à l'autre, par-dessus le mouvement enregistré.
   - La caméra tourne autour de l'acteur : clic droit glissé pour tourner, molette pour zoomer.
   - Pense à cliquer sur « Enregistrer ».
5. **Caméras** (« Réalisateur… ») : un film est une suite de **plans**. Chaque plan commence à un moment précis : la **coupe**.
   - **Trajectoire** : la caméra glisse en douceur de point en point. Chaque point garde la position, l'angle,
     l'inclinaison et le zoom.
   - **Plan fixe** : la caméra ne bouge pas. Elle peut suivre un acteur du regard.
   - **Suivi** : la caméra garde sa distance avec un acteur qui bouge.
   - **Orbite** : la caméra tourne autour d'un acteur, à la vitesse que tu règles.

   Pour placer des points : **N** pour la caméra libre, **←/→** pour aller au bon moment, puis **K** à chaque point.
   En caméra libre, les plans sont dessinés dans le monde : le plan choisi en jaune, les points avec leur direction.
6. **Aperçu** : la scène est rejouée seulement chez toi, sans toucher au monde.
   **▶ Lire** : la scène est jouée sur le serveur. Les blocs sont remis comme au départ, puis reposés et cassés au bon
   moment, et tous les joueurs la voient. La caméra du film suit tes plans, avec le HUD caché et les bandes noires.
   Enregistre ton écran avec OBS ou un autre logiciel.

Autres outils :
- « Déplacer la prise ici » déplace et tourne toute la prise pour qu'elle commence là où tu es (impossible si la prise modifie des blocs).
- « Dupliquer » copie un acteur avec sa prise, pratique pour une foule.
- Pendant la caméra libre, la molette règle la vitesse, **Ctrl+molette** le zoom et **Alt+molette** l'inclinaison.

## Rendu film

- Bandes noires au format 2.39:1.
- Étalonnage teal & orange avec une courbe filmique (ACES).
- Grain animé à 24 images par seconde, vignette, aberration chromatique sur les bords, léger bloom.
- **Profondeur de champ** : la mise au point se fait sur ce que tu regardes. Dans les plans du film, elle se fait sur
  l'acteur filmé.
- **Caméra fluide** en 1re personne : seule l'image est lissée, ta visée reste exacte. La caméra penche dans les
  virages et tremble légèrement, comme tenue à la main.

## Réglages

- `config/cinematic-client.toml` : force de chaque effet du rendu film.
- `serverconfig/cinematic-server.toml` (un par monde) :
  - niveau de permission pour réaliser (2 par défaut ; en solo, le propriétaire du monde peut toujours) ;
  - durée maximale d'une prise (10 min) ;
  - taille maximale d'un skin (2048 px).

Où sont rangées les données :
- **Prises et scènes** : dans le monde, `data/cinematic_studio.dat`.
- **Skins** : `cinematic_skins/` sur le serveur, `.minecraft/cinematic/skin_cache` sur les clients.
- **Plans de caméra** : sur ton PC, `.minecraft/cinematic/cameras/<monde>/<scène>.json`.

## Limites honnêtes

- Les plans de caméra sont **à toi** (sur ton PC). Les autres joueurs voient la scène, mais pas ta caméra.
- L'enregistrement suit ton perso tick par tick (20 par seconde). Une prise rejouée est fluide, mais si le serveur rame
  pendant la prise, les blocs peuvent arriver un poil décalés.
- Les blocs rejoués sont posés sans mettre à jour leurs voisins (pour que l'eau ou le sable ne partent pas en vrille).
  Le contenu des coffres et des panneaux n'est pas enregistré.
- Le geste « s'asseoir » baisse le corps mais ne s'assoit pas sur un bloc précis.
- Dormir dans un lit n'est pas rejoué comme une vraie pose couchée.
- Le rendu film passe après le monde et avant la main : la main en 1re personne n'est pas étalonnée.
- Incompatible avec un shader pack OptiFine/Oculus actif.
- Testé automatiquement par la CI (`.github/workflows/cinematic-forge.yml`) : un client lance un monde, crée un acteur
  avec un skin 512×512, enregistre une prise, la rejoue, ouvre chaque menu et prend des captures d'écran.
  Ce n'est pas un test fait à la main en jeu.
