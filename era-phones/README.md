# Époque RP

Mod NeoForge 1.21.1 **séparé** (son propre `.jar`, `epoque-rp-<version>.jar`, sans Arcanes Oubliés ni Iron's Spells)
pour un serveur de jeu de rôle : téléphones d'époque, objets du quotidien des années 80 (lampe torche, talkie-walkie,
poste de radio, magnétoscope et cassettes, argent, carte d'identité, journal), commandes de RP et outils des maîtres
du jeu. L'identifiant interne du mod reste `eraphones` : les objets déjà dans les mondes sont conservés. Tout reste dans le personnage : on ne voit jamais un pseudo
de compte, seulement des numéros, les noms enregistrés dans les répertoires et les noms des personnages.

Contenu original (noms, modèles, sonneries, icônes) : aucune marque réelle ; l'opérateur fictif s'appelle **VOXA**.

## Le menu créatif

Quatre onglets : **Époque RP · Téléphones**, **· Lumière & radio** (lampe torche, piles, talkie-walkie, poste de
radio), **· Son & image** (magnétoscope, cassette), **· Vie quotidienne** (pièces et billets, carte d'identité,
feuille de papier, journal).

## Les objets des années 80

| Objet | Ce qu'il fait |
| --- | --- |
| Lampe torche | Clic droit pour l'allumer ou l'éteindre. Tenue (dans n'importe quelle main), elle projette un faisceau doux depuis la lampe : le chemin devant soi est éclairé, de plus en plus faiblement jusqu'à 16 blocs, et la tache de lumière là où il tombe. Un jeu de piles dure 20 minutes. |
| Piles | Clic droit avec une lampe torche ou un talkie-walkie dans l'autre main : piles neuves. |
| Talkie-walkie | Clic droit : sa façade (marche/arrêt, canal 1 à 16, piles). Avec Simple Voice Chat, en le tenant en main et en parlant, ta voix sort (avec un son de radio) des talkies allumés sur le même canal, où qu'ils soient dans l'inventaire, et ceux qui sont autour l'entendent aussi. Un « kssht » au début de chaque message. |
| Poste de radio (bloc) | Clic droit : allumer, régler la fréquence (88 à 108 FM, cadran à faire glisser, molette ou boutons) et le volume. Il grésille entre les stations ; l'œil magique s'allume quand une station passe. Une station, c'est un maître du jeu qui fait `/radio emettre 100.3` : sa voix sort des radios réglées dessus (`/radio arreter` pour finir). |
| Magnétoscope (bloc) + cassette | Clic droit sur le magnétoscope avec une cassette en main pour l'insérer (ou bouton « Insérer » sur sa façade). Touches : ⏏ éjecter, ◀◀ rembobiner, ▶ lecture, ❚❚ pause, ■ stop, ▶▶ avance rapide ; compteur de bande. Le son sort du magnétoscope (16 blocs autour, Simple Voice Chat requis). Comme une vraie bande : pour revenir au début il faut rembobiner, et on entend tout défiler à l'envers ; la cassette éjectée garde l'endroit où en était la bande. |
| Pièces et billets | 10 et 25 cents (frappées en relief, tranche striée), billets de 1, 5, 10, 20, 50 et 100 dollars imprimés recto verso, en 3D (dessins originaux). |
| Télévision (bloc) | Clic droit pour l'allumer ou l'éteindre. Posée à 4 blocs ou moins d'un magnétoscope, elle montre la cassette vidéo (MP4) qu'il lit : image de vieux poste cathodique (lignes, couleurs qui bavent, image qui tremble et se déchire, bande d'interférence, bas de l'image abîmé par la bande), son de petit haut-parleur usé avec décrochages. Écran bleu sans cassette, neige et grésillement sans magnétoscope. Rembobiner ou avancer fait défiler l'image aussi. Le film est préparé une fois par le serveur (≈ 1 min pour 3 min de film), puis gardé. Sans Simple Voice Chat, l'image défile quand même, sans le son. |
| Boîte à musique (bloc) | Clic droit : une petite valse en mineur, qui ralentit et baisse quand le ressort se détend (24 s). |
| Poupée de porcelaine (bloc) | Sa tête se tourne vers le joueur le plus proche… seulement quand tu ne la regardes pas. |
| Ballon rouge | Un ballon rouge au bout de sa ficelle, à tenir. |
| Télécommande du MJ (staff) | Clic droit : liste des joueurs à 64 blocs (clique pour choisir), touches pour brouillard, coupure, sang, tremblement, flotter, vertige, ballon, tout arrêter, les 12 sons, et un texte à murmurer ou narrer. |
| Cassette vierge | Clic droit (staff) : le dossier des cassettes s'ouvre sur ton ordinateur et un écran liste les MP3 qui s'y trouvent ; choisis-en un, écris l'étiquette, « Graver ». Le fichier est envoyé au serveur (24 Mo au plus) et gravé pour toujours sur la cassette tenue. Un joueur sans droits voit seulement qu'elle est vierge. |
| Carte d'identité | Vierge, elle est établie au nom de la première personne qui l'utilise (nom RP, signalement de `/desc`, numéro). Clic droit pour la regarder (avec la photo du visage du personnage), clic droit sur quelqu'un pour la lui montrer. |
| Journal | Imprimé par les maîtres du jeu ; clic droit pour le lire. |
| Feuille de papier | Clic droit : on écrit dessus avec la page sous les yeux qui se remplit en direct (titre, texte jusqu'à 2000 caractères avec retours à la ligne), on choisit le papier (feuille de carnet, papier à lettre, vieux parchemin, post-it, page arrachée, papier quadrillé, fiche bristol, vieux papier taché), l'encre (noire, bleue, rouge, verte, crayon) et l'écriture (à la main, penchée, ou tapée à la machine). « Garder » en fait un brouillon qu'on peut reprendre ; « Signer » la ferme avec le nom du personnage : ensuite, clic droit pour la lire, page entière à l'écran (molette pour les longs textes). Une pile de feuilles vierges en donne une écrite à la fois. |

### Commandes des maîtres du jeu pour ces objets

| Commande | Effet |
| --- | --- |
| `/narration <texte>` | Narration sur l'écran de tous les joueurs (bandes noires de cinéma, texte qui s'écrit) et dans leur chat. |
| `/narrationalone <joueurs> <texte>` | La même chose pour un seul joueur (ou ceux choisis). |
| `/cassette liste` | Les fichiers MP3 disponibles sur le serveur (`config/eraphones/cassettes/`, où arrivent aussi ceux gravés depuis le jeu). |
| `/cassette graver <fichier> <titre>` | Donne une cassette de ce fichier, avec ce titre sur l'étiquette. Elle ne peut plus être modifiée. |
| `/radio emettre <fréquence>` · `/radio arreter` | Passer à l'antenne sur une fréquence (ta voix sort des radios réglées dessus), ou s'arrêter. |
| `/lampe <joueurs> eteindre` · `clignoter [secondes]` · `casser` · `reparer` | Éteindre leurs lampes torches, les faire clignoter (10 s par défaut, avec de petits grésillements), les casser (elles ne s'allument plus, « Cassée » dans la description) ou les réparer. |
| `/identite delivrer <joueur>` | Donne à ce joueur une carte d'identité établie à son nom. |
| `/journal imprimer <exemplaires> Titre \| texte` | Imprime le journal (jusqu'à 64 exemplaires). |

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
| `/horreur sang <joueurs> [secondes]` | Un peu de sang au bord de la vue : un voile rouge qui bat comme un cœur, quelques coulures lentes, une éclaboussure dans un coin. |
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
