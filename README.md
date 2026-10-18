# Arcanes Oubliés

Addon pour **Iron's Spells 'n Spellbooks** (Minecraft 1.20.1, Forge 47.4+), pour notre serveur RP.
La liste complète des écoles et des sorts prévus est dans [`ADDONS_SPELLS.txt`](ADDONS_SPELLS.txt).

## Contenu de la bêta 0.2.0 : école de l'Or complète

École de magie **Or** (nouvelle école dans Iron's Spells, avec ses attributs « Puissance des sorts d'Or » et
« Résistance à la magie de l'Or ») :

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

Plus le **Grimoire d'Or** (livre de sorts 3D, 10 emplacements, +10 % puissance des sorts d'Or, +200 mana), les
parchemins de l'école Or et un onglet créatif « Arcanes Oubliés » avec le grimoire et tous les parchemins.

Les sons sont des sons de Minecraft combinés. Les textures sont dessinées par `tools/gen_assets.py`.

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
- Textures et modèles d'objets : modifier `tools/gen_assets.py` puis lancer `python3 tools/gen_assets.py` (Pillow).
  Une texture faite à la main peut aussi remplacer directement le PNG du même nom.
