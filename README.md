# Arcanes Oubliés

Addon pour **Iron's Spells 'n Spellbooks** (Minecraft 1.21.1, NeoForge 21.1.221+), pour notre serveur RP.
La dernière version pour Minecraft 1.20.1 (Forge) est la 0.9.0 (commit `6359b13` de l'historique).
La liste complète des écoles et des sorts prévus est dans [`ADDONS_SPELLS.txt`](ADDONS_SPELLS.txt).

## Contenu de la bêta 1.0.0 : écoles de l'Or, de l'Explosion, du Livre, d'Errethys, Caractéristique Physique, Magie de Boss, Magie Runique, Magie de Glyphe, Magie des Jetons, Magie de Mousse, Cuivre et Rouille et Magie des Murs

Quatre nouvelles écoles complètes dans Iron's Spells, chacune avec ses attributs « Puissance des sorts » et
« Résistance », ses parchemins et son livre de sorts 3D (+10 % puissance de l'école, +200 mana ; 10 emplacements,
12 pour le Grimoire Vivant), plus l'école d'invocation Errethys (5 sorts, pas encore de livre) et les techniques de
combat de Caractéristique Physique (38 techniques pour 9 styles d'armes, ce n'est pas de la magie).

**Onglet créatif** : un seul onglet « Arcanes Oubliés », rangé école par école (le livre de l'école puis le parchemin
de chacun de ses sorts à tous les niveaux, dans l'ordre ; style par style pour Caractéristique Physique). Une colonne de
boutons à gauche de l'inventaire (toutes les écoles, puis une par école) n'affiche qu'une école à la fois.

**Parchemins** : ce sont les parchemins d'Iron's Spells, seule la couleur change d'une école à l'autre (comme les
parchemins de feu, de glace, etc. d'Iron's). La licence d'Iron's interdit de reprendre ses images : l'addon ne
contient que les palettes de couleurs, et le jeu recolore lui-même le parchemin d'Iron's au chargement
(`tools/scroll_assets.py`).

**Invocations** (Sentinelles d'Or, Bibliothèque Interdite, Pile Volante, Dragon d'Origami et tous les sorts
d'Errethys) : relancer le sort renvoie ses invocations (relance d'Iron's, instantanée et gratuite, visible sur
l'écran) ; le temps de recharge démarre quand elles sont toutes parties (renvoyées, tuées ou à la fin de leur temps).

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

### Livre (10 sorts + 6 sorts bonus)

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
| **Tempête de Papier** (Paper Tempest, bonus) | Une tornade de pages (7 blocs de haut) se lève là où le lanceur regarde et suit son regard : elle aspire les ennemis, les lacère deux fois par seconde, les aveugle, avale les flèches, puis projette tout au loin en se dissipant. 6 à 12 s. |
| **Pile Volante** (Flying Stack, bonus) | Une pile de papier soulève le lanceur : il vole là où il regarde (avancer, regarder en haut ou en bas pour monter ou piquer, pas de côté ; sans touche elle flotte sur place). S'accroupir pour descendre, avec une chute lente ; clic droit dessus pour remonter (elle attend 5 s). 11 à 16 blocs/s, 1 à 3 min ; on peut l'abattre. |
| **Dragon d'Origami** (Origami Dragon, ultime bonus) | Une grande feuille de papier apparaît au-dessus du lanceur et se plie toute seule, pli après pli, en un dragon de 8 blocs aux cornes et aux crêtes dorées. Il chasse ce que le lanceur combat (sinon les monstres autour) : il tourne autour, crache un cône de papier tranchant qui ralentit, puis fond sur sa proie en piqué et la projette. À la fin il se replie en feuille et éclate en confettis. 18 à 24 s, invulnérable. |

Livre : **Grimoire Vivant** (Living Grimoire) : couverture violette aux veines de runes qui pulsent, un œil qui
regarde autour de lui et cligne, des pages qui bougent toutes seules. Focus de l'école : livre.

### Errethys (invocations de créatures de mana)

Des animaux de mana bleu ciel transparent, aux veines de lumière qui brillent de l'intérieur. Ils restent **paisibles**
tant que le lanceur ne se bat pas, même à côté d'un monstre : ils n'attaquent que **ce que le lanceur frappe** (au
corps à corps, à l'arc ou avec un sort) et **ce qui attaque le lanceur**. Ils ne touchent jamais ses alliés, ne
laissent rien en mourant et se dissolvent en étincelles quand leur temps est écoulé.

| Sort | Effet |
|---|---|
| **Rat de Mana** (Mana Rat) | Un rat rapide (queue qui ondule) qui suit le lanceur et mord ses ennemis, 30 s ou plus. |
| **Piranha de Mana** (Mana Piranha) | Un piranha qui ne vit que dans l'eau : il apparaît dans l'eau que regarde le lanceur (ou la plus proche, 6 blocs ; sinon le sort échoue), y file comme une flèche, suit le lanceur le long de l'eau et mord les ennemis dans l'eau ou au bord, deux fois plus vite que les autres créatures, ses dents de lumière étincelantes. **Frénésie** : chaque morsure d'affilée sur la même proie (en moins de 3 s) mord 25 % plus fort, jusqu'au double. Échoué sur la terre ferme, il se débat puis se dissout au bout de 3 s. |
| **Dromadaire de Mana** (Mana Dromedary) | Un dromadaire sellé : clic droit pour le monter, avec la ruée du chameau ; un second joueur peut monter derrière le lanceur. 2 à 4 min. |
| **Serpent de Mana** (Mana Serpent) | Un serpent qui ondule en S ; sa morsure empoisonne (Poison II au niveau 4) et ralentit. |
| **Corbeau de Mana** (Mana Crow) | Un corbeau qui vole comme un oiseau : battements d'ailes, vol plané en descente, cercles au-dessus du lanceur, piqués sur les ennemis. |

Chaque sort n'invoque qu'une seule créature (plus forte qu'un membre des anciennes nuées) ; le relancer la renvoie.
Les créatures de sorts différents peuvent se battre ensemble.

Focus de l'école : cristaux de prismarine.

### Caractéristique Physique (techniques de combat, pas de la magie)

Des coups et des mouvements de combattant pour se battre sans magie, rangés dans Iron's comme des sorts (dans un livre
de sorts, ou incrustés dans une arme à l'Enclume Arcanique d'Iron's). Ce n'est pas de la magie : la force d'une
technique vient de son niveau et de la **Puissance physique** (jamais de la puissance des sorts), les armes ajoutent
une part de leurs dégâts (Tranchant compris), les dégâts sont physiques (l'armure et la **Résistance physique** les
réduisent) et le Sceau du Silence ne les empêche pas. Chaque coup est calé sur l'animation du corps : presque toutes
sont propres à l'addon (hanches et épaules qui tournent derrière le coup, la main gauche qui vient saisir les armes à
deux mains, la tête qui garde ton regard), seules la tranche, la coupe montante et les deux premières techniques de faux
reprennent les animations de combat d'Iron's. Les coups laissent une traînée pâle dans l'air (lame, tour complet,
estoc, onde d'un coup de poing). Les armes de jet et de tir (haches, javelots, couteaux, flèches, carreaux) sont des
projectiles qu'on ne peut pas ramasser et qui ne touchent jamais les alliés. Coût en mana faible, recharges courtes.

| Technique | Effet |
|---|---|
| **Coup de Poing** (Punch) | Le poing armé à la hanche, puis un direct avec les hanches et les épaules derrière : frappe fort l'ennemi le plus proche devant (3 blocs) et le repousse. |
| **Rafale de Coups de Poing** (Punch Flurry) | Pieds plantés, 7 directs gauche-droite depuis la garde sur tout ce qui est devant, puis un direct armé qui repousse. |
| **Tranche** (Slash) | Une entaille en diagonale à travers tout un arc devant (3,2 blocs), à la lame ou au tranchant de la main. |
| **Rafale de Tranches** (Slash Flurry) | Trois entailles en aller-retour puis une grande taille horizontale qui repousse, en une seconde. |
| **Dash** | 4 à 5 blocs d'un coup dans le sens où l'on se déplace déjà (en arrière ou de côté pour esquiver), sinon droit devant ; s'arrête net. |
| **Saut** (Leap) | Un saut de 3 blocs de haut (6 au niveau 3), en gardant l'élan ; la retombée ne fait pas de dégâts (seule une chute plus profonde que le saut en fait) ; pieds au sol seulement. |
| **Esquive** (Evade) | Un grand bond en arrière pour sortir d'un combat : 6 à 8 blocs, en arc bas, sans dégâts de chute ; pieds au sol seulement. |
| **Ruée** (Rush) | Contrairement au Dash, on fonce sur l'ennemi qu'on regarde (10 à 14 blocs) en le suivant s'il bouge, et on s'arrête juste devant lui d'un coup d'épaule qui le fait chanceler. Sans personne en vue : une longue course basse droit devant (7 blocs). |
| **Faux 1 : Fauchage** (Scythe 1: Reap) | Armé lent puis balayage au ras du sol à travers un grand demi-cercle (4,5 blocs) : ralentit les victimes. |
| **Faux 2 : Danse de la Faucheuse** (Scythe 2: Reaper's Dance) | Une taille devant, puis un tour complet qui fauche tout autour (4,5 blocs) et projette. |
| **Faux 3 : Crochet de la Faucheuse** (Scythe 3: Reaper's Hook) | La lame accroche les ennemis loin devant (5,5 blocs, cône étroit) et les traîne jusqu'à tes pieds. |
| **Épée 1 : Coupe Montante** (Sword 1: Rising Cut) | Une taille de bas en haut qui soulève les ennemis d'environ un bloc et demi. |
| **Épée 2 : Fente** (Sword 2: Lunge) | Une fente d'escrime : la jambe avant part, le corps descend et l'estoc traverse toute la ligne (3,5 blocs). |
| **Épée 3 : Désarmement** (Sword 3: Disarm) | Lie et arrache l'arme de l'ennemi en face : une créature la perd quelques secondes (rien n'est perdu), un joueur perd sa prise (Faiblesse, bouclier écarté, objet en main inutilisable). |
| **Dague 1 : Estocs Rapides** (Dagger 1: Quick Stabs) | Trois estocs courts depuis la hanche sur l'ennemi le plus proche ; il saigne, et le troisième creuse la blessure (Saignement II). |
| **Dague 2 : Contournement** (Dagger 2: Flank) | Un bond autour de l'ennemi en face (5 blocs) pour finir dans son dos (ou sur son flanc), puis un coup : dans le dos, x1,75 et saignement. |
| **Dague 3 : Coup de Grâce** (Dagger 3: Coup de Grâce) | Dague retournée, enfoncée de haut en bas : x2,5 contre un ennemi à moins d'un tiers de sa vie, x1,6 s'il saigne, est cloué ou ralenti. |
| **Marteau 1 : Coup Sonné** (Hammer 1: Concussive Blow) | Un coup à plat à la tête : l'ennemi pivote d'un demi-tour (la vue d'un joueur tourne de 180°), étourdi (nausée) et ralenti. |
| **Marteau 2 : Séisme** (Hammer 2: Earthshaker) | Le marteau abattu au sol : l'onde blesse, soulève et ralentit tous ceux qui sont au sol à 4 blocs autour de l'impact. |
| **Marteau 3 : Bélier** (Hammer 3: Battering Ram) | Le marteau enfoncé droit devant : l'ennemi est projeté plusieurs blocs en arrière, second coup s'il percute un mur. |
| **Hache 1 : Fendoir** (Axe 1: Cleave) | Coup vertical sur l'ennemi le plus proche ; un bouclier levé est écarté (5 s sans pouvoir le relever) avant que la hache ne morde. |
| **Hache 2 : Lancer de Hache** (Axe 2: Axe Throw) | Une hache lancée en tournoyant là où tu regardes (celle que tu tiens si c'est une hache). |
| **Hache 3 : Brise-Armure** (Axe 3: Armor Splitter) | Un coup en biais qui fend l'armure : Armure Fendue (-25 % d'armure, -50 % dès le niveau 4) et usure de chaque pièce. |
| **Lance 1 : Estoc Long** (Spear 1: Long Thrust) | Un estoc à deux mains qui traverse toute la ligne jusqu'à 5 blocs. |
| **Lance 2 : Charge** (Spear 2: Charge) | Une course de 7 blocs lance baissée : le premier ennemi touché est embroché et projeté, la charge s'arrête là. |
| **Lance 3 : Javelot** (Spear 3: Javelin) | Une lance projetée loin et vite qui traverse le premier ennemi et touche le suivant, et les ralentit. |
| **Épée Longue 1 : Taille de Colère** (Longsword 1: Wrath Cut) | Une grande taille en diagonale à deux mains dans un large arc (3,6 blocs) qui fait chanceler. |
| **Épée Longue 2 : Demi-Lune** (Longsword 2: Half-Moon) | Un balayage à plat en demi-cercle jusque sur les côtés (4 blocs) qui repousse. |
| **Épée Longue 3 : Parade-Riposte** (Longsword 3: Parry and Riposte) | Une garde d'environ une seconde : le premier coup porté de face est paré (les flèches déviées), l'attaquant est déséquilibré et reçoit la riposte. Ne pare pas la magie. |
| **Arc 1 : Flèche Téléguidée** (Bow 1: Guided Arrow) | Une flèche qui suit ton regard pendant 3 à 5 secondes, vers la créature que tu regardes. |
| **Arc 2 : Volée** (Bow 2: Volley) | Cinq flèches d'un coup en éventail de 40 degrés. |
| **Arc 3 : Pluie de Flèches** (Bow 3: Arrow Rain) | Des flèches tirées vers le ciel qui retombent une seconde plus tard là où tu regardais (cercle de 3,5 blocs). |
| **Arbalète 1 : Carreau Perforant** (Crossbow 1: Piercing Bolt) | Un carreau très rapide qui traverse 3 ennemis et touche un quatrième (plus avec Perforation). |
| **Arbalète 2 : Rafale de Carreaux** (Crossbow 2: Bolt Burst) | Trois carreaux à un cinquième de seconde d'intervalle, chacun là où tu vises. |
| **Arbalète 3 : Harpon** (Crossbow 3: Harpoon) | Un carreau barbelé sur une corde de ~24 blocs : ramène l'ennemi à tes pieds, ou te tire jusqu'au mur touché. |
| **Couteaux 1 : Lancer Précis** (Throwing Knives 1: Precise Throw) | Un couteau rapide et droit qui fait saigner ; x1,5 à 8 blocs ou plus. |
| **Couteaux 2 : Éventail de Lames** (Throwing Knives 2: Fan of Knives) | Cinq couteaux en éventail de 60 degrés. |
| **Couteaux 3 : Clouage** (Throwing Knives 3: Pinning Throw) | Un couteau dans le pied : impossible de marcher ou de sauter pendant 2,4 à 4 secondes (Cloué). |

Les techniques d'armes ne demandent pas d'avoir l'arme en main (à vous de les donner aux bons personnages), mais les
techniques de mêlée frappent surtout avec les dégâts de l'arme tenue : à mains nues, une technique de faux fait peu de
mal. Les tirs ne consomment pas de flèches ; la Puissance d'un arc tenu renforce les techniques d'arc, la Perforation
d'une arbalète allonge le Carreau Perforant. Focus de
l'école : cuir. Les coûts, recharges et la puissance de chaque technique se règlent dans la configuration serveur
d'Iron's, comme pour tous les sorts.

### Magie de Boss

Des attaques dignes d'un boss final de RPG. Chacune est **annoncée** : dès le lancement, la zone menacée est dessinée
au sol (cercle, anneau, ligne ou cône) sous des cercles de runes qui tournent, épouse le relief bloc par bloc, se
remplit comme un compte à rebours jusqu'à l'impact, son bord pulse de plus en plus vite et des symboles s'en élèvent ;
les attaques venues d'en haut ont en plus un grand cercle runique dans le ciel. La dernière seconde, des étincelles
soulignent le bord (lisible même dans l'herbe haute) et un son monte. **3,5 s** séparent l'apparition d'une zone de
son impact (réglable) ; seuls ceux qui sont **encore dans la zone** au moment de l'impact sont touchés, sortir à temps
suffit. Le lanceur et ses alliés ne sont jamais touchés, et le lanceur peut bouger pendant que l'attaque se prépare.
Plusieurs attaques ont plusieurs temps (impact puis onde de choc, coup puis éruption, trois anneaux l'un après
l'autre), chaque temps ayant sa propre zone et son propre compte à rebours.

| Sort | Effet |
|---|---|
| **Météore Écrasant** (Crushing Meteor) | Un cercle de runes s'ouvre dans le ciel au-dessus de la cible et marque le sol ; un météore en flammes s'y écrase (gros dégâts, brûlure, recul), puis une onde de choc part en anneau autour du cratère une seconde plus tard. |
| **Lame du Jugement** (Judgment Blade) | Une lame de lumière géante se dresse sur une longue bande marquée devant le lanceur (18 à 26 blocs) et s'abat dessus ; la bande jaillit en piliers de lumière une seconde après. |
| **Rayon d'Anéantissement** (Annihilation Beam) | Un cercle de runes se dresse debout devant le lanceur et se charge, puis un rayon d'énergie balaie la bande marquée (28 à 32 blocs) pendant 2 s. |
| **Nova Triple** (Triple Nova) | Trois zones de feu autour du lanceur : le disque central explose, puis un anneau, puis un anneau plus large (13 blocs), l'un après l'autre. |
| **Pluie d'Étoiles** (Starfall) | De petits cercles se marquent en cascade autour de la cible ; une étoile tombe dans chacun à son tour (7 à 15 étoiles). |
| **Faille Sismique** (Seismic Rift) | Une faille se marque depuis les pieds du lanceur et s'ouvre tronçon par tronçon en projetant en l'air ceux qui s'y trouvent : le danger avance le long de la ligne. |
| **Orbe du Néant** (Void Orb) | Une énorme sphère de ténèbres roule lentement sur un chemin marqué (la toucher blesse, s'en approcher attire) ; au bout, elle s'effondre en aspirant tout autour, puis explose. |
| **Croix de Lumière** (Cross of Light) | Une grande croix de lumière se marque autour de la cible et jaillit en piliers, puis une seconde croix tournée d'un huitième de tour. |
| **Prison Runique** (Rune Prison) | Un anneau se marque autour de la cible, puis un mur de runes s'y dresse 10 à 16 s : quiconque n'est pas du camp du lanceur et tente de le franchir est blessé et repoussé. |
| **Jugement Dernier** (Last Judgment, ultime) | Un cercle de runes géant s'ouvre dans le ciel ; toute l'arène autour du lanceur (15 blocs) est condamnée, sauf trois petits refuges verts. Cinq météores tombent pendant le compte à rebours, puis le jugement frappe tout ce qui est hors des refuges. |

Livre : **Codex du Tyran** (cuir rouge sang, cercle de runes ardent sur la couverture, ferrures noires et dorées).
Focus de l'école : étoile du Nether. Le lanceur a ses propres animations (bras levés vers le ciel, lame abattue,
poings au sol, mains en avant pour le rayon, bras écartés pour la prison).

**Option staff** : la durée d'avertissement se règle dans `<monde>/serverconfig/arcanes_oublies-server.toml`,
section `[boss]`, `warningTicks` (70 ticks = 3,5 s par défaut, de 40 à 200).

### Magie Runique

Des runes **gravées** dans le sol, dans la pierre et dans les armes. On les pose à l'avance et elles agissent dans la
durée : la moitié protège et soigne, l'autre moitié frappe. Les runes brillent en violet dans de la deepslate
taillée (de vrais blocs de Minecraft assemblés en pierres levées et en murs). Rien à voir avec la Magie de Boss : ce
sont des sorts de joueur, sans compte à rebours.

| Sort | Effet |
|---|---|
| **Rune de Garde** (Ward Rune, soutien) | Un cercle de runes gravé aux pieds du lanceur, sous un rideau de lumière ; ses alliés à l'intérieur ont Résistance (II au niveau 4) pendant 8 à 14 s. |
| **Pierre de Guérison** (Mending Runestone, soutien) | Une pierre levée aux runes orchidée surgit à l'endroit visé ; toutes les 2 s elle soigne les alliés autour d'elle (5,5 blocs). Une seule par lanceur. |
| **Rune Tranchante** (Edge Rune, soutien) | Une rune gravée sur l'arme du lanceur et des alliés proches (6 blocs) : chaque coup au corps à corps (arme, poing, technique de combat) fait des dégâts en plus, 24 à 40 s. |
| **Sentier Runique** (Runic Path, soutien) | Un chemin de dalles runiques qui s'allument en vague devant le lanceur (10 à 16 blocs) : Vitesse II (III au niveau 5) et Saut pour les alliés dessus. |
| **Mur Runique** (Runic Bulwark, soutien) | Un arc de dalles de pierre gravées jaillit du sol devant le lanceur : solide, il arrête les passants, les flèches et les sorts, puis s'effondre (7 à 11 s). |
| **Rune Piège** (Snare Rune) | Une petite rune qui brille à peine, posée à l'endroit visé ; le premier ennemi qui marche dessus la fait éclater en pics de pierre (dégâts, ralentissement, projection). Trois au plus à la fois. |
| **Éclats Runiques** (Rune Shards) | Un éventail de 3 à 5 éclats de pierre gravés ; chacun traverse un ennemi avant de se briser. |
| **Rune de Pétrification** (Petrifying Rune) | La pierre monte le long des jambes de la cible : ni marche ni saut pendant 3 à 5 s, la pierre la broie, puis vole en éclats en la blessant une dernière fois. |
| **Éruption de Menhirs** (Menhir Surge) | Une ligne de menhirs gravés jaillit du sol l'un après l'autre droit devant le lanceur, projetant et blessant ce qui se trouve dessus. Un mur ou une falaise arrête la ligne. |
| **Cercle des Anciens** (Circle of the Elders, ultime) | Huit grandes pierres runiques s'élèvent en cercle autour du lanceur (8 blocs) : chaque seconde, ses alliés à l'intérieur sont soignés et protégés, et des éclairs runiques frappent jusqu'à 6 ennemis à l'intérieur. |

Livre : **Codex des Runes Anciennes** (couvertures de pierre grise gravée, cercle de runes lumineux, coins de fer,
gemme violette). Focus de l'école : briques de pierre sculptées. Les runes sont dessinées pour l'addon (aucun
alphabet réel).

### Magie de Glyphe

Des glyphes **élémentaires écrits dans la lumière**, en temps réel : le lanceur trace le glyphe dans l'air devant lui
(on voit la plume de lumière dessiner chaque trait), et le sort part dès que le glyphe est terminé. Six éléments, six
glyphes dessinés pour l'addon : **Feu**, **Givre**, **Orage**, **Marée**, **Vent** et **Pierre**. Rien de gravé, rien
qui dure dans le sol comme la Magie Runique : ici tout est instantané, élémentaire, et se combine.

**Marques et réactions** : chaque coup inscrit la marque de son élément sur la victime (son glyphe flotte au-dessus de
sa tête pendant 6 s). Un glyphe d'un **autre** élément qui la frappe efface la marque et déclenche la réaction des
deux éléments (son nom s'affiche à l'écran du lanceur) :

| Réaction | Éléments | Effet |
|---|---|---|
| **Fonte** | Feu + Givre | Le coup fait 60 % de dégâts en plus. |
| **Surcharge** | Feu + Orage | Explosion autour de la victime : dégâts et projection. |
| **Vapeur** | Feu + Marée | Colonne de vapeur 4 s : dégâts et Cécité. |
| **Tempête de feu** | Feu + Vent | Tornade de flammes 4 s : dégâts, brûlure, aspiration. |
| **Magma** | Feu + Pierre | Mare de lave 4 s : dégâts, brûlure, ralentissement. |
| **Supraconduction** | Givre + Orage | Décharge glacée autour : dégâts et Faiblesse II. |
| **Gel** | Givre + Marée | La victime est prise dans un bloc de glace 2,5 s (ni marche ni saut). |
| **Blizzard** | Givre + Vent | Tourbillon de neige 4 s : dégâts, ralentissement, gel. |
| **Brisure** | Givre + Pierre | Le coup fait le double de dégâts (brise aussi le Gel). |
| **Électrocution** | Orage + Marée | La foudre saute sur les ennemis autour (4 blocs) et les immobilise. |
| **Orage** | Orage + Vent | Nuage d'orage 4 s : un éclair toutes les 0,5 s sur un ennemi dedans. |
| **Magnétisme** | Orage + Pierre | Les ennemis autour (6 blocs) sont attirés sur la victime. |
| **Typhon** | Marée + Vent | Trombe d'eau 4 s qui aspire et soulève les ennemis. |
| **Bourbier** | Marée + Pierre | Mare de boue 4 s : ralentissement IV et Faiblesse. |
| **Tempête de sable** | Vent + Pierre | Tourbillon de sable 4 s : dégâts et Cécité. |

| Sort | Effet |
|---|---|
| **Glyphe du Feu** (Fire Glyph) | Le glyphe de la flamme lance 3 à 6 comètes de feu l'une après l'autre ; elles embrasent ce qu'elles touchent. |
| **Glyphe du Givre** (Frost Glyph) | Le flocon éclate en un éventail de 5 à 8 aiguilles de glace qui traversent chacune un ennemi et le ralentissent. |
| **Glyphe de l'Orage** (Storm Glyph) | La foudre jaillit vers l'ennemi visé puis bondit d'ennemi en ennemi (3 à 5). |
| **Glyphe de la Marée** (Tide Glyph) | Un mur d'eau roule devant le lanceur en un large arc (8 à 12 blocs), repousse les ennemis et éteint les flammes. |
| **Glyphe du Vent** (Gale Glyph) | Une bourrasque en arc étroit (11 blocs) projette les ennemis en l'air. |
| **Glyphe de la Pierre** (Stone Glyph) | Un rocher lancé en cloche, qui éclate à l'impact (2,5 blocs). |
| **Sceau élémentaire** (Elemental Seal) | Les six glyphes tournent autour du lanceur (12 à 22 s) et lancent tour à tour leur trait sur l'ennemi le plus proche : les réactions s'enchaînent toutes seules. |
| **Écriture élémentaire** (Elemental Script) | Une ligne de 4 à 6 glyphes s'écrit au sol, chacun dans l'élément suivant, et chacun entre en éruption dès qu'il est écrit. |
| **Résonance des glyphes** (Glyph Resonance) | Toutes les marques des ennemis autour (16 blocs) éclatent à la fois ; leur élément rejaillit sur les voisins et déclenche leurs propres marques. |
| **Glyphe Primordial** (Primordial Glyph, ultime) | Un grand glyphe s'écrit dans le ciel : anneau d'or, les six glyphes, l'étoile qui les relie ; puis chaque élément abat son trait à tour de rôle sur les ennemis en dessous (14 blocs), toutes les 0,2 s. |

Livre : **Codex des Six Encres** (cuir bleu nuit, étoile du Glyphe Primordial dorée à la feuille sur la couverture,
une goutte de chaque encre, opale). Focus de l'école : plume. Les glyphes sont dessinés pour l'addon : le jeu et les
icônes (`tools/glyph_assets.py`) utilisent les mêmes tracés.

### Magie des Jetons

Des **jetons d'âme** bleu cyan : de petites pièces carrées qui gardent une étincelle d'âme (le modèle 3D est le
**Jeton d'Âme** de l'équipe du serveur, fait sous Blockbench, passé du turquoise au bleu cyan). On les lance, on les
sème, on les fait tourbillonner, et un jeton lancé est une porte : le lanceur peut s'y téléporter.

| Sort | Effet |
|---|---|
| **Lancer de Jeton** (Token Throw) | Un jeton lancé là où tu regardes : il blesse ce qu'il touche et reste là où il tombe (8 à 18 s, un fil de lumière le signale). **Relancer le sort** (instantané et gratuit, relance d'Iron's) te téléporte sur le jeton. |
| **Vague de Jetons** (Token Wave) | Une vague de jetons jaillit du sol et roule devant toi (11 à 18 blocs, 3 de large) : chaque ennemi touché est frappé et projeté en l'air. |
| **Tempête de Jetons** (Token Storm) | Un tourbillon de 24 jetons autour de toi qui te suit (3,5 blocs, 7 à 12 s) : il taillade les ennemis à l'intérieur toutes les demi-secondes et abat en plein vol les tirs ennemis qui y entrent. |
| **Ricochet** (Token Ricochet) | Un jeton tiré sur un ennemi rebondit sur le suivant, puis encore (3 à 6 ennemis, 8 blocs entre deux). |
| **Pile ou Face** (Coin Flip) | Un jeton lancé en l'air décide. **Face** (doré) : il file sur l'ennemi visé et frappe deux fois plus fort (sans personne en vue : Force 10 s). **Pile** (cyan) : il te soigne et te donne Absorption 10 s. |
| **Égide de Jetons** (Token Aegis) | 3 à 6 jetons tournent autour de toi (20 s) : chacun arrête un coup ou un tir qui t'était destiné (aucun dégât) et repart frapper l'attaquant. Les chutes et le feu passent. |
| **Pluie de Jetons** (Token Rain) | Des jetons pleuvent 2 à 4 s sur l'endroit visé (cercle de 4 blocs, jusqu'à 24 blocs), chacun éclate à l'impact. |
| **Jetons Piégés** (Trap Tokens) | Un éventail de 3 à 5 jetons semés au sol devant toi ; armés au bout d'une seconde, ils éclatent quand un ennemi approche (dégâts et ralentissement autour). Neuf au plus à la fois, 45 s. |
| **Tribut des Âmes** (Soul Tribute) | Arrache un jeton à chaque ennemi autour de toi (10 blocs, 8 au plus), ce qui le blesse ; les jetons volent jusqu'à toi et te soignent en arrivant. |
| **Main des Âmes** (Hand of Souls, ultime) | Des dizaines de jetons arrivent de toutes parts et forment une **main géante** au-dessus de l'ennemi visé : elle le saisit, le soulève de 3 blocs en l'écrasant pendant 2 s, puis le fracasse au sol (projection et dégâts à 4 blocs autour). Sans personne en vue, la main ouverte frappe l'endroit visé. |

Livre : **Registre des Âmes** (cuir bleu nuit, un cercle de jetons serti dans la couverture, coins d'argent).
Focus de l'école : le **Jeton d'Âme**, un objet à part entière (4 jetons = 1 cristal de prismarine + 1 pépite d'or +
1 sable des âmes, sans forme).

### Magie de Mousse, Cuivre et Rouille

La magie des temples engloutis, **offensive** de bout en bout (elle remplace l'école « Mousse et Flore » prévue au
départ, qui faisait surtout du soin). Des pics, des piliers et des murs de **vieux cuivre vert-de-gris** jaillissent du
sol dans un style antique, couverts de mousse, des **lianes** pendues en décor ; la rouille ronge les armures. Tout est
construit avec les blocs du jeu (cuivre oxydé, coupé et ciselé, briques de pierre moussues, mousse, lianes) mis à
l'échelle, pas de textures inventées.

| Sort | Effet |
|---|---|
| **Pics de Cuivre** (Copper Spikes) | Tu frappes le sol : un coin de pics de cuivre jaillit devant toi, rangée après rangée (8 à 14 blocs), empale et projette en l'air les ennemis, qui saignent 3 s. |
| **Pilier Antique** (Ancient Pillar) | Un pilier moussu jaillit sous l'ennemi visé (ou là où tu vises, jusqu'à 20 blocs) et le projette haut. |
| **Mur Basculant** (Toppling Wall) | Un pan de rempart crénelé (5 blocs de large, 3,5 de haut) surgit devant toi, vacille et s'abat vers l'avant : il écrase et ralentit fort tout ce qui est dessous. |
| **Rouille** (Rust) | Un caillot de rouille : la cible perd 15 % d'armure et de robustesse par niveau (10 à 18 s) et ses pièces d'armure s'usent toutes les 2 s. |
| **Lianes Étrangleuses** (Strangling Vines) | Des lianes escaladent l'ennemi visé : il ne peut plus marcher ni sauter (3,5 à 6 s) et elles le serrent chaque seconde. Dès le niveau 3, elles saisissent aussi ses voisins. |
| **Spores de Mousse** (Moss Spores) | Un tapis de mousse fleurie (3,5 blocs) s'étend là où tu vises : toutes les demi-secondes, les ennemis dessus sont blessés, empoisonnés et ralentis. |
| **Colonnade** (Colonnade) | Huit colonnes jaillissent en cercle autour de toi : les ennemis proches sont blessés et repoussés. |
| **Paratonnerre Antique** (Ancient Lightning Rod) | Une haute colonne couronnée d'un paratonnerre s'élève là où tu vises (9,5 à 17 s) : toutes les 1,5 s la foudre tombe dessus et saute sur l'ennemi le plus proche (8 blocs). |
| **Cloche Antique** (Ancient Bell) | Une grande cloche verdie tombe du ciel sur l'ennemi visé : écrasé et étourdi dessous, blessé et repoussé autour (5 blocs). |
| **Temple Englouti** (Sunken Temple, ultime) | Les ruines d'un temple s'élèvent autour de l'ennemi visé : huit colonnes de 6 blocs, reliées par des linteaux, l'enferment (ceux qui approchent du bord sont repoussés dedans) sous une pluie de gravats, puis **s'effondrent toutes vers l'intérieur** et ensevelissent tout. |

Livre : **Codex Rouillé** (plaques de cuivre vert-de-gris rivetées, rouille sur les bords, mousse sur le dos).
Focus de l'école : le **lingot de cuivre**.

### Magie des Murs

La magie du grand méchant. Faite de trois blocs seulement : **briques de pierre**, **briques de pierre moussues** et
**briques de pierre craquelées** ; tout le reste n'est que lumière par-dessus (le mortier qui rougeoit comme des
braises entre les briques, des runes gravées, le **sceau du maçon** qui s'embrase au sol à chaque incantation). Les
murs se **bâtissent brique par brique**, chaque bloc jaillissant du sol jusqu'à sa place. Le Mur Surgissant,
l'Oubliette, le Bastion et le rempart de la Forteresse sont faits de **vrais blocs** : solides, on ne les traverse pas
et les tirs s'y brisent. Ils ne se posent que dans l'air (ou l'herbe, la neige, l'eau...), jamais dans une
construction ni là où le lanceur n'a pas le droit de construire, et à la fin le sol redevient exactement comme avant
(même après un redémarrage du serveur). Un bloc cassé pendant le sort ne donne rien : il retombe en poussière.

| Sort | Effet |
|---|---|
| **Volée de Briques** (Brick Volley) | Des briques s'arrachent du sol à hauteur d'épaules, flottent un instant puis partent l'une après l'autre sur l'ennemi visé (3 à 6 briques), frappent fort et éclatent. |
| **Mur Surgissant** (Rising Wall) | Un mur crénelé en vrais blocs se bâtit devant toi : 5 x 3, puis 4 de haut (niv. 3) et 7 de large (niv. 4), 8 à 13 s. |
| **Pluie de Briques** (Brick Rain) | Un sceau s'embrase sur l'endroit visé, un tourbillon de briques se forme au-dessus puis déverse 16 à 30 briques sur 4 blocs (la première pile au centre). |
| **Mur Éclaté** (Wall Burst) | Un mur se bâtit, son mortier chauffe à blanc, il tremble puis explose vers l'avant : cône de briques sur 8 blocs. |
| **Mortier** (Mortar) | Un bloc de briques lancé en cloche (jusqu'à 30 blocs), un sceau marque l'impact : éclate, blesse, ralentit et disperse sur 3,5 blocs. |
| **Murs Broyeurs** (Crushing Walls) | Deux murs se bâtissent de part et d'autre de l'ennemi visé et se referment sur lui de plus en plus vite, **face contre face** : broyé, étourdi, puis les murs volent en éclats. |
| **Oubliette** (Oubliette) | Une cellule close en vrais blocs (anneau puis couvercle, à la taille de la cible) se bâtit autour de l'ennemi visé : emmuré dans le noir 5 à 9 s. |
| **Charge du Rempart** (Rampart Charge) | Un grand mur crénelé se bâtit et charge droit devant (10 à 15 blocs) en labourant le sol, balaie les ennemis puis s'écrase et explose. |
| **Bastion** (Bastion) | Un donjon carré en vrais blocs se bâtit autour de toi en faisant le tour (murs crénelés à 3 blocs, une tour à chaque coin), 9,5 à 15,5 s : personne n'entre, aucun tir ne passe, et tu résistes aux dégâts. |
| **Forteresse du Tyran** (Tyrant's Fortress, ultime) | 2 s d'incantation, un grand sceau s'étend sous toi ; puis un rempart circulaire en vrais blocs (9 blocs de rayon) se bâtit tout autour de toi et huit tours surgissent le long, braseros allumés. Pendant 10 s, les tours pilonnent tous les ennemis à 26 blocs pendant que tu résistes aux coups ; puis toute la forteresse **explose vers l'extérieur**. |

Livre : **Codex des Remparts**. Focus : les trois briques de pierre.

### Rituels (sorts de groupe)

Ce n'est pas une école : des rituels à plusieurs, lents et spectaculaires. Ils se font avec la **Craie rituelle**
(calcite + poudre d'os + éclat d'améthyste, sans forme ; elle trace 32 cercles).

1. **Choisir le rituel** : accroupi + clic dans le vide avec la craie.
2. **Tracer le cercle** : clic sur le sol. Le cercle se dessine à la craie en 3 secondes ; celui qui le trace mène le
   rituel.
3. **Tenir les points** : chaque participant s'agenouille (accroupi) sur un point du cercle. Quand tous les points
   sont tenus, l'incantation commence.
4. **L'incantation**, très lente (20 à 60 secondes) : les douze runes s'allument une à une, des rayons montent de
   chaque participant vers le centre, un pilier de lumière monte dans le ciel, puis un anneau de runes s'ouvre
   au-dessus et tourne de plus en plus vite. Chaque participant donne 75 de mana au fil de l'incantation.
5. **La rupture** : si un point reste vide plus de 2 secondes (quelqu'un se relève ou part) ou si quelqu'un n'a plus
   de mana, le rituel est rompu : le cercle vire au rouge et explose sur les participants.

Les joueurs agenouillés dans le cercle mais hors des points forment le **chœur** : +20 % de puissance chacun (jusqu'à
deux fois plus fort). Pour une scène RP, le staff peut donner le tag `arcanes_oublies_ritualist` à un PNJ
(`/tag <pnj> add arcanes_oublies_ritualist`) pour qu'il tienne un point à la place d'un joueur.

| Rituel | Participants | Incantation | Effet |
|---|---|---|---|
| **Météore guidé** | 3 | 30 s | Un météore tombe lentement du ciel là où regarde le meneur (64 blocs) : explosion de feu sur les créatures hostiles (aucun bloc cassé). |
| **Dôme de protection** | 4 | 40 s | Un dôme de lumière de 16 blocs pendant 5 min : les créatures hostiles ne peuvent pas entrer, leurs tirs se brisent dessus, les joueurs dessous résistent aux dégâts. |
| **Appel de la tempête** | 3 | 30 s | Un orage d'une minute au-dessus du lieu (24 blocs) : la foudre frappe une créature hostile toutes les 1,5 s. |
| **Portail** | 2 | 20 s | Un portail d'une minute vers l'ancre marquée avec la craie du meneur (accroupi + clic sur le sol, même dans une autre dimension), et son jumeau pour revenir. |
| **Bénédiction des anciens** | 4 | 45 s | Chaque joueur à 24 blocs est soigné, purifié et renforcé 5 min (Régénération, Absorption, Résistance, Force). |
| **Éveil du colosse** | 5 | 60 s | Un colosse géant (golem 2,5 fois plus grand, 300 PV) s'éveille et combat pour le groupe pendant 3 min. |

Les rituels n'épargnent que leurs participants et ne visent que les créatures hostiles : ils ne blessent jamais les
autres joueurs.

Les sons sont des sons de Minecraft combinés. Les textures sont dessinées par `tools/gen_assets.py`
(`tools/explosion_assets.py` pour l'Explosion, `tools/book_assets.py` pour le Livre, `tools/errethys_assets.py`
pour Errethys, `tools/physical_assets.py` et `tools/physical_weapon_icons.py` pour Caractéristique Physique,
`tools/boss_assets.py` pour la Magie de Boss, `tools/rune_assets.py` pour la Magie Runique, `tools/glyph_assets.py`
pour la Magie de Glyphe, `tools/token_assets.py` pour la Magie des Jetons à partir de `tools/models/soul_token.bbmodel`,
`tools/verdigris_assets.py` pour la Magie de Mousse, Cuivre et Rouille, `tools/wall_assets.py` pour la Magie des Murs).

## Récupérer le mod

Chaque push lance le workflow GitHub Actions **Build & Test** :
- il compile le mod et lance des tests sur un serveur sans écran (chaque sort fait ce qu'il doit) ;
- il lance un vrai client Minecraft sans écran qui lance chaque sort devant la caméra (détecte les crashs d'affichage) ;
- il publie le `.jar` dans l'artefact **arcanes-oublies-jar** de l'exécution.

## Installer

Dans le dossier `mods` (client et serveur), à côté de :

- NeoForge 21.1.221 ou plus récent (Minecraft 1.21.1)
- Iron's Spells 'n Spellbooks 1.21.1-3.16 ou plus récent
- ses dépendances : Iron's Lib, Curios, GeckoLib, Player Animator

## Développer

- Le code est dans `src/main/java/com/waylays/arcanes`.
- Textures, modèles d'objets et animations des techniques : modifier `tools/gen_assets.py` (ou `explosion_assets.py`,
  `book_assets.py`, `errethys_assets.py`, `physical_assets.py`, `boss_assets.py`, `rune_assets.py`, `glyph_assets.py`,
  `token_assets.py`) puis lancer
  `python3 tools/gen_assets.py` (Pillow).
  Une texture faite à la main peut aussi remplacer directement le PNG du même nom.
- Animations des techniques et des lanceurs de Magie de Boss : `tools/physical_animations.py` et
  `tools/boss_animations.py`, écrites avec le petit squelette de `tools/anim_rig.py`
  (hanches, buste, bras qui suivent les épaules, main gauche qui saisit les armes à deux mains, une clé par tick).
  `python3 tools/anim_preview.py <fichier.json> <animation> <sortie.png>` dessine une animation tick par tick (de
  profil, de trois quarts et de dessus) comme Player Animator la joue, pour la vérifier avant de lancer le jeu.
