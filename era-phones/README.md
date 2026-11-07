# Téléphones d'Époque & RP

Mod NeoForge 1.21.1 **séparé** (son propre `.jar`, sans Arcanes Oubliés ni Iron's Spells) pour un serveur de jeu de
rôle : quatre téléphones d'époque et les commandes de RP. Tout reste dans le personnage : on ne voit jamais un pseudo
de compte, seulement des numéros, les noms enregistrés dans les répertoires et les noms des personnages.

Contenu original (noms, modèles, sonneries, icônes) : aucune marque réelle ; l'opérateur fictif s'appelle **VOXA**.

## Les téléphones

| Objet | Époque | Ce qu'il fait |
| --- | --- | --- |
| Radiotéléphone | 1985 | Brique beige à longue antenne, 8 chiffres rouges à 7 segments, 3 voyants. Appels seulement : SND / END / CLR, STO + chiffre et RCL + chiffre (10 mémoires), FCN # (mon numéro), FCN * ou VOL (silencieux), PWR. Batterie de quelques minutes d'appel, perd une barre de réseau à l'intérieur. |
| Téléphone GSM | 1997 | Écran vert rétroéclairé, menu en carrousel : messages de 160 caractères (lettres au multi-tap ou au clavier), répertoire, journal d'appels, Serpent, calculatrice, sonnerie, mon numéro. |
| Téléphone à clapet | 2004 | Écran couleur, menu en grille de 9 icônes, 4 thèmes, appareil photo VGA (160×120, couleurs un peu délavées), galerie, **Infos VOXA** (les articles du Journal de la ville, comme sur le smartphone), sonneries polyphoniques (dans Réglages). |
| Smartphone | 2026 | Écran verrouillé (widgets batterie, réseau, météo, lune), 12 applis + dock : Téléphone, Messages, Contacts, Appareil photo, Murmures (réseau social, signé d'un @nom choisi), Plans (positions partagées entre contacts), Photos, Météo, Journal (actualités de la ville), Calculette, Notes, Horloge, Boussole, Serpent, Dés, Réglages. |
| Chargeur | — | Tenu avec un téléphone dans l'autre main, clic droit maintenu : charge complète en ~12 s. |

Chaque téléphone reçoit **son numéro** (`555-XXXX`) la première fois qu'il est porté. Répertoire, messages, journal
d'appels et @nom appartiennent **au numéro** : qui tient le téléphone tient la ligne (un téléphone volé révèle ses
messages). Les messages attendent sur le serveur si le téléphone est éteint, sans batterie ou hors réseau.

- **Ouvrir** : utiliser l'objet (clic droit), ou la touche du téléphone (`N` par défaut) s'il est en main ou dans la
  barre d'action. Quand un téléphone sonne, la touche sort celui qui sonne.
- **Appels** : composer un numéro (ou un contact). Le téléphone appelé sonne là où il est (tout le monde autour
  l'entend, sauf en silencieux). Décroché, ce que l'on tape s'affiche chez l'autre ; avec Simple Voice Chat, les deux
  bouts **s'entendent à la voix**.
- **Réseau** : 4 barres à l'air libre, moins sous un toit, plus rien sous terre sans lumière du jour (caves, tunnels,
  égouts). Sans réseau, pas d'appel ni de message ; un appel en cours coupe.
- **Batterie** : se vide écran allumé et en appel (vite pour la brique de 1985), barre visible sous l'objet.

## Le jeu de rôle

| Commande | Effet |
| --- | --- |
| `/setname Prénom Nom` | Le nom du personnage remplace le pseudo partout (au-dessus de la tête, liste des joueurs, tout ce qui est dit). |
| *(chat normal)* | Parole entendue autour (20 blocs) : « Bill Denbrough dit : « … » ». |
| `/me action` | Action en violet italique, dans le chat (rien au-dessus de la tête). |
| `/do description` | Description de la scène (ambre). |
| `/roll [100]` | « Bill Denbrough a lancé un dé sur 100 : 42 » (dé sur 100 par défaut ; aussi `1d20`, `2d6`…). |
| `/try action` | Tente quelque chose : réussite ou échec. |
| `/chuchoter message` | 4 blocs ; un peu plus loin on voit seulement que quelqu'un chuchote. |
| `/crier message` | 48 blocs. |
| `/hrp message` | Hors personnage, à tout le monde (seul endroit où le pseudo apparaît). |
| `/desc texte` · `/examiner joueur` | Ce que les autres voient du personnage. |
| `/voir joueur` · `/voir tous` · `/voir personne` | N'être **vu et entendu que** par ce(s) joueur(s) : pour les autres le personnage n'existe plus (ni dessiné, ni entendu, ni touchable), ses paroles ne leur parviennent pas, et avec Simple Voice Chat sa voix non plus. Réservé aux maîtres du jeu par défaut (`voirPermission`). |
| `/pseudos cacher` · `/pseudos afficher` | Cache (ou réaffiche) les pseudos au-dessus de toutes les têtes, pour tout le monde : passe par une équipe du tableau des scores (« nom jamais visible »), que le jeu de base respecte sur chaque client, avec ou sans le mod (Essential compris, qui s'accroche au nom du jeu). Les joueurs sont alors dans l'équipe `eraphones_rp` (un joueur ne peut être que dans une équipe). Maîtres du jeu. |
| `/rp` · `/rp qui` · `/rp nom joueur Prénom Nom` | Aide ; pour les maîtres du jeu : qui joue qui, nommer un personnage. |

Réglages serveur (`serverconfig/eraphones-server.toml`) : chat local oui/non, portées (parole, actions, chuchotement,
cri), permission de `/voir`, noms au-dessus des têtes, bulles au-dessus des têtes (désactivées par défaut), voix au téléphone.

## Outils des maîtres du jeu (horreur)

| Commande | Effet |
| --- | --- |
| `/telephone inconnu sms <numéro> <message>` | SMS d'un **numéro inconnu** (impossible d'y répondre). |
| `/telephone inconnu appel <numéro> <ligne 1 \| ligne 2 \| …>` | Appel d'un numéro inconnu : sonnerie grave et déréglée ; une fois décroché, les lignes arrivent une à une, puis ça raccroche. |
| `/telephone hanter <numéro> [secondes] [message]` | Le téléphone se dérègle : parasites, image déchirée, un ballon rouge qui remonte l'écran, message en rouge, grésillement. |
| `/telephone journal Titre \| texte` | Publie un article dans le Journal des smartphones et les Infos VOXA des téléphones à clapet. |
| `/telephone numeros <joueur>` | Les numéros que porte un joueur. |
| `/telephone recharger` | Recharge le téléphone en main. |

### `/horreur` : ce que seules les victimes voient et entendent

`joueurs` accepte un pseudo ou un sélecteur (`@a`, `@a[distance=..15]`…). Rien n'apparaît dans le chat des victimes.

| Commande | Effet |
| --- | --- |
| `/horreur son <joueurs> <son> [ici]` | Un son qu'eux seuls entendent, placé autour d'eux (ou là où se tient le maître du jeu avec `ici`) : `pas` (des pas qui approchent dans leur dos), `toc` (on frappe), `porte` (une porte qui grince), `rire`, `enfants` (des rires d'enfants), `murmures`, `souffle` (une respiration dans la nuque), `coeur` (un cœur qui s'accélère), `cri`, `manege` (une boîte à musique qui se dérègle), `glas` (une cloche au loin), `gouttes`. |
| `/horreur murmure <joueurs> <texte>` | Des mots chuchotés dans leur tête : ils s'écrivent lettre par lettre au milieu de l'écran, en rouge, en tremblant. |
| `/horreur brouillard <joueurs> [secondes]` | Un brouillard épais se referme autour d'eux (60 s par défaut). |
| `/horreur coupure <joueurs> [secondes]` | La lumière vacille puis s'éteint ; un éclair de lumière de temps en temps ; elle revient en clignotant. |
| `/horreur sang <joueurs> [secondes]` | Du sang coule du haut de l'écran, les bords rougissent au rythme d'un cœur. |
| `/horreur sursaut <joueurs> [texte]` | Un visage surgit en plein écran avec un cri, l'image tremble (texte facultatif en dessous). |
| `/horreur ballon <joueurs> [ici]` | Un ballon rouge flotte devant eux (ou là où se tient le maître du jeu), se balance au bout de sa ficelle pendant 20 s, s'envole puis éclate. Les autres joueurs ne le voient pas. |
| `/horreur flotter <joueurs> [secondes]` | Ils s'élèvent lentement et flottent (6 s par défaut), puis redescendent doucement, sans dégâts. |
| `/horreur vertige <joueurs> [secondes]` | Le monde tangue autour d'eux. |
| `/horreur tremblement <joueurs> [secondes]` | Le sol tremble et gronde. |
| `/horreur stop <joueurs>` | Arrête tout pour eux (sons à venir, visions, vertige ; ceux qui flottent redescendent doucement). |

### `/voix` : changer de voix (Simple Voice Chat)

Avec Simple Voice Chat sur le serveur (version 2.5.27 ou plus récente, de décembre 2024), la voix du micro est
transformée **sur le serveur**, avant que quiconque l'entende : les joueurs n'ont rien à installer de plus, et la voix
changée passe aussi par `/voir` et par le téléphone. Avec une version plus ancienne, la voix reste normale.

| Commande | Effet |
| --- | --- |
| `/voix` | La liste des voix et la tienne. |
| `/voix <voix> [joueurs]` | Prendre une voix (ou la donner à des joueurs : un personnage possédé…). La hauteur et le timbre (la forme de la bouche et de la gorge) sont changés séparément : une voix grave garde une vraie bouche au lieu de sonner « ralentie ». `masque` (voix d'homme beaucoup trop grave, rauque, métallique, façon appel masqué), `demon` (deux gorges énormes, salle de pierre), `clown` (aiguë et chantante, un grognement caché dessous), `possede` (tes mots chantés par un accord sombre, vocodeur), `legion` (plusieurs voix à la fois), `robot` (une note bourdonnante qui parle), `geant`, `grave`, `enfant`, `spectre` (un souffle sans gorge), `egouts` (tunnel de béton), `radio`, `glitch` (voix qui se dérègle : saute, bégaie, se désagrège). |
| `/voix dose <10-100> [joueurs]` | Dose de l'effet en pourcentage (le reste est la vraie voix). 100 par défaut. |
| `/voix normale [joueurs]` | Retrouver sa voix. |
| `/voix ecoute` | S'entendre soi-même avec la voix changée, pour l'essayer (encore une fois pour arrêter). |

Les voix durent jusqu'au redémarrage du serveur. Permissions : `horreurPermission` et `voixPermission` (2 par défaut)
dans `serverconfig/eraphones-server.toml`.

## Développement

- Images, modèles 3D et textes : `python3 era-phones/tools/gen_assets.py` (numpy, pillow).
- Compilation, GameTests et test client (captures dans le journal) : workflow `Era Phones` sur GitHub Actions, qui ne
  tourne que quand `era-phones/` change. En local : `cd era-phones && ../gradlew build`.
- Simple Voice Chat est **optionnel** (API en `compileOnly`) : sans lui, tout marche sauf la voix.
