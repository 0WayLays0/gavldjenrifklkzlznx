# Arcanes Oubliés

Addon pour **Iron's Spells 'n Spellbooks** (Minecraft 1.20.1, Forge 47.4+), pour notre serveur RP.
La liste complète des écoles et des sorts prévus est dans [`ADDONS_SPELLS.txt`](ADDONS_SPELLS.txt).

## Contenu de la bêta 0.5.0 : écoles de l'Or, de l'Explosion et du Livre

Trois nouvelles écoles complètes dans Iron's Spells, chacune avec ses attributs « Puissance des sorts » et
« Résistance », ses parchemins et son livre de sorts 3D (+10 % puissance de l'école, +200 mana ; 10 emplacements,
12 pour le Grimoire Vivant). Un onglet créatif « Arcanes Oubliés » contient les livres et tous les parchemins à tous
les niveaux.

### Or

| Sort | Effet |
|---|---|
| **Trait d'Or** (Gold Bolt) | Lingot d'or tournoyant. La cible prend tous les dégâts, les ennemis à 2,5 blocs autour en prennent 40 %. |
| **Chaînes Dorées** (Gilded Chains) | Chaînes d'or qui sortent d'un cercle de runes et immobilisent la cible, puis se brisent. |
| **Pluie d'Or** (Goldfall) | Lingots géants qui tombent du ciel l'un après l'autre autour de l'endroit visé. |
| **Pics d'Or** (Golden Spikes) | Vague de pics de cristal d'or qui jaillissent du sol devant le lanceur et projettent les ennemis. |
| **Malédiction de Midas** (Midas Curse) | La cible ralentit jusqu'à se figer en statue d'or, puis vole en éclats à la fin de l'effet. |
| **Lame Aurum** (Aurum Blade) | Épée d'or géante qui se forme au-dessus de la cible et s'abat sur elle. |
| **Torrent d'Or Fondu** (Molten Torrent) | Sort maintenu : flot d'or fondu qui brûle et laisse des flaques incandescentes. |
| **Armure Dorée** (Gilded Armor) | Armure d'or liquide : armure en plus et une partie des dégâts reçus renvoyée à l'attaquant. |
| **Sentinelles d'Or** (Golden Sentinels) | 2 à 4 chevaliers d'or qui défendent le lanceur pendant 30 s ou plus. |
| **Soleil d'Eldorado** (Sun of Eldorado, ultime) | Soleil d'or qui tire des rayons sur les ennemis proches puis explose. |

Livre : **Grimoire d'Or**. Focus de l'école : bloc d'or.

### Explosion

Chaque explosion : boule de feu, flash blanc, ondes de choc en anneaux (blanc puis violet), fumée, sifflement qui
monte avant la détonation et écran qui tremble. Le lanceur et ses alliés ne sont jamais touchés.

| Sort | Effet |
|---|---|
| **Orbe Explosif** (Blast Orb) | Orbe d'énergie instable crépitant qui éclate en double onde de choc à l'impact (3 blocs). |
| **Détonation en Chaîne** (Chain Detonation) | Explosion sur la cible, puis saut d'ennemi en ennemi (3 à 5 explosions, chacune plus grosse). |
| **Sceaux de Ruine** (Sigils of Ruin) | 3 à 5 runes au sol devant le lanceur. Un ennemi qui marche dessus les fait exploser, en cascade avec les sceaux voisins. |
| **Vague de Détonation** (Detonation Wave) | Le lanceur frappe le sol : des anneaux d'explosions s'étendent autour de lui, chaque ennemi touché une fois. |
| **Implosion** | Point de lumière noire qui aspire les ennemis proches, se contracte, puis détone en sphère blanche. |
| **Marque Volatile** (Volatile Brand) | Marque incandescente sur la cible : chaque coup reçu la charge, au 5e elle explose. Sinon elle s'éteint. |
| **Saut Explosif** (Blast Leap) | Explosion sous les pieds qui propulse le lanceur, piqué, puis seconde explosion à l'atterrissage. Pas de dégâts de chute. |
| **Barrage à Fragmentation** (Cluster Barrage) | Obus qui monte au-dessus de la cible et éclate en 8 sous-charges qui retombent et explosent en cascade. |
| **Bouclier Réactif** (Reactive Shield) | Bulle hexagonale crépitante : absorbe une partie de chaque coup et explose sur les attaquants (3 charges). |
| **Cataclysme** (Cataclysm, ultime) | Le lanceur s'élève, une sphère grossit 3 s en aspirant la lumière, puis dôme de lumière blanche géant et couronne de 12 explosions. |

Livre : **Codex de Détonation** (couverture noire craquelée, fissures de magma). Focus de l'école : TNT.

**Option staff** : par défaut les explosions ne cassent aucun bloc. Pour l'activer, dans
`<monde>/serverconfig/arcanes_oublies-server.toml`, mettre `explosionsBreakBlocks = true` (section `[explosion]`).
Même activée, la roche du fond, l'obsidienne et les blocs résistants aux explosions ne cassent pas.

### Livre (10 sorts + 3 sorts bonus)

| Sort | Effet |
|---|---|
| **Pages Tranchantes** (Razor Pages) | Éventail de 3 à 6 pages aux bords dorés brûlants ; chacune traverse un ou deux ennemis puis se déchire. |
| **Tempête de Pages** (Page Storm) | Un tourbillon de pages runiques entoure la cible, la suit et la lacère toutes les 0,4 s (et la ralentit). |
| **Encre Vivante** (Living Ink) | Vague d'encre qui s'étend en cône devant le lanceur ; des mains noires saisissent chaque ennemi touché, l'immobilisent, l'aveuglent et le serrent. |
| **Tome du Jugement** (Tome of Judgment) | Un livre géant pourpre apparaît dans le ciel au-dessus de la zone visée, s'ouvre, descend, puis ses deux moitiés se referment d'un coup sur la zone : gros dégâts, ralentissement, faiblesse. |
| **Marque-page** (Bookmark) | Pose un ruban lumineux sur un cercle de runes. Relancer le sort (relance d'Iron's, visible sur l'écran) ramène le lanceur au marque-page dans une pluie de pages. Même dimension seulement ; pas utilisable en parchemin (comme le Portail d'Iron's). |
| **Sceau du Silence** (Seal of Silence) | Un sceau de cire violette s'abat sur la cible : elle ne peut plus lancer de sorts (joueurs, parchemins compris, et mages d'Iron's) pendant 3 à 7 s. Le lait l'enlève. |
| **Bibliothèque Interdite** (Forbidden Library) | 2 à 4 grimoires volants (yeux violets, crocs le long des pages) qui suivent le lanceur, mordent et crachent de petits sorts runiques, 20 s ou plus. |
| **Mot de Pouvoir** (Word of Power) | Un mot runique géant s'écrit lettre par lettre en lettres de feu devant le lanceur, puis explose vers l'avant en cône de flammes. |
| **Réécriture** (Rewrite) | Le lanceur efface tous ses effets négatifs (et le feu) puis récupère un peu de vie ; des bandes d'écriture dorée s'enroulent autour de lui. |
| **Le Dernier Chapitre** (The Final Chapter, ultime) | Un grimoire colossal s'ouvre au-dessus de la zone, ses 64 pages forment un dôme, chaque ennemi dessous est marqué d'une rune qui explose ; puis le livre se referme. |
| **Grues de Papier** (Paper Cranes, bonus) | 3 à 5 grues en origami lumineuses s'envolent, puis piquent sur les ennemis devant le lanceur et éclatent en confettis de papier. |
| **Plume du Destin** (Quill of Fate, bonus) | Une plume spectrale géante trace une ligne d'encre au sol ; l'écriture s'illumine puis s'embrase en piliers de runes qui projettent les ennemis en l'air. |
| **Égide de Parchemin** (Parchment Aegis, bonus) | 3 à 6 pages tournent autour du lanceur : chacune arrête un projectile ennemi ou absorbe une partie d'un coup et repart lacérer l'attaquant. |

Livre : **Grimoire Vivant** (Living Grimoire) : couverture violette aux veines de runes qui pulsent, un œil qui
regarde autour de lui et cligne, des pages qui bougent toutes seules. Focus de l'école : livre.

Les sons sont des sons de Minecraft combinés. Les textures sont dessinées par `tools/gen_assets.py`
(`tools/explosion_assets.py` pour l'Explosion, `tools/book_assets.py` pour le Livre).

## Récupérer le mod

Chaque push lance le workflow GitHub Actions **Build & Test** :
- il compile le mod et lance des tests sur un serveur sans écran (chaque sort fait ce qu'il doit) ;
- il lance un vrai client Minecraft sans écran qui lance chaque sort devant la caméra (détecte les crashs d'affichage) ;
- il publie le `.jar` dans l'artefact **arcanes-oublies-jar** de l'exécution.

## Installer

Dans le dossier `mods` (client et serveur), à côté de :

- Iron's Spells 'n Spellbooks 1.20.1-3.4.0 ou plus récent
- ses dépendances : Curios, GeckoLib, Player Animator, Caelus

## Développer

- Le code est dans `src/main/java/com/waylays/arcanes`.
- Textures et modèles d'objets : modifier `tools/gen_assets.py` (ou `explosion_assets.py`, `book_assets.py`) puis lancer
  `python3 tools/gen_assets.py` (Pillow).
  Une texture faite à la main peut aussi remplacer directement le PNG du même nom.
