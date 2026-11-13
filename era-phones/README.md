# Époque RP

Mod NeoForge 1.21.1 **séparé** (son propre `.jar`, `epoque-rp-<version>.jar`, sans Arcanes Oubliés ni Iron's Spells)
pour un serveur de jeu de rôle : téléphones d'époque, objets du quotidien des années 80 (lampe torche, talkie-walkie,
poste de radio, magnétoscope et cassettes, journal), commandes de RP et outils des maîtres
du jeu. L'identifiant interne du mod reste `eraphones` : les objets déjà dans les mondes sont conservés. Tout reste dans le personnage : on ne voit jamais un pseudo
de compte, seulement des numéros, les noms enregistrés dans les répertoires et les noms des personnages.

Contenu original (noms, modèles, sonneries, icônes) : aucune marque réelle ; l'opérateur fictif s'appelle **VOXA**.

## Le menu créatif

Quatre onglets : **Époque RP · Téléphones**, **· Lumière & radio** (lampe torche, piles, talkie-walkie, poste de
radio), **· Son & image** (magnétoscope, cassette, baladeur, caméscope) et **· Vie quotidienne** (appareil photo, cabine,
vélos, lampadaire, répondeur, feuille de papier, journal, cadenas, clés, lecteur de carte, badge).

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
| Cabine téléphonique (bloc, 3 de haut) | Assez haute pour s'y tenir debout. Cadre alu, vitres fumées, enseigne lumineuse « TELEPHONE » qui éclaire la rue, publiphone en acier avec combiné. Chaque cabine a son numéro (555-9xxx, affiché sur l'écran du publiphone). Clic droit : le publiphone s'ouvre ; compose au clavier (souris ou clavier de l'ordinateur), « Appeler » : les appels sont gratuits. Appelée (par un joueur ou par un MJ avec un numéro masqué), elle sonne fort dans la rue : celui qui décroche (clic droit) répond. S'éloigner de la cabine coupe l'appel. |
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
| `/panne <rayon> [secondes]` | Coupe le courant autour de toi (jusqu'à `/panne fin`, ou pendant le temps donné) : un bruit de coupure, les lampadaires grésillent et s'éteignent, les télés s'éteignent (et se rallument au retour du courant), les magnétoscopes s'arrêtent, les cabines téléphoniques s'éteignent et n'ont plus de tonalité (elles ne sonnent plus). La lampe torche, elle, marche toujours. |
| `/panne clignote <rayon> [secondes]` | Le courant faiblit : la nuit, les lampadaires clignotent et grésillent (10 s par défaut). |
| `/panne fin` | Rétablit le courant partout. |

Les coupures ne survivent pas à un redémarrage du serveur.

### Commandes des maîtres du jeu pour ces objets

| Commande | Effet |
| --- | --- |
| `/narration <texte>` | Narration sur l'écran de tous les joueurs (bandes noires de cinéma, texte qui s'écrit) et dans leur chat. |
| `/narrationalone <joueurs> <texte>` | La même chose pour un seul joueur (ou ceux choisis). |
| `/narrationchat <texte>` | Narration seulement dans le chat de tous (rien à l'écran). |
| `/narrationchatalone <joueurs> <texte>` | Pareil, pour un ou quelques joueurs. |
| `/cassette liste` | Les fichiers MP3 disponibles sur le serveur (`config/eraphones/cassettes/`, où arrivent aussi ceux gravés depuis le jeu). |
| `/cassette graver <fichier> <titre>` | Donne une cassette de ce fichier, avec ce titre sur l'étiquette. Elle ne peut plus être modifiée. |
| `/radio emettre <fréquence>` · `/radio arreter` | Passer à l'antenne sur une fréquence (ta voix sort des radios réglées dessus), ou s'arrêter. |
| `/lampe <joueurs> eteindre` · `clignoter [secondes]` · `casser` · `reparer` | Éteindre leurs lampes torches, les faire clignoter (10 s par défaut, avec de petits grésillements), les casser (elles ne s'allument plus, « Cassée » dans la description) ou les réparer. |
| `/journal imprimer <exemplaires> Titre \| texte` | Imprime le journal (jusqu'à 64 exemplaires). |

## Outils d'événement

Des outils faits pour un grand événement d'horreur en ville, pensés pour un maître du jeu **seul**.

| Chose | Comment ça marche |
|---|---|
| `/me ferme les yeux` | Dans un `/me`, « ferme les yeux » (ou « ferme ses yeux », « closes his eyes »…) ferme les yeux du joueur sur son écran : les paupières descendent, tout est noir pendant 6 s, puis elles se rouvrent avec un clignement. Les maîtres du jeu reçoivent « X ferme les yeux » dans le chat. |

### Déclencheurs de zone : `/declencheur`

Une zone invisible ; quand un joueur y entre, ce qui a été prévu lui arrive tout seul. Les joueurs en créatif ou en spectateur ne déclenchent rien (le maître du jeu peut construire tranquille). Gardé avec le monde.

1. `/declencheur pos1` puis `/declencheur pos2` : les deux coins (là où tu es, ou `pos1 x y z`).
2. `/declencheur creer <nom>` : la zone (affichée en poussière rouge pendant 10 s).
3. `/declencheur action <nom> ...`, autant de fois que voulu, dans l'ordre :
   - `son <son>` (derrière le joueur), `ballon`, `yeux`, `lampe` (sa lampe torche casse) ;
   - `brouillard | coupure | sang | peur | tremblement | flotter [secondes]` ;
   - `murmure <texte>`, `narration <texte>` ;
   - `panne <rayon> <secondes>` ou `clignote <rayon> <secondes>` (le quartier autour de la zone) ;
   - `sms <numéro|inconnu> <texte>` (sur le téléphone qu'il porte) ;
   - `commande <commande>` (n'importe quelle commande ; `{joueur}` = son pseudo, `@s` = lui) ;
   - `attendre <secondes>` : une pause avant la suite.
4. `/declencheur mode <nom> une_fois | chaque_joueur | toujours [secondes entre deux]` (par défaut : une seule fois).
5. `/declencheur tester <nom> [joueur]`, `liste`, `info <nom>`, `montrer [secondes]` (voir les zones), `actif <nom> true|false`, `reset <nom>` (pour qu'il se relance), `vider <nom>`, `supprimer <nom>`.

Exemple : `/declencheur action ruelle son rire`, `/declencheur action ruelle attendre 3`, `/declencheur action ruelle ballon`.

**Important :** les joueurs en **créatif** ou spectateur ne déclenchent rien (pour que le staff puisse circuler). Pour essayer une zone toi-même : `/declencheur moi` (tu la déclenches même en créatif ; à nouveau pour arrêter), ou `/declencheur tester <nom>`, ou passe en survie. En mode `une_fois`, la zone ne part qu'une fois : `reset <nom>` pour la réarmer. Quand une zone part, le staff le voit au-dessus de sa barre (« Déclencheur X lancé par Y »).

## Inventaire limité

Les joueurs (sauf en créatif ou en spectateur) n'ont **pas d'inventaire** : seulement les **5 premières cases** de leur barre, dessinée plus courte, centrée, plus la main secondaire. La molette fait le tour de ces 5 cases. Les autres cases sont grisées et fermées dans tous les écrans (inventaire, coffres…). Ce qui arrive dans une case fermée va dans une case libre de la main, sinon par terre. On ne ramasse rien s'il n'y a pas de place.

Configuration serveur (`eraphones-server.toml`) : `limitedInventory` (true/false), `hotbarSlots` (1 à 9, 5 par défaut).

## Baladeur et casque

Un **baladeur** à cassette (onglet Son & image) : une cassette dedans à la fois. Clic droit : lecture ou pause. La musique n'est entendue **que par celui qui l'écoute** (dans son casque, avec Simple Voice Chat), et elle continue tant que le baladeur est sur lui (en main ou dans sa barre). Pendant l'écoute, les autres voient un **casque à mousse orange** sur sa tête. Pour mettre une cassette : cassette dans l'autre main + clic droit (l'ancienne revient dans cette main), ou dans l'inventaire, clic droit avec la cassette sur le baladeur. Accroupi + clic droit (ou clic droit à vide sur lui dans l'inventaire) : la cassette sort. La cassette garde l'endroit où elle s'est arrêtée ; arrivée au bout, elle repart du début. On voit la cassette à travers la vitre du baladeur.

## Taille des joueurs : `/taille`

`/taille <joueurs> <cm>` (de 50 à 400 cm, 180 = normal), `/taille <joueurs> normale`, `/taille` (sa propre taille, pour tout le monde). Tout suit la taille : le corps dessiné, la hitbox, la hauteur des yeux (donc ce qu'on voit), la caméra à la 3e personne, la portée des mains, la marche qu'on peut monter, un peu la vitesse et le saut, la chute sans se blesser. Gardé même après la mort et la déconnexion. Permission : comme `/horreur`.

## Outils du staff (lot 2)

### Atelier d'armes : `/arme`
Tiens un objet (épée, hache, armure, n'importe quoi) et tape `/arme`. L'atelier s'ouvre : l'objet en haut, quatre onglets, et à droite l'**aperçu** de l'objet tel qu'il sera (nom, lore, stats, effets), mis à jour pendant que tu écris.
- **Général** : son nom, incassable, brillance. Pastilles de couleur : un clic ajoute le code (`&c` rouge, `&6` or…), plus `&l` gras, `&o` italique, `&n` souligné, `&r` normal.
- **Lore** : une grande zone de texte, jusqu'à **16 lignes** (Entrée pour changer de ligne), avec les mêmes pastilles de couleur.
- **Stats** : dégâts, vitesse d'attaque (comme affichés sous l'objet : une main nue = 1 et 4), armure, robustesse, résistance au recul, vitesse (%), PV max (+), portée (+). Une case vide = rien.
- **Effets %** : jusqu'à 8 lignes. Pour chacune :
  - **Quand** : *Frappe* (quand celui qui le tient tape) ou *Défense* (quand celui qui le porte ou le tient est touché) ;
  - **l'effet** : clic dessus ouvre la liste de **tous les effets du jeu et de tous les mods installés**, avec leur icône, qu'on fait défiler à la molette, et une **barre de recherche** (Entrée prend le premier). En tête, les actions de l'atelier : enflamme, foudre, recul, vol de vie, dégâts ±, épines, soin, gel ;
  - **Sur** : *Autre* (celui qu'on frappe, ou celui qui nous frappe) ou *Soi* ;
  - la **chance en %**, la **force** (niveau de l'effet, ou points, ou % pour le vol de vie), les **secondes**.
  - Sous l'objet : « ✦ 5 % en frappant → l'autre : Poison II (5 s) ».

### Porter quelqu'un
Accroupi + clic droit main vide sur un joueur : il reçoit une demande dans le chat ([Accepter] ou `/porter accepter`). Il est alors porté dans les bras, allongé, devant le porteur, qui marche plus lentement. Accroupi + clic droit main vide (ou `/porter lacher`) : on le pose. Le porté descend en s'accroupissant. Staff : `/porter <porteur> <porté>` sans demander.

### Cadenas et clés
Un **cadenas** (clic droit sur une porte, trappe, portillon, coffre ou tonneau) le ferme à clé et donne sa **clé**. Avec une clé dans l'autre main, le cadenas prend le secret de cette clé (une seule clé pour plusieurs portes). Sans la bonne clé en main : « C'est fermé à clé ». Accroupi + clic droit avec la clé : on enlève le cadenas. Impossible à casser ou à faire exploser (sauf en créatif). On voit le cadenas accroché. Staff : `/cle nouvelle`, `/cle copier` (copie la clé en main).

### Lecteur de carte et badges
Un **lecteur de carte** se pose sur un mur à côté d'une porte (on ne peut pas le casser en survie). `/badge creer <nom>` donne un **badge**. En créatif, passe le badge sur le lecteur pour qu'il le connaisse (encore une fois : il l'oublie ; accroupi main vide : il oublie tout). Un joueur qui passe un badge connu : bip vert, les portes (même en fer), portillons et trappes à 2 blocs s'ouvrent 5 secondes. Badge inconnu : bip rouge, « Accès refusé ». `/badge copier` copie le badge en main.

### `/vitesse` et `/hp`
`/vitesse <joueurs> <fois>` (1 = normal, 0.5 = moitié, 2 = double) ou `normale`. `/hp <joueurs> <points>` (20 = normal = 10 cœurs) ou `normal`. Gardés après la mort et la déconnexion. Les **effets** qu'un joueur a quand il meurt (donnés par `/effect` ou autrement) lui sont rendus quand il réapparaît, avec le temps qui leur restait.

## Jauge de peur, photos épinglées, emotes

### Jauge de peur : `/peur jauge`
Chaque joueur a une peur de 0 à 100 %. Elle **monte** dans le noir (une lampe torche allumée, une torche ou une lanterne en main comptent comme de la lumière), seul, la nuit dehors, blessé, avec des monstres autour, et d'un coup quand le staff lui envoie quelque chose (`/horreur` sons, murmure, coupure, sang, ballon, `/peur`, coups reçus). Elle **descend** à la lumière et à plusieurs. À partir d'un tiers : le cœur bat, de plus en plus vite, les bords de l'écran s'assombrissent au rythme du cœur ; au-delà des deux tiers : la vue tremble et se brouille un peu. Un petit cœur et un tracé de moniteur en bas à gauche la montrent.
**Le staff** (ceux qui peuvent utiliser `/horreur`), et les joueurs en créatif ou spectateur, **ne ressentent rien**.
- `/peur jauge on` / `/peur jauge off` : l'active ou la coupe pour tout le serveur (gardé avec le monde) ;
- `/peur jauge` ou `/peur jauge info` : l'état, et la peur de chaque joueur ;
- `/peur jauge <joueurs> <0-100>` : la régler à la main.
- `/peur jauge test` : **toi (staff), tu la ressens comme un joueur** pour l'essayer (à nouveau : arrêter) ; `/peur jauge test <0-100>` règle la tienne directement (même si la jauge est coupée).

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
| `/pseudos cacher` · `/pseudos afficher` | Les pseudos au-dessus des têtes sont **cachés par défaut** ; `/pseudos afficher` les réaffiche (et `/pseudos cacher` les recache), pour tout le monde : passe par une équipe du tableau des scores (« nom jamais visible »), que le jeu de base respecte sur chaque client, avec ou sans le mod (Essential compris, qui s'accroche au nom du jeu). Les joueurs sont alors dans l'équipe `eraphones_rp` (un joueur ne peut être que dans une équipe). Maîtres du jeu. |
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
