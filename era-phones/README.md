# Époque RP

Mod NeoForge 1.21.1 **séparé** (son propre `.jar`, `epoque-rp-<version>.jar`, sans Arcanes Oubliés ni Iron's Spells)
pour un serveur de jeu de rôle : téléphones d'époque, objets du quotidien des années 80 (lampe torche, talkie-walkie,
poste de radio, magnétoscope et cassettes, journal), et commandes des maîtres
du jeu pour ces objets. L'identifiant interne du mod reste `eraphones` : les objets déjà dans les mondes sont conservés. Tout reste dans le personnage : on ne voit jamais un pseudo
de compte, seulement des numéros, les noms enregistrés dans les répertoires et les noms des personnages.

Contenu original (noms, modèles, sonneries, icônes) : aucune marque réelle ; l'opérateur fictif s'appelle **VOXA**.

**Le jeu de rôle et les outils d'horreur sont dans Arcanes Oubliés** depuis la version 1.6.0 : `/setname`, `/me`, `/do`,
`/roll`, `/try`, `/chuchoter`, `/crier`, `/hrp`, `/desc`, `/examiner`, `/voir`, `/pseudos`, `/rp`, `/narration…`,
`/horreur`, `/peur`, `/ecrire`, `/voix`, `/declencheur`, `/taille`, `/vitesse`, `/hp`, `/porter` et `/arme`. Avec les
deux mods, les noms des personnages s'affichent aussi sur les photos, cassettes et feuilles, ce qu'on dit au
téléphone passe par le chat local d'Arcanes, et les déclencheurs de zone peuvent couper le courant, casser une lampe
torche ou envoyer un SMS. Les noms, tailles, PV, vitesses, la jauge de peur et les déclencheurs d'un monde déjà
joué sont repris tels quels. Les effets % posés par l'atelier d'armes d'avant sont gardés sur les objets mais ne font
plus rien : il faut les reposer avec `/arme`.

## Le menu créatif

Quatre onglets : **Époque RP · Téléphones**, **· Lumière & radio** (lampe torche, piles, talkie-walkie, poste de
radio), **· Son & image** (magnétoscope, cassette, baladeur, caméscope) et **· Vie quotidienne** (appareil photo,
vélos, lampadaire, répondeur, feuille de papier, journal).

## Les objets des années 80

| Objet | Ce qu'il fait |
| --- | --- |
| Lampe torche | Clic droit pour l'allumer ou l'éteindre. Tenue (dans n'importe quelle main), elle projette un faisceau doux depuis la lampe : le chemin devant soi est éclairé, de plus en plus faiblement jusqu'à 16 blocs, et la tache de lumière là où il tombe. Un jeu de piles dure 20 minutes. |
| Piles | Clic droit avec une lampe torche ou un talkie-walkie dans l'autre main : piles neuves. |
| Talkie-walkie | Clic droit : sa façade (marche/arrêt, canal 1 à 16, piles). Avec Simple Voice Chat, en le tenant en main et en parlant, ta voix sort (avec un son de radio) des talkies allumés sur le même canal, où qu'ils soient dans l'inventaire, et ceux qui sont autour l'entendent aussi. Un « kssht » au début de chaque message. |
| Poste de radio (bloc) | Clic droit : allumer, régler la fréquence (88 à 108 FM, cadran à faire glisser, molette ou boutons) et le volume. Il grésille entre les stations ; l'œil magique s'allume quand une station passe. Une station, c'est un maître du jeu qui fait `/radio emettre 100.3` : sa voix sort des radios réglées dessus (`/radio arreter` pour finir). |
| Magnétoscope (bloc) + cassette | Clic droit sur le magnétoscope avec une cassette en main pour l'insérer (ou bouton « Insérer » sur sa façade). Touches : ⏏ éjecter, ◀◀ rembobiner, ▶ lecture, ❚❚ pause, ■ stop, ▶▶ avance rapide ; compteur de bande. Le son sort du magnétoscope (16 blocs autour, Simple Voice Chat requis). Comme une vraie bande : pour revenir au début il faut rembobiner, et on entend tout défiler à l'envers ; la cassette éjectée garde l'endroit où en était la bande. |
| Télévision (bloc) | Lecteur intégré : clic droit avec une cassette pour l'insérer, elle se lance tout de suite. Clic droit pour allumer/éteindre (éteindre met en pause), accroupi + clic droit pour éjecter. Sans cassette, posée à 4 blocs ou moins d'un magnétoscope, elle montre ce qu'il lit. Image : image de vieux poste cathodique (lignes, couleurs qui bavent, image qui tremble et se déchire, bande d'interférence, bas de l'image abîmé par la bande), son de petit haut-parleur usé avec décrochages. Écran bleu sans cassette, neige et grésillement sans magnétoscope. Rembobiner ou avancer fait défiler l'image aussi. Le film est préparé une fois par le serveur (≈ 1 min pour 3 min de film), puis gardé. Sans Simple Voice Chat, l'image défile quand même, sans le son. |
| Cassette vierge | Clic droit (staff) : le dossier des cassettes s'ouvre sur ton ordinateur et un écran liste les MP3 qui s'y trouvent ; choisis-en un, écris l'étiquette, « Graver ». Le fichier est envoyé au serveur (aucune limite de taille, environ 3 Mo par seconde) et gravé pour toujours sur la cassette tenue. Un joueur sans droits voit seulement qu'elle est vierge. |
| Appareil photo instantané + pellicule | Clic droit : photographie ce que tu vois (sans la main ni l'interface), une photo sort de l'appareil. Pellicule de 10 photos ; pour recharger, une pellicule dans l'autre main. La photo se regarde (clic droit) dans son cadre blanc, signée du nom du photographe ; à peine sortie, elle se révèle en une minute (d'abord gris-bleu, puis les formes, puis les couleurs). |
| Vélo des années 80 | Cadre rouge, selle banane avec sissy bar chromé, guidon haut avec mousses, garde-boues chromés, pneus à flancs blancs, lampe, sonnette (dessins originaux). Utilise-le sur le sol pour le poser, utilise-le pour monter. Avancer pédale (il accélère jusqu'à ~30 km/h), reculer freine, il va où tu regardes et penche dans les virages, sauter fait sonner la sonnette, s'accroupir pour descendre. Il monte une marche d'un bloc, roule en roue libre, ralentit dans l'eau. Les roues et le pédalier tournent, le guidon tourne dans les virages, le cycliste pédale. Frappe-le pour le ramasser. |
| Vélos (4 modèles) | Chopper rouge (selle banane, sissy bar), vélo de course bleu (guidon de course, pneus fins, bidon), vélo de ville vert à cadre bas (panier en osier, porte-bagages, selle en cuir à ressorts), BMX chromé (plaque « 80 », mousses jaunes, pneus à crampons). Même conduite pour les quatre. |
| Lampadaire (bloc, 4 de haut) | Poteau galvanisé, bras au-dessus de la rue, tête « cobra » au verre sodium orange. Les lampadaires s'allument un par un à la tombée de la nuit (ils grésillent et clignotent en chauffant) et s'éteignent à l'aube. |
| Caméscope VHS | À porter sur l'épaule. Clic droit : il filme ce que tu vois (sans ta main ni l'interface) sur une cassette vierge que tu as sur toi, à 10 images/s, jusqu'à 15 min ; dans le viseur : REC qui clignote, le temps filmé, la date du jeu, la batterie. Clic droit encore (ou le ranger) : stop. La cassette se met ensuite dans une télé cathodique et le film passe. **Le son** : avec Simple Voice Chat, ta voix quand tu parles en filmant est enregistrée sur la bande, et aussi celles des joueurs qui parlent à 8 blocs ou moins (plus ils sont loin, moins on les entend) ; à la télé on les entend au bon moment, avec le son abîmé du vieux poste. Les autres bruits du jeu ne sont pas enregistrés. |
| Répondeur téléphonique (bloc) | Boîtier beige à deux micro-cassettes, compteur rouge, voyant de message. Clic droit avec ton téléphone en main : il est relié à ton numéro. Un appel sans réponse (9 s, ou 3 s si le téléphone est éteint ou pas sur toi) est pris par le répondeur : l'appelant entend un bip et laisse son message, à la voix (Simple Voice Chat, jusqu'à 1 min) et/ou par écrit. Le voyant clignote tant qu'il y a des messages. Clic droit : il les passe dans son petit haut-parleur (et les liste dans le tchat : numéro, quand, durée, texte) ; accroupi + clic droit : tout effacer. Il ne marche pas pendant une panne de courant. |
| Journal | Imprimé par les maîtres du jeu ; clic droit pour le lire. |
| Feuille de papier | Clic droit : on écrit dessus avec la page sous les yeux qui se remplit en direct (titre, texte jusqu'à 2000 caractères avec retours à la ligne), on choisit le papier (feuille de carnet, papier à lettre, vieux parchemin, post-it, page arrachée, papier quadrillé, fiche bristol, vieux papier taché), l'encre (noire, bleue, rouge, verte, crayon) et l'écriture (à la main, penchée, ou tapée à la machine). « Garder » en fait un brouillon qu'on peut reprendre ; « Signer » la ferme avec le nom du personnage : ensuite, clic droit pour la lire, page entière à l'écran (molette pour les longs textes). Une pile de feuilles vierges en donne une écrite à la fois. |

### Panne de courant

| Commande | Effet |
|---|---|
| `/panne <rayon> [secondes]` | Coupe le courant autour de toi (jusqu'à `/panne fin`, ou pendant le temps donné) : un bruit de coupure, les lampadaires grésillent et s'éteignent, les télés s'éteignent (et se rallument au retour du courant), les magnétoscopes s'arrêtent. La lampe torche, elle, marche toujours. |
| `/panne clignote <rayon> [secondes]` | Le courant faiblit : la nuit, les lampadaires clignotent et grésillent (10 s par défaut). |
| `/panne fin` | Rétablit le courant partout. |

Les coupures ne survivent pas à un redémarrage du serveur.

### Commandes des maîtres du jeu pour ces objets

| Commande | Effet |
| --- | --- |
| `/cassette liste` | Les fichiers MP3 disponibles sur le serveur (`config/eraphones/cassettes/`, où arrivent aussi ceux gravés depuis le jeu). |
| `/cassette graver <fichier> <titre>` | Donne une cassette de ce fichier, avec ce titre sur l'étiquette. Elle ne peut plus être modifiée. |
| `/radio emettre <fréquence>` · `/radio arreter` | Passer à l'antenne sur une fréquence (ta voix sort des radios réglées dessus), ou s'arrêter. |
| `/lampe <joueurs> eteindre` · `clignoter [secondes]` · `casser` · `reparer` | Éteindre leurs lampes torches, les faire clignoter (10 s par défaut, avec de petits grésillements), les casser (elles ne s'allument plus, « Cassée » dans la description) ou les réparer. |
| `/journal imprimer <exemplaires> Titre \| texte` | Imprime le journal (jusqu'à 64 exemplaires). |

## Baladeur et casque

Un **baladeur** à cassette (onglet Son & image) : une cassette dedans à la fois. Clic droit : lecture ou pause. La musique n'est entendue **que par celui qui l'écoute** (dans son casque, avec Simple Voice Chat), et elle continue tant que le baladeur est sur lui (en main ou dans sa barre). Pendant l'écoute, les autres voient un **casque à mousse orange** sur sa tête. Pour mettre une cassette : cassette dans l'autre main + clic droit (l'ancienne revient dans cette main), ou dans l'inventaire, clic droit avec la cassette sur le baladeur. Accroupi + clic droit (ou clic droit à vide sur lui dans l'inventaire) : la cassette sort. La cassette garde l'endroit où elle s'est arrêtée ; arrivée au bout, elle repart du début. On voit la cassette à travers la vitre du baladeur.

## Photos épinglées, emotes

### Photos épinglées (Polaroid)
L'**appareil photo instantané** (onglet Objets) prend ce que tu vois ; la photo sort et se révèle en une minute. **Accroupi + clic droit** avec une photo sur un **mur** : elle y est épinglée (une punaise de couleur, un peu de travers) ; sur une **table ou le sol** : elle y est posée. On y voit l'image, et le nom de qui l'a prise. Clic droit dessus : on la regarde en grand. Un coup : on la reprend. Plusieurs côte à côte font un **tableau d'enquête**.

### Emotes (mod Emotecraft)
Dans `emotes/` (et dans le zip du build) : onze emotes **dans le style d'un clown inquiétant**, faites ici de zéro, pose par pose (pas copiées d'un film). Chaque partie du corps bouge : coudes et genoux qui plient, torse qui se courbe, épaules qui tournent contre les hanches, tête qui compense.
| Fichier | Emote |
|---|---|
| `clown_sautille` | Pas sautillé léger (pas une course) : droit, un pas puis un rebond sur le même pied, l'autre genou très haut, long temps en l'air ; bras qui montent en grand sur les côtés ; la tête reste droite et fixe. Fait pour marcher avec. |
| `clown_danse` | Sa gigue : sautille d'un pied sur l'autre, mains agitées au-dessus de la tête d'un côté puis de l'autre ; accroupi, il bat des coudes et secoue les épaules ; bras au ciel, coup de pied, « ta-da » et petit salut. |
| `clown_tete_penchee` | Immobile, la tête se penche lentement sur l'épaule ; il respire, se balance, observe, puis sursaute (tête qui claque, bras qui part), et tourne la tête d'un coup. |
| `clown_assis` | Assis par terre, penché sur ses genoux, les bras autour des tibias (mains jointes devant), il se balance d'avant en arrière, tête penchée, yeux levés. |
| `clown_assis_rebord` | Assis au bord d'un bloc (mets-toi au bord, face au vide) : jambes qui se balancent comme un enfant, tête qui roule d'une épaule à l'autre. |
| `clown_tend_objet` | Il tend l'objet qu'il a en main droite (le ballon…) à hauteur de poitrine, l'autre main dans le dos, tête sur le côté ; la main bouge doucement, puis s'approche. |
| `clown_viens` | Signe de venir : la main tendue vers toi puis ramenée contre lui, deux fois, l'autre main sur la hanche. |
| `clown_rire` | Il se tient le ventre et éclate de rire : penché en arrière tête renversée, puis plié en deux, le corps qui tressaute (ha-ha-ha). |
| `clown_coucou` | Il se cache le visage dans les mains en se balançant, puis les ouvre d'un coup de chaque côté du visage en se jetant vers toi : COUCOU ! puis se cache à nouveau. |
| `clown_reverence` | Une vraie révérence : un pied glisse en arrière, une main sur le ventre, l'autre bras ouvert sur le côté, il se plie aux hanches, tête baissée, puis se redresse. |
| `clown_marionnette` | Une marionnette : un peu soulevé, poignets tenus par les fils, tête qui pend ; il « marche » dans le vide, un genou et la main opposée tirés vers le haut, puis l'autre. |

Chacune a son **icône** (`.png` du même nom : le clown dans sa pose, sur un rond rouge et or). Installation : avec **Emotecraft**, copier les `.json` **et les `.png`** dans `.minecraft/emotes/`, relancer le jeu, les mettre sur la roue des emotes. Avec Emotecraft aussi sur le serveur, les autres joueurs qui l'ont voient l'emote. `tools/emote_preview.py` les dessine sur un joueur en blocs pour les vérifier.

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
  l'entend, sauf en silencieux). Avec Simple Voice Chat, l'appel est **vocal** : dès que l'autre décroche, le téléphone
  se range (on le garde à l'oreille, l'appel continue, affiché en haut de l'écran) et les deux bouts **se parlent au
  micro**, où qu'ils soient ; chacun entend l'autre « au téléphone », selon l'appareil de celui qui parle (la brique de
  1985 grésille et crachote, le GSM et le clapet ont la bande étroite du téléphone, le smartphone est clair). Ceux qui
  sont autour entendent toujours ce qu'on dit. Sans Simple Voice Chat (ou pour ceux qui n'ont pas de micro), ce qu'on
  tape s'affiche chez l'autre.
- **Réseau** : 4 barres à l'air libre, moins sous un toit, plus rien sous terre sans lumière du jour (caves, tunnels,
  égouts). Sans réseau, pas d'appel ni de message ; un appel en cours coupe.
- **Batterie** : se vide écran allumé et en appel (vite pour la brique de 1985), barre visible sous l'objet.

## Commandes des téléphones (maîtres du jeu)

| Commande | Effet |
| --- | --- |
| `/telephone inconnu sms <numéro> <message>` | SMS d'un **numéro inconnu** (impossible d'y répondre). |
| `/telephone inconnu appel <numéro> <ligne 1 \| ligne 2 \| …>` | Appel d'un numéro inconnu : sonnerie grave et déréglée ; une fois décroché, les lignes arrivent une à une, puis ça raccroche. |
| `/telephone hanter <numéro> [secondes] [message]` | Le téléphone se dérègle : parasites, image déchirée, un ballon rouge qui remonte l'écran, message en rouge, grésillement. |
| `/telephone journal Titre \| texte` | Publie un article dans le Journal des smartphones et les Infos VOXA des téléphones à clapet. |
| `/telephone numeros <joueur>` | Les numéros que porte un joueur. |
| `/telephone recharger` | Recharge le téléphone en main. |

## Développement

- Images, modèles 3D et textes : `python3 era-phones/tools/gen_assets.py` (numpy, pillow).
- Compilation, GameTests et test client (captures dans le journal) : workflow `Era Phones` sur GitHub Actions, qui ne
  tourne que quand `era-phones/` change. En local : `cd era-phones && ../gradlew build`.
- Simple Voice Chat est **optionnel** (API en `compileOnly`) : sans lui, tout marche sauf la voix.
