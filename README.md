# Arcanes Oubliés

Addon pour **Iron's Spells 'n Spellbooks** (Minecraft 1.20.1, Forge 47.4+), pour notre serveur RP.
La liste complète des écoles et des sorts prévus est dans [`ADDONS_SPELLS.txt`](ADDONS_SPELLS.txt).

## Contenu de la bêta 0.1.0

École de magie **Or** (nouvelle école dans Iron's Spells, avec ses attributs « Puissance des sorts d'Or » et
« Résistance à la magie de l'Or ») :

| Sort | Effet |
|---|---|
| **Trait d'Or** (Gold Bolt) | Lingot d'or tournoyant entouré d'un halo doré. À l'impact : flash, onde de choc dorée, éclats de pépites. La cible prend les dégâts complets, les ennemis à moins de 2,5 blocs prennent 40 %. |
| **Chaînes Dorées** (Gilded Chains) | Sort chargé (1 s) sur une cible visée : un cercle de runes doré apparaît au sol, 4 chaînes d'or jaillissent et immobilisent la cible (3 s au niveau 1, +1 s par niveau), puis se brisent en éclats. |

Plus le **Grimoire d'Or** (livre de sorts, 10 emplacements, +10 % puissance des sorts d'Or, +200 mana), les
parchemins de l'école Or et un onglet créatif « Arcanes Oubliés » avec le grimoire et tous les parchemins.

Les sons de la bêta sont des sons de Minecraft combinés (tintements d'améthyste, chaînes, enclume, armure en or).
Les textures sont dessinées par `tools/gen_assets.py`.

## Récupérer le mod

Chaque push lance le workflow GitHub Actions **Build & Test** : il compile le mod, lance des tests sur un serveur
sans écran (le mod se charge avec Iron's Spells, les deux sorts font des dégâts et les chaînes immobilisent), puis
publie le `.jar` dans l'artefact **arcanes-oublies-jar** de l'exécution.

## Installer

Dans le dossier `mods` (client et serveur), à côté de :

- Iron's Spells 'n Spellbooks 1.20.1-3.4.0 ou plus récent
- ses dépendances : Curios, GeckoLib, Player Animator, Caelus

## Développer

- Le code est dans `src/main/java/com/waylays/arcanes`.
- Textures et modèles d'objets : modifier `tools/gen_assets.py` puis lancer `python3 tools/gen_assets.py` (Pillow).
  Une texture faite à la main peut aussi remplacer directement le PNG du même nom.
