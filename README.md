# Arcanes Oubliés

Addon pour **Iron's Spells 'n Spellbooks** (Minecraft 1.21.1, NeoForge 21.1.221+), pour notre serveur RP.
La dernière version pour Minecraft 1.20.1 (Forge) est la 0.9.0 (commit `6359b13` de l'historique).
La liste complète des écoles et des sorts prévus est dans [`ADDONS_SPELLS.txt`](ADDONS_SPELLS.txt).

## Contenu de la bêta 1.0.0 : écoles de l'Or, de l'Explosion, du Livre, d'Errethys, Caractéristique Physique, Magie de Boss, Magie Runique, Magie de Glyphe, Magie des Jetons, Magie de Mousse, Cuivre et Rouille, Magie des Murs et Magie des Pierres Précieuses

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

**Invocations** (Sentinelles d'Or, Dragon d'Or, Bibliothèque Interdite, Pile Volante, Dragon d'Origami et tous les sorts
d'Errethys) : relancer le sort renvoie ses invocations (relance d'Iron's, instantanée et gratuite, visible sur
l'écran) ; le temps de recharge démarre quand elles sont toutes parties (renvoyées, tuées ou à la fin de leur temps).

### Or

| Sort | Effet |
|---|---|
| **Trait d'Or** (Gold Bolt) | Lingot d'or tournoyant qui frappe sa cible, projette des pièces et ricoche sur l'ennemi visible le plus proche (75 % des dégâts, 1 ricochet + 1 tous les 4 niveaux). |
| **Chaînes Dorées** (Gilded Chains) | De vraies chaînes d'or sortent de médaillons solaires, se referment en deux boucles et un cadenas : la cible est clouée sur place, les chaînes se resserrent toutes les 2 s. |
| **Pluie d'Or** (Goldfall) | La pierre du soleil s'ouvre dans le ciel, des lingots géants en tombent pointe en avant, écrasent et repoussent, restent plantés puis fondent en pièces. |
| **Pics d'Or** (Golden Spikes) | Vague de grappes de cristaux d'or (formes et tailles toutes différentes) qui blessent et projettent, terminée par une couronne de pics. |
| **Malédiction de Midas** (Midas Curse) | La cible se change en or depuis les pieds jusqu'à se figer en statue qui vole en éclats et blesse ceux autour. |
| **Lame Aurum** (Aurum Blade) | Épée géante forgée d'or fondu sous la voûte solaire (garde au soleil gravé, pommeau en rubis) qui se plante sur la cible : onde de choc, puis une réplique avant de fondre en pièces. |
| **Torrent d'Or Fondu** (Molten Torrent) | Sort maintenu : filet d'or fondu qui brûle. Les flaques refroidissent en plaques d'or massif qui retiennent ceux qui s'y tiennent, puis se brisent en pièces. |
| **Armure Dorée** (Gilded Armor) | De l'or liquide monte le long du corps et durcit : armure en plus et une partie des dégâts renvoyée à l'attaquant. |
| **Sentinelles d'Or** (Golden Sentinels) | 2 à 4 chevaliers d'or couronnés du soleil, qui sortent de l'or fondu et frappent tes ennemis. |
| **Soleil d'Eldorado** (Sun of Eldorado, ultime) | La pierre du soleil sertie de rubis et d'émeraudes : ses rayons brûlent et changent en statues les ennemis frappés 5 fois, puis supernova d'or. |
| **Dragon d'Or** (Golden Dragon, bonus légendaire) | Un soleil géant s'embrase dans le ciel et le Dragon d'Or d'Eldorado en sort : long dragon d'écailles d'or, cornes, crinière cramoisie, moustaches de lumière et perle de feu. Il chasse les ennemis du lanceur, crache de l'or fondu, s'enroule autour de sa proie, rugit, plonge, et finit en cascade de pièces. Relancer le sort le renvoie. |

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
| **Salamandre de Mana** (Mana Salamander) | Une salamandre de mana **rouge**, des braises dans les veines et une crête de petites flammes sur le dos : elle ondule comme un lézard, sa morsure met le feu (4 s) et, toutes les 3 s, elle crache un jet de flammes sur les ennemis proches devant elle (seulement ceux qu'elle combat). Le feu ne lui fait rien ; elle n'enflamme jamais les blocs. |

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
| **Pendule du bourreau** (Pendulum of Doom) | Un pendule géant à lame d'acier en croissant pend d'un cercle de runes au-dessus d'une bande de 20 blocs et la traverse trois fois. |
| **Faille Sismique** (Seismic Rift) | Une faille se marque depuis les pieds du lanceur et s'ouvre tronçon par tronçon en projetant en l'air ceux qui s'y trouvent : le danger avance le long de la ligne. |
| **Orbe du Néant** (Void Orb) | Une énorme sphère de ténèbres roule lentement sur un chemin marqué (la toucher blesse, s'en approcher attire) ; au bout, elle s'effondre en aspirant tout autour, puis explose. |
| **Croix de Lumière** (Cross of Light) | Une grande croix de lumière se marque autour de la cible et jaillit en piliers, puis une seconde croix tournée d'un huitième de tour. |
| **Prison Runique** (Rune Prison) | Un anneau se marque autour de la cible, puis un mur de runes s'y dresse 10 à 16 s : quiconque n'est pas du camp du lanceur et tente de le franchir est blessé et repoussé. |
| **Jugement Dernier** (Last Judgment, ultime) | Un cercle de runes géant s'ouvre dans le ciel ; toute l'arène autour du lanceur (15 blocs) est condamnée, sauf trois petits refuges verts. Cinq météores tombent pendant le compte à rebours, puis le jugement frappe tout ce qui est hors des refuges. |

Les **grandes attaques**, dix de plus, plus fortes (épiques et légendaires), chacune avec son propre décor en 3D :

| Sort | Effet |
|---|---|
| **Mille Lames** (Thousand Blades) | Une couronne d'épées de lumière se forme au-dessus du lanceur ; de petits cercles se marquent en cascade sur les ennemis autour de la cible puis partout autour (14 à 22 épées) ; chaque épée s'élève, se retourne et plonge dans son cercle, reste plantée un instant puis se brise en lumière. |
| **Roue du Bourreau** (Executioner's Wheel) | Une grande roue de fer noir hérissée de lames ardentes se forme devant le lanceur et tourne de plus en plus vite, puis dévale une bande de 26 blocs en traçant un sillon de feu ; la bande est marquée en quatre parties qui frappent quand la roue passe dessus (brûlure, projection sur le côté). |
| **Gueule des Abysses** (Abyssal Maw) | Le sol s'ouvre sous la cible en une gueule de nuit violette, deux rangées de crocs se dressent autour et elle attire doucement ce qui est proche ; à la fin de l'alerte elle mord : dégâts, ralentissement et flétrissement. |
| **Prisme Divin** (Divine Prism) | Un grand cristal flotte au-dessus du lanceur ; douze lignes se marquent autour de lui comme les heures d'une horloge et un rayon de chaque couleur de l'arc-en-ciel balaie chacune à son tour, tout autour. Il faut se tenir entre les lignes. |
| **Chaînes des Damnés** (Chains of the Damned) | Des chaînes noires jaillissent de portes de runes autour des 3 à 5 ennemis les plus proches et enchaînent leur place (très ralenti tant qu'on y reste) ; à la fin, les chaînes se brisent et une colonne de feu infernal s'élève. |
| **Pentacle d'Épines** (Pentacle of Thorns) | Une étoile à cinq branches se marque trait après trait autour de la cible ; d'énormes épines rouge sang jaillissent le long de chaque trait, puis le cœur de l'étoile éclate. Les espaces entre les traits sont sûrs. |
| **Ailes du Séraphin** (Seraph's Wings, légendaire) | Deux grandes ailes de lumière dorée se déploient là où se tient le lanceur pendant qu'un large éventail de sol est marqué devant lui (16 blocs) ; elles battent une fois et une tempête de plumes de lumière balaie l'éventail, projetant et soulevant tout ce qu'elle touche. |
| **Glas funèbre** (Bell of Doom, épique) | Une grande cloche de bronze sonne trois fois au-dessus du lanceur ; chaque coup envoie un anneau marqué sur le sol, plus large à chaque fois (jusqu'à 13,5 blocs) : frappe, recul et ralentissement. |
| **Damier infernal** (Infernal Checkerboard, épique) | Un damier de 5 × 5 cases autour du point visé : les cases d'une couleur s'embrasent en colonnes de feu, puis les autres. |
| **Poing du titan** (Titan's Fist, épique) | Un gantelet colossal de fer et d'or plonge du ciel sur le point visé (4 blocs), puis le sol tremble en deux anneaux plus larges et plus faibles. |
| **Colonnes renversées** (Toppling Columns, rare) | Cinq colonnes de temple en marbre, hautes de 18 blocs, sortent de terre devant le lanceur ; l'une après l'autre, du milieu vers l'extérieur, elles se fissurent et s'écrasent le long de leur bande. |
| **Faux de la Faucheuse** (Reaper's Scythe, épique) | Une faux spectrale fauche la moitié devant le lanceur, puis la moitié derrière (9 blocs) ; ceux qu'elle touche se flétrissent. |
| **Vierges de fer** (Iron Maidens, épique) | Un cercueil de fer noir hérissé de pointes sort de terre autour de chacun des trois ennemis les plus proches ; ses portes claquent à la fin de l'alerte : retenu et saigné. |
| **Spectres royaux** (Spectral Kings, épique) | Quatre rois fantômes aux quatre vents autour de la cible chargent l'un après l'autre à travers elle, le long de quatre bandes marquées. |
| **Horloge du destin** (Clock of Fate, rare) | Le sol autour du lanceur devient le cadran d'une horloge géante (11 blocs) ; sa grande aiguille d'or balaie le cadran et chaque heure brûle quand elle passe dessus. |
| **Lotus écarlate** (Crimson Lotus, rare) | Un lotus de verre écarlate éclot sur le point visé : son cœur éclate, puis ses six pétales s'ouvrent et tranchent six parts de sol. |
| **Effondrement** (Collapse, légendaire) | Une sphère noire attire tout dans 12 blocs pendant 2 s, s'effondre sur son cœur (4 blocs) puis explose vers l'extérieur. |
| **Colère des Cieux** (Wrath of the Heavens, légendaire) | Une tempête se forme au-dessus du lanceur (roue de nuages sombres autour d'un œil lumineux) et lance 10 à 14 éclairs, chacun marqué là où se tient sa cible quand il est appelé : il faut rester en mouvement. |
| **Éclipse** (Eclipse, légendaire) | Un soleil s'allume dans le ciel et un disque noir glisse devant lui ; à la totalité, sa couronne s'embrase et l'arène autour du lanceur (15,5 blocs) est frappée en trois anneaux de feu noir, du centre vers l'extérieur, chacun un peu après le précédent (ténèbres et faiblesse). Il faut passer dans un anneau qui a déjà brûlé. |
| **Lune Déchue** (Fallen Moon, ultime) | Une lune apparaît haut au-dessus de la cible et tombe lentement puis de plus en plus vite en s'embrasant (6 s d'alerte) ; elle écrase tout sur 14 blocs, puis le souffle frappe jusqu'à 22 blocs autour du cratère. |

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

### Magie des Pierres Précieuses

Cinq gemmes taillées, chacune avec sa nature : le **rubis** brûle (la lumière concentrée), le **lapis** scelle (le
ciel nocturne et ses étoiles d'or), le **diamant** réfracte et endure, l'**améthyste** résonne, l'**émeraude**
soigne. Les gemmes sont de vrais solides taillés dessinés en verre translucide (brillant, taille émeraude, pointes de
cristal) dont les facettes s'allument tour à tour, avec un cœur lumineux et des éclats qui scintillent ; chaque sort
s'appelle avec le **sceau du joaillier** (le plan d'une pierre taillée) qui brûle au sol dans la couleur de sa gemme.
Le rubis et le diamant se combinent : un Rayon de Rubis visé à travers un Prisme de Diamant se divise en trois.

| Sort | Effet |
|---|---|
| **Rayon de Rubis** (Ruby Ray) | Maintenu : un rubis tourne devant la main et déverse un rayon rouge (24 blocs) qui traverse et brûle tout ce qui est sur sa ligne. À travers un Prisme de Diamant, il se divise en trois rayons qui cherchent les ennemis autour du prisme. |
| **Cœur Ardent** (Burning Heart) | Un rubis lancé se plante dans l'ennemi (ou le sol), bat trois fois en brûlant autour de lui, puis éclate en éclats en fusion (4,5 blocs). |
| **Éclats de rubis** (Ruby Shards) | Trois longs cristaux de rubis en éventail, une traînée de feu derrière chacun : chacun transperce deux ennemis en les enflammant, puis se brise. |
| **Nova de rubis** (Ruby Nova) | Un grand rubis s'élève au-dessus du lanceur et rassemble la lumière une seconde, puis lâche un anneau de 16 rayons sur le sol (9 blocs) : dégâts, feu, recul ; le rubis se brise. |
| **Météore de rubis** (Ruby Meteor) | Un rubis géant tombe en flammes du ciel sur le sceau visé 1,5 s plus tard (4,5 blocs, recul, feu) et laisse un bassin en fusion qui brûle 4 s, des cristaux de rubis dressés dans la chaleur. |
| **Sceau de Lapis** (Lapis Seal) | Quatre tablettes de lapis à runes d'or entourent l'ennemi visé et l'enchaînent de lumière dorée : ni sort ni mouvement (4 à 9 s), puis le sceau éclate. |
| **Constellation de Lapis** (Lapis Constellation) | Sept étoiles de lapis tournent autour du lanceur, reliées comme une constellation ; chacune fond sur un ennemi qui approche (8 blocs). |
| **Étoile filante** (Shooting Star) | Une étoile de lapis mouchetée et cerclée d'or vole vers l'ennemi regardé et éclate en étoiles d'or : dégâts sur 2 blocs, ralentissement. |
| **Voûte céleste** (Celestial Vault) | Un disque de ciel nocturne (lapis, constellation et runes d'or) s'ouvre au-dessus de la zone visée : pendant 5 s, des étoiles d'or tombent sur les ennemis dessous. |
| **Miroir de lapis** (Lapis Mirror) | Une tablette de lapis dans un cadre d'or tourne autour du lanceur 10 s : elle renvoie les tirs ennemis et, toutes les 2 s, enchaîne l'ennemi le plus proche (dégâts, silence, ralentissement). |
| **Prisme de Diamant** (Diamond Prism) | Un grand diamant flotte au-dessus de l'endroit visé et frappe chaque seconde jusqu'à trois ennemis de rayons arc-en-ciel ; il divise le Rayon de Rubis (deux prismes à la fois). |
| **Carapace de Diamant** (Diamond Shell) | Douze plaques de diamant autour du lanceur : chaque coup perd 60 % de sa force sur une plaque, qui file frapper l'agresseur ; à la fin le reste éclate sur les ennemis. |
| **Lance de diamant** (Diamond Lance) | Une pointe de diamant tirée plus vite que l'œil traverse tous les ennemis sur 22 blocs, un rayon arc-en-ciel derrière elle. |
| **Poussière de diamant** (Diamond Dust) | Un dôme de poussière de diamant scintillant (3,5 blocs, 4 s) taillade, aveugle et ralentit les ennemis dedans. |
| **Bastion de diamant** (Diamond Bastion) | Un grand amas de diamant jaillit juste devant le lanceur pour 12 s sous un dôme de lumière : Résistance II pour son camp, tirs ennemis détournés ; à la fin il éclate sur les ennemis autour. |
| **Résonance d'Améthyste** (Amethyst Resonance) | Des grappes d'améthyste jaillissent en cercle et sonnent trois fois : chaque onde blesse et étourdit les ennemis (8 blocs) et brise leurs tirs. |
| **Géode** (Geode) | Une géode (basalte, calcite, améthyste) se referme autour de l'ennemi visé, ouverte côté lanceur ; ses cristaux poussent vers l'intérieur et le transpercent, puis elle éclate. |
| **Pointes d'améthyste** (Amethyst Spikes) | Des grappes d'améthyste jaillissent l'une après l'autre sur 12 blocs devant le lanceur, de plus en plus grandes : dégâts et projection. |
| **Améthyste bourgeonnante** (Budding Amethyst) | Un bloc d'améthyste bourgeonnante grandit au point visé (bourgeons, grappes, grands cristaux) ; chaque étape résonne sur les ennemis autour (dégâts, ralentissement), puis il éclate sur eux (5 blocs). |
| **Carillon d'améthyste** (Amethyst Chimes) | Des cristaux d'améthyste pendent d'un anneau d'or au-dessus du lanceur 12 s : chaque seconde, une onde frappe jusqu'à 3 ennemis visibles (ralentis) et brise les tirs ennemis proches. |
| **Floraison d'Émeraude** (Emerald Bloom) | Une fleur d'émeraudes soigne chaque seconde le lanceur et ses alliés (8 blocs) et retire un mal à chacun ; à la fin, Régénération et Absorption. |
| **Lien de vie** (Lifeline) | Un rayon vert relie une émeraude sur la poitrine du lanceur au plus blessé de son camp (12 blocs) : il lui retire un mal, puis le soigne toutes les demi-secondes pendant 3 s. |
| **Égide d'émeraude** (Emerald Aegis) | Des plaques d'émeraude se referment en carapace autour du lanceur puis s'enfoncent en lui : Absorption (8 PV et plus) pour lui et son camp à 6 blocs pendant 15 s, et un peu de régénération. |
| **Arbre d'émeraude** (Emerald Tree) | Une émeraude lancée devient un arbre de cristal chargé de fruits d'émeraude : toutes les 2 s pendant 12 s, un fruit tombe et soigne le camp du lanceur autour (5 blocs). |
| **Épines d'émeraude** (Emerald Thorns) | Un fourré d'épines d'émeraude jaillit autour du lanceur (7 blocs) et empale les ennemis ; des flots de vie reviennent et le soignent de 40 % du mal fait. |
| **Couronne des Cinq Joyaux** (Crown of the Five Gems, ultime) | Un pentagramme et cinq grandes gemmes autour du lanceur : pendant 8 s le rubis brûle, le lapis scelle, le diamant protège, l'améthyste résonne, l'émeraude soigne ; puis elles fusionnent en une gemme prismatique qui **foudroie l'endroit visé** d'un rayon de toutes les couleurs. |

Livre : **Codex du Joaillier** (cuir violet, cinq gemmes serties). Focus : diamant, émeraude, éclat d'améthyste,
lapis-lazuli.

### Magie de l'Écho

Le son et le sculk des Abysses. Les coups de l'école laissent de la **Résonance** dans leurs victimes (jusqu'à 5 niveaux,
10 s) : chaque niveau rend les coups d'Écho 10 % plus forts, et certains sorts la font éclater. Livre : **Codex des
Échos** ; focus : éclat d'écho et blocs de sculk.

| Sort | Effet |
|---|---|
| **Onde Sonore** (Sound Wave, commun) | Un anneau de son traverse deux ennemis et laisse 1 Résonance dans chacun. |
| **Hurlement** (Shriek, commun) | Un hurlement sur un cône de 7 blocs : dégâts, ténèbres 2 s, recul, 1 Résonance. |
| **Pas d'Écho** (Echo Step, peu commun) | Un pas jusqu'à 10 blocs ; l'écho laissé derrière vibre puis éclate 1,5 s plus tard (dégâts sur 3 blocs). |
| **Vrilles de Sculk** (Sculk Tendrils, peu commun) | Le sculk s'étend là où l'on vise et des vrilles fouettent ce qui s'y trouve toutes les demi-secondes : dégâts, immobilisation, Résonance. |
| **Capteur de Sculk** (Sculk Sensor, rare) | Un capteur posé au sol 20 s : il frappe d'un trait de son tout ennemi qui bouge près de lui (7 blocs), toutes les 1,5 s. Deux au plus. |
| **Choc Résonant** (Resonant Burst, rare) | Fait éclater la Résonance de tous les ennemis à 12 blocs : dégâts par niveau ; ceux qui n'en ont pas se mettent à résonner. |
| **Rayon Sonique** (Sonic Boom, rare) | Une seconde de charge comme le Warden, puis un rayon de 24 blocs qui traverse tout, projette et laisse 2 Résonance. |
| **Dôme du Silence** (Silence Dome, épique) | Un dôme de 6 blocs, 8 à 12 s : les ennemis dedans ne peuvent plus lancer de sorts, sont ralentis et résonnent, leurs tirs retombent ; le camp du lanceur est renforcé. |
| **Chœur des Abysses** (Deep Choir, épique) | Trois hurleurs de sculk sortent du sol et hurlent à tour de rôle, chacun lançant un rayon sonique sur l'ennemi le plus proche (2 Résonance). |
| **Requiem** (ultime) | Un cœur de sculk bat au-dessus du lanceur ; cinq murs de son balaient 14 blocs, puis au dernier battement toute la Résonance éclate et le terrain tombe dans le silence et le noir. |

### Magie du Sable

Le désert : sable, verre et grès. Livre : **Codex des Dunes** ; focus : sable, sable rouge, grès et verre.

| Sort | Effet |
|---|---|
| **Lance de sable** (Sand Lance, commun) | Une lance de sable tassé qui tourne sur elle-même, pointe de verre : elle transperce jusqu'à 3 ennemis en ligne droite et les ralentit. |
| **Nuée de scarabées** (Scarab Swarm, commun) | Une nuée de scarabées d'or (carapace bleu-vert et or, pattes, ailes de verre) traque l'ennemi le plus proche et le mord sans cesse pendant 5 s. |
| **Étreinte des dunes** (Grasp of the Dunes, peu commun) | Une main géante de sable surgit autour de l'ennemi visé, le retient, puis le poing se referme et l'écrase. |
| **Voile de sable** (Sand Veil, peu commun) | Des anneaux de sable et des éclats de verre tourbillonnent autour du lanceur 10 s : Résistance I, tirs ennemis détournés, ce qui s'approche est décapé. |
| **Sablier** (Hourglass, rare) | Un grand sablier de verre et d'or au-dessus de la zone visée (5 blocs) : plus son sable s'écoule, plus les ennemis dessous ralentissent ; vide, il se brise sur eux. |
| **Serpent des dunes** (Dune Serpent, rare) | Un cobra de sable nage dans le sol vers l'ennemi le plus proche, se dresse et le mord 3 fois. |
| **Lentille du soleil** (Lens of the Sun, épique) | Une lentille de verre cerclée d'or concentre le soleil en un point brûlant qui poursuit l'ennemi le plus proche 7 s et l'embrase. |
| **Chute de sable** (Sandfall, épique) | Le ciel se déchire et un torrent de sable s'abat 3 s sur la zone visée : dégâts, aveuglement, lourdeur. |
| **Djinn des sables** (Djinn of the Sands, épique) | Un djinn de sable tourbillonnant flotte à l'épaule du lanceur 15 s et lance des traits de sable sur l'ennemi le plus proche. |
| **Simoun** (Simoom, ultime) | Une immense tempête de sable suit le regard du lanceur 12 s : elle attire, fait tournoyer et aveugle les ennemis, sa foudre marque le sol de verre. |

### Magie du Lotus

Le lotus qui sort pur de la vase : pétales tranchants, étang sacré, racines et graines, la lumière de son cœur. Les
fleurs sont de vrais lotus en 3D (anneaux de pétales galbés, blancs à la base et rosissant vers la pointe, veinés, autour
d'un cœur d'étamines dorées et d'une tête à graines), avec leurs grandes feuilles rondes. Livre : **Codex du Lotus**
(soie rose, un lotus sur une feuille de jade) ; focus : nénuphar, pétales roses, tulipe rose, pousse de cerisier.

| Sort | Effet |
|---|---|
| **Pétales tranchants** (Razor Petals, commun) | Cinq pétales en éventail, tournoyants, le bord en feu : chacun tranche le premier ennemi sur son chemin puis éclate en petits pétales. |
| **Bouton de lotus** (Lotus Bud, commun) | Un grand bouton jaillit autour de l'ennemi visé et le retient (la lumière filtre entre ses pétales), puis éclot d'un coup et le projette en l'air. |
| **Étang sacré** (Sacred Pond, peu commun) | Une eau claire couverte de feuilles et de fleurs de lotus autour du lanceur (5 blocs, 10 s) : elle soigne son camp chaque seconde et éteint ses flammes ; les ennemis y sont ralentis et affaiblis. |
| **Trône de lotus** (Lotus Throne, peu commun) | Un grand lotus s'ouvre sous les pieds du lanceur et le porte 10 s, un halo de lumière derrière sa tête : Résistance I, chute lente, soin ; un pétale part frapper l'ennemi le plus proche toutes les 1,5 s. |
| **Racines de la vase** (Roots of the Mire, rare) | Le sol visé devient une boue noire (4 blocs) où remuent les racines noueuses du lotus : les ennemis s'enlisent, sont attirés au centre et saisis toutes les 1,5 s. |
| **Cosse de graines** (Seed Pod, rare) | Une cosse de lotus flotte à l'épaule du lanceur 12 s et tire ses graines sur l'ennemi le plus proche ; un petit lotus éclot là où elles frappent. |
| **Mandala de lotus** (Lotus Mandala, épique) | Un vaste mandala de pétales se dessine en lumière au sol visé (7 blocs) ; ses trois anneaux s'embrasent l'un après l'autre, du cœur vers l'extérieur, frappant, soulevant et marquant ce qui s'y tient. |
| **Lanternes de lotus** (Lotus Lanterns, épique) | 5 à 7 lanternes en forme de lotus, une flamme dans chacune, s'élèvent puis dérivent chacune vers son ennemi et éclatent sur lui en une fleur de feu. |
| **Tempête de pétales** (Petal Storm, épique) | Un tourbillon de pétales enveloppe le lanceur 8 s (4,5 blocs) : les ennemis pris dedans sont tailladés toutes les demi-secondes, emportés et aveuglés. |
| **Lotus aux mille pétales** (Thousand-Petal Lotus, ultime) | Un étang de lumière s'ouvre et un immense lotus en sort, s'ouvrant anneau après anneau ; en pleine éclosion un pilier de lumière tombe à travers son cœur (10 blocs) : ennemis frappés, soulevés, ralentis et marqués, alliés soignés ; puis ses pétales tombent un à un sur les ennemis restants. |

### Magie de l'Aurore

Les aurores boréales : des rideaux de lumière qui pendent en plis ondulants, leurs rayons montant d'un ourlet vert
éclatant vers le turquoise, le bleu, le violet et le rose, des rubans de la même lumière, des étoiles qui scintillent et
des pointes de glace pâle. Livre : **Codex Boréal** (cuir bleu nuit peint d'une aurore au-dessus de pics enneigés, cadre
de filigrane d'argent, une étoile de glace en relief sur chaque plat, des fleurons de cristal au dos) ; focus : glace
bleue, glace compactée, poche d'encre luisante, boule de neige.

| Sort | Effet |
|---|---|
| **Lance boréale** (Aurora Lance, commun) | Une pique de lumière à pointe de glace, trois rubans d'aurore tournoyant derrière elle : elle traverse trois ennemis et les ralentit. |
| **Feux polaires** (Polar Wisps, commun) | 3 à 5 feux follets d'aurore s'élèvent, puis filent chacun vers son ennemi en traînant des rubans de couleur et éclatent en le marquant. |
| **Rideau boréal** (Borealis Curtain, peu commun) | Un rideau d'aurore de 12 blocs se déroule jusqu'au sol en travers du regard, pendant 8 s : il brûle, ralentit et marque les ennemis dedans et détourne les tirs. |
| **Envol boréal** (Aurora Glide, peu commun) | Le lanceur chevauche une vague d'aurore sur une douzaine de blocs ; les ennemis sur son chemin sont frappés, projetés et marqués. |
| **Manteau d'aurore** (Aurora Mantle, rare) | Des rideaux d'aurore enveloppent le lanceur 10 s (sous le niveau des yeux) : Résistance I, Vitesse I, tirs détournés, les ennemis proches brûlés et ralentis. |
| **Bénédiction boréale** (Boreal Blessing, rare) | Des rideaux d'aurore descendent du ciel autour du lanceur (8 blocs) : son camp est soigné, libéré d'un mal, régénéré et protégé. |
| **Spirale céleste** (Celestial Spiral, rare) | Une colonne tourbillonnante de rubans d'aurore au point visé attire, soulève et brûle les ennemis 6 s. |
| **Serpent d'aurore** (Aurora Serpent, épique) | Un serpent tissé d'aurore, à tête et nageoires de glace, tourne dans le ciel 10 s et plonge toutes les 2 s sur l'ennemi le plus proche. |
| **Halo polaire** (Polar Halo, épique) | Un grand halo d'aurore cerclé de glace s'ouvre au-dessus de la zone visée, des rideaux en pendant : des rayons de toutes les couleurs tombent sur les ennemis 6 s. |
| **Nuit boréale** (Boreal Night, ultime) | La nuit tombe sous un dôme d'étoiles (16 blocs) où ondule l'aurore ; quatre immenses rideaux balaient le terrain, puis toutes les lumières se rassemblent en un pilier qui frappe chaque ennemi et soigne le camp du lanceur. |

### Magie Horlogère

Des machines de laiton brossé, de cuivre poli et d'acier bleui : engrenages qui s'engrènent, ressorts, pistons qui
crachent de la vapeur, rivets chauffés au rouge, et des cœurs d'énergie bleue derrière des hublots, d'où jaillissent des
arcs électriques. Livre : **Codex de l'Horloger** (cuir sang-de-bœuf repoussé d'un cadran d'horloge et d'engrenages de
laiton, cadre de laiton riveté, un grand engrenage en relief s'engrenant avec deux petits sur chaque plat, un cœur
bleu à leur centre, une clé de remontage sur le dos) ; focus : horloge, lingot de cuivre, piston, poudre de redstone.

| Sort | Effet |
|---|---|
| **Engrenage-scie** (Saw-Gear, commun) | Un anneau de dents d'acier tournant autour d'engrenages de laiton : il tranche 4 ennemis puis éclate en rouages. |
| **Salve de rivets** (Rivet Volley, commun) | Un éventail de 3 à 6 rivets chauffés au rouge, chacun frappant le premier ennemi sur son chemin. |
| **Bond du ressort** (Mainspring Leap, peu commun) | Un grand ressort s'écrase puis projette le lanceur haut et loin en repoussant les ennemis proches ; à l'atterrissage, un choc d'engrenages frappe autour (3 blocs). Pas de dégâts de chute. |
| **Piège à engrenages** (Gear Trap, peu commun) | Un piège posé au sol (30 s) dont les mâchoires d'acier se referment sur les premiers ennemis, les mordent et les retiennent 3 s. |
| **Tourelle mécanique** (Clockwork Turret, rare) | Une tourelle de laiton sur trépied, aux canons tournants, tire 10 s sur l'ennemi visible le plus proche (14 blocs). |
| **Cage gyroscopique** (Gyroscopic Cage, rare) | Trois anneaux dentés tournoient autour de l'ennemi visé : retenu au centre, frappé par des arcs, puis un dernier coup quand ils éclatent. |
| **Automate de laiton** (Brass Automaton, épique) | Une machine de près de 3 blocs combat 20 s : elle marche vers l'ennemi le plus proche, arme son bras à piston et frappe ; elle a une clé de remontage qui tourne dans le dos. |
| **Locomotive de laiton** (Brass Locomotive, épique) | Des rails se posent sur 24 blocs à côté du lanceur, puis une locomotive à vapeur rouge et laiton fonce dessus et écarte chaque ennemi sur la voie. |
| **Bombardement en dirigeable** (Airship Bombardment, épique) | Un dirigeable de toile et de laiton survole la zone visée en larguant 10 bombes qui éclatent dans le feu et la fumée. |
| **Grand Mécanisme** (Great Mechanism, ultime) | Un grand engrenage sous le lanceur, un anneau denté autour du terrain (12 blocs), huit grands engrenages et des pistons à vapeur, un astrolabe au cœur d'énergie bleue qui lance des arcs ; puis la surcharge : une vague de lumière bleue frappe et projette tous les ennemis et protège le camp du lanceur. |

### Magie de la Musique

Des notes d'or qui brillent, des portées de lumière aux cinq lignes qui changent de couleur (rose, violet, or), une clé
de sol d'or et des instruments fantômes : tambour de guerre, harpe, trompettes, orgue et violons. Livre : **Codex de la
Symphonie** (velours prune parcouru de portées d'or chargées de notes, cadre d'or à petites lyres, une lyre d'or en relief
sur chaque plat sur une lueur rose, des chevilles et des cordes d'or sur le dos, une volute de violon à chaque bout) ;
focus : bloc musical, juke-box, corne de chèvre, éclat d'améthyste.

| Sort | Effet |
|---|---|
| **Volée de notes** (Note Volley, commun) | 3 notes d'or (jusqu'à 5), chacune s'incurvant vers l'ennemi le plus devant elle, une petite portée de lumière derrière elle. |
| **Tranche de portée** (Staff Slash, commun) | Une portée de cinq lignes de lumière balayée devant le lanceur comme une lame (4,5 blocs, en arc) : frappe et repousse. |
| **Mélodie apaisante** (Soothing Melody, peu commun) | Un air doux pendant 5 s : chaque seconde, soigne le lanceur et ses alliés proches (6 blocs) et chasse poison et flétrissement. |
| **Crescendo** (peu commun) | Des anneaux de son qui roulent en grandissant : plus ils ont voyagé, plus ils frappent fort et repoussent loin. |
| **Berceuse** (Lullaby, rare) | Une clé de sol d'or au-dessus de la zone visée (5 blocs) : les ennemis s'endorment, immobiles, jusqu'à ce qu'un coup les réveille ou qu'ils sortent de la zone. |
| **Tambour de guerre** (War Drum, rare) | Un grand tambour rouge cerclé d'or frappé par deux mailloches : chaque coup frappe les ennemis autour (6 blocs) et donne force et vitesse au camp du lanceur. |
| **Harpe de lumière** (Harp of Light, rare) | Une harpe d'or flotte au-dessus de l'épaule du lanceur 8 s ; chaque corde pincée lance un rayon sur l'ennemi visible le plus proche (18 blocs). |
| **Trompettes fantômes** (Phantom Trumpets, épique) | Cinq trompettes d'or en arc derrière le lanceur sonnent trois fanfares : chaque sonnerie frappe et projette les ennemis devant lui (14 blocs). |
| **Orgue de la tempête** (Storm Organ, épique) | Un grand orgue aux tuyaux d'or s'élève là où l'on vise : chaque accord fait tomber une colonne de lumière sur un ennemi (12 blocs), puis un dernier grand accord frappe et projette tous les ennemis. |
| **Orchestre fantôme** (Phantom Orchestra, ultime) | Huit violons en cercle, des portées de lumière qui tournent, une grande clé d'or : des notes tombent sur les ennemis, le camp du lanceur est soigné, accéléré et renforcé, les ennemis ralentis ; puis le final frappe tout le terrain (14 blocs) et protège le camp du lanceur. |

### Magie des Soins

Des soins qui ne touchent qu'un seul allié à la fois, chacun à sa façon : on vise un allié (sinon soi-même). Lumière verte
et dorée. Livre : **Codex de la Guérison** (cuir ivoire, un arbre de vie d'or aux feuilles vertes, un cœur d'émeraude) ;
focus : pomme d'or, tranche de pastèque scintillante, larme de ghast, baies sucrées.

| Sort | Effet |
|---|---|
| **Fil de vie** (Lifethread, commun) | Un fil de lumière dorée relie le lanceur à l'allié : il le soigne chaque demi-seconde pendant 6 s, tant qu'il reste à moins de 16 blocs. |
| **Suture** (commun) | Soigne d'autant plus que l'allié est blessé (jusqu'au double au bord de la mort). |
| **Soin en écho** (Echoing Mend, peu commun) | Un soin, puis deux échos 2 s et 4 s après, chacun moitié du précédent. |
| **Purge** (peu commun) | Retire tous les effets négatifs et soigne pour chacun retiré. |
| **Bourgeon de soin** (Mending Bud, rare) | Un bourgeon de lumière au-dessus de l'allié (20 s) qui éclot quand il passe sous la moitié de sa vie : grand soin et régénération. |
| **Feu follet gardien** (Guardian Wisp, rare) | Un feu follet tourne autour de l'allié et soigne la moitié de chaque blessure, un instant après. |
| **Pacte de sang** (Blood Pact, rare) | Le lanceur donne un peu de sa vie : l'allié est soigné de trois fois ce coût (jamais le dernier cœur). |
| **Rembobinage** (Rewind, épique) | La vie de l'allié revient au plus haut qu'elle a été dans les 5 dernières secondes. |
| **Cocon** (Cocoon, épique) | 3 s dans un cocon de lumière : aucun coup ne passe, l'allié guérit mais ne peut pas bouger. |
| **Seconde vie** (Second Life, ultime) | Une auréole d'or : si l'allié devait mourir pendant qu'elle brille, il se relève à mi-vie, purifié et protégé. |

### Magie des Pièges

Des pièges cachés au sol, à peine visibles, qui ne paralysent jamais : aucun étourdissement, aucun enracinement, seulement
des affaiblissements. Livre : **Codex du Trappeur** (cuir sombre cousu, une mâchoire de fer et un œil vert poison sur
chaque plat, fermoirs de laiton) ; focus : crochet, ficelle, œil d'araignée, œil d'araignée fermenté. La puissance des sorts
allonge les malédictions.

| Sort | Effet |
|---|---|
| **Collet de ronces** (Bramble Snare, commun) | Poison et lenteur. |
| **Glyphe de faiblesse** (Hex Glyph, commun) | Faiblesse II et coups ralentis. |
| **Saigneur** (Bloodletter, peu commun) | Saignement. |
| **Mine de suie** (Soot Mine, peu commun) | Un nuage noir : cécité et marque lumineuse sur tous les ennemis proches (3,5 blocs). |
| **Mâchoire de rouille** (Rust Jaw, rare) | Rouille et armure brisée. |
| **Piège à écho** (Echo Trap, rare) | Se déclenche 5 fois, sur chaque ennemi qui passe ; à chaque reprise du même ennemi, lenteur et faiblesse montent d'un cran (jusqu'à III). |
| **Porteur de peste** (Plague Carrier, rare) | Poison et faim ; pendant 8 s la victime transmet la peste aux ennemis proches. |
| **Piège sangsue** (Leech Trap, épique) | Flétrissement ; tant qu'il dure, le lanceur se soigne chaque seconde. |
| **Champ de mines** (Minefield, épique) | Sept mines autour du point visé, chacune une malédiction différente. |
| **Toile maudite** (Cursed Web, ultime) | Une toile de 10 blocs : toutes les 2 s, chaque ennemi dedans subit une nouvelle malédiction, à tour de rôle. |

### Magie de la Neige

Cinq sorts de neige et de glace, aucune invocation. Livre : **Codex du Premier Givre** (First Frost Codex) ; focus : boule
de neige, seau de neige poudreuse, glace compactée, glace bleue.

| Sort | Effet |
|---|---|
| **Boule d'avalanche** (Avalanche Ball, commun) | Une boule de neige roule devant vous en grossissant, renverse et refroidit tout ce qu'elle croise, puis éclate en poudreuse. |
| **Globe de neige** (Snow Globe, peu commun) | Un dôme de verre se referme sur l'ennemi visé ; la neige tourbillonne et gèle dedans, personne ne sort, puis le verre se brise sur eux. |
| **Glissade de givre** (Frost Glide, peu commun) | Vous glissez en avant sur un ruban de glace qui reste derrière vous et ralentit, gèle et blesse ceux qui le traversent. |
| **Couronne de givre** (Hoarfrost Crown, rare) | Une couronne de cristaux tourne au-dessus de votre tête et tire des stalactites sur l'ennemi visible le plus proche. |
| **Voile blanc** (Whiteout, ultime) | Un mur de blizzard autour de vous : les ennemis dedans sont aveuglés, ralentis et gèlent un peu plus chaque seconde ; à la fin, ceux gelés de part en part volent en éclats. |

### Magie de la Lave

Cinq sorts de roche en fusion, aucune invocation. Livre : **Codex de Magma** (Magma Codex) ; focus : bloc de magma, crème de
magma, seau de lave, obsidienne.

| Sort | Effet |
|---|---|
| **Crachat de magma** (Magma Spit, commun) | Une boule de magma lobée là où vous visez : elle éclabousse et laisse une flaque bouillonnante qui brûle. |
| **Geyser de lave** (Lava Geyser, peu commun) | Le sol se fend et rougeoie, puis une colonne de lave jaillit, projette et brûle ceux qui sont dessus. |
| **Carapace d'obsidienne** (Obsidian Carapace, rare) | Des plaques d'obsidienne veinées de lave flottent autour de vous : moins de dégâts, pas de feu, et qui vous frappe au corps à corps prend feu. |
| **Rivière de lave** (Lava River, rare) | Une rivière de lave coule devant vous, ralentit et brûle ceux qui y entrent, puis refroidit en croûte noire. |
| **Caldeira** (Caldera, ultime) | Le sol tremble et se fissure tout autour de vous, puis s'effondre en lac de magma qui projette les ennemis en l'air et les brûle. |

### Magie des Plantes

Cinq sorts de fleurs, de pollen et de soleil, aucune invocation. Livre : **Codex Herbier** (Herbarium Codex) ; focus :
poudre d'os, bloc de mousse, azalée fleurie, sporophore.

| Sort | Effet |
|---|---|
| **Floraison d'épines** (Thorn Bloom, commun) | Un bouton sort du sol, s'ouvre en grande fleur et projette ses épines tout autour : saignement. |
| **Nuage de pollen** (Pollen Cloud, peu commun) | Un nuage de pollen doré : les ennemis éternuent, titubent et faiblissent ; vous et vos alliés dedans êtes soignés. |
| **Salve de graines** (Seed Volley, peu commun) | Un éventail de graines qui se plantent, germent en vesses-de-loup et éclatent en spores empoisonnées. |
| **Photosynthèse** (Photosynthesis, rare) | Un rayon de soleil tombe sur vous : vous et vos alliés proches êtes soignés chaque seconde, deux fois plus à ciel ouvert le jour. |
| **Gueule verdoyante** (Verdant Maw, ultime) | Une immense fleur carnivore jaillit du sol, referme ses mâchoires sur les ennemis, les mâche, et chaque bouchée vous soigne. |

### Magie de l'Acide

Cinq sorts d'acide qui brûlent la chair et rongent les armures, aucune invocation. L'acide brise l'armure (effet
« armure brisée ») et use la durabilité des pièces portées. Livre : **Codex Corrosif** (Corrosive Codex) ; focus : boule de
slime, œil d'araignée, œil d'araignée fermenté, pomme de terre empoisonnée.

| Sort | Effet |
|---|---|
| **Jet d'acide** (Acid Spray, commun) | Un éventail d'acide projeté de la main (6,5 blocs) : dégâts, armure brisée, armure portée rongée. |
| **Flaque corrosive** (Corrosive Pool, peu commun) | Une flaque d'acide fumante là où vous visez : dégâts chaque seconde et armure rongée pour qui reste dedans. |
| **Pluie acide** (Acid Rain, rare) | Un nuage verdâtre pleut de l'acide sur la zone : dégâts, faiblesse, armure rongée. |
| **Dissolution** (Dissolve, épique) | Une bulle d'acide se referme sur l'ennemi visé, ronge son armure quelques secondes puis éclate et le fait fondre. |
| **Marée corrosive** (Corroding Tide, ultime) | Une vague d'acide déferle en anneau autour de vous (12 blocs) : repousse, faiblesse, armure brisée et rongée. |

### Téléphones (mod à part)

Les téléphones ne font plus partie d'Arcanes Oubliés : ils sont dans le mod séparé **Téléphones d'Époque & RP**
(dossier `era-phones/`, son propre `.jar`), avec les commandes de jeu de rôle.

### Lecteur de musique

Un lecteur de musique en jeu, ouvert avec la touche **M** (modifiable dans les commandes, catégorie Arcanes Oublies).

- **Ta musique** : dépose tes fichiers **MP3, OGG ou WAV** dans le dossier `arcanes_music` du jeu (à côté de `saves` et
  `mods` ; le bouton dossier du lecteur l'ouvre, les sous-dossiers sont lus aussi), puis clique sur Actualiser. Le titre
  et l'artiste écrits dans le fichier (tags ID3, commentaires Vorbis) sont affichés, sinon le nom du fichier. Une valse
  originale, « Valse des Arcanes », est fournie avec l'addon pour essayer.
- **Le son est sur toi** : la musique joue depuis ton personnage, le suit et s'entend de plus en plus fort en
  s'approchant. Tu choisis sa **portée** (8, 16, 24, 32, 48 ou 64 blocs, ou « Moi » pour être seul à l'entendre) et son
  **volume**. En multijoueur, les joueurs dans ta portée l'entendent : le fichier leur est transmis par le serveur
  (24 Mo au plus) ; le volume « Disques » des options règle ce que chacun entend.
- **Lecture** : lecture/pause, précédent, suivant, barre de progression cliquable et glissable, aléatoire, répétition
  (aucune, tout, ce titre). La musique continue quand le lecteur est fermé, une note s'affiche en haut à droite quand
  un titre commence, et la musique du jeu s'efface pendant ce temps.
- **Favoris et playlists** : l'étoile d'un titre le met dans les favoris ; les playlists se créent avec +, se remplissent
  avec le + de chaque titre, se vident avec la croix et se suppriment avec la corbeille. Recherche par titre ou artiste.
  Le tout est gardé dans `config/arcanes_oublies-music.json`.
- **L'écran** : une fenêtre bleu nuit cadrée d'or (une lyre en haut, des volutes aux coins), un disque qui tourne sous
  son bras de lecture, des barres qui dansent sur la musique.

Décodage MP3 : JLayer (javazoom, LGPL), embarqué dans le jar de l'addon.

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
| **Météore guidé** | 2 | 30 s | Un météore tombe lentement du ciel là où regarde le meneur (64 blocs) : explosion de feu sur les créatures hostiles (aucun bloc cassé). |
| **Dôme de protection** | 2 | 40 s | Un dôme de lumière de 16 blocs pendant 5 min : les créatures hostiles ne peuvent pas entrer, leurs tirs se brisent dessus, les joueurs dessous résistent aux dégâts. |
| **Appel de la tempête** | 2 | 30 s | Un orage d'une minute au-dessus du lieu (24 blocs) : la foudre frappe une créature hostile toutes les 1,5 s. |
| **Portail** | 2 | 20 s | Un portail d'une minute vers l'ancre marquée avec la craie du meneur (accroupi + clic sur le sol, même dans une autre dimension), et son jumeau pour revenir. |
| **Bénédiction des anciens** | 2 | 45 s | Chaque joueur à 24 blocs est soigné, purifié et renforcé 5 min (Régénération, Absorption, Résistance, Force). |
| **Éveil du colosse** | 2 | 60 s | Un colosse géant (golem 2,5 fois plus grand, 300 PV) s'éveille et combat pour le groupe pendant 3 min. |

Les rituels n'épargnent que leurs participants et ne visent que les créatures hostiles : ils ne blessent jamais les
autres joueurs.

Les sons sont des sons de Minecraft combinés. Les textures sont dessinées par `tools/gen_assets.py`
(`tools/explosion_assets.py` pour l'Explosion, `tools/book_assets.py` pour le Livre, `tools/errethys_assets.py`
pour Errethys, `tools/physical_assets.py` et `tools/physical_weapon_icons.py` pour Caractéristique Physique,
`tools/boss_assets.py` pour la Magie de Boss, `tools/rune_assets.py` pour la Magie Runique, `tools/glyph_assets.py`
pour la Magie de Glyphe, `tools/token_assets.py` pour la Magie des Jetons à partir de `tools/models/soul_token.bbmodel`,
`tools/verdigris_assets.py` pour la Magie de Mousse, Cuivre et Rouille, `tools/wall_assets.py` pour la Magie des Murs, `tools/gem_assets.py` pour la Magie des Pierres Précieuses).

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

## Jeu de rôle et outils des maîtres du jeu (venus d'Époque RP)

Ces commandes étaient dans Époque RP ; elles sont maintenant ici (et plus là-bas). Les mondes déjà joués gardent les
noms des personnages, les tailles, PV et vitesses, la jauge de peur et les déclencheurs. Simple Voice Chat est
facultatif (il sert à `/voix` et à la voix de `/voir`). Avec Époque RP installé aussi, les déclencheurs de zone
peuvent couper le courant (`panne`, `clignote`), casser une lampe torche (`lampe`) et envoyer un SMS (`sms`) ; sans
lui, ces actions ne font rien.

### Narration

| Commande | Effet |
| --- | --- |
| `/narration [portée] <texte>` | Narration sur l'écran (bandes noires de cinéma, texte qui s'écrit) et dans le chat, sous la forme **`✦ [NARRATION] : texte`** (doré et parchemin). Sans portée : tout le serveur ; avec une portée (`/narration 30 La pluie redouble.`) : les joueurs à 30 blocs ou moins. |
| `/narrationalone <joueurs> <texte>` | La même chose pour un seul joueur (ou ceux choisis). |
| `/narrationchat [portée] <texte>` | Narration seulement dans le chat (rien à l'écran), à tous ou dans la portée. |
| `/narrationchatalone <joueurs> <texte>` | Pareil, pour un ou quelques joueurs. |

### Outils d'événement

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

### Taille des joueurs : `/taille`

`/taille <joueurs> <cm>` (de 50 à 400 cm, 180 = normal), `/taille <joueurs> normale`, `/taille` (sa propre taille, pour tout le monde). Tout suit la taille : le corps dessiné, la hitbox, la hauteur des yeux (donc ce qu'on voit), la caméra à la 3e personne, la portée des mains, la marche qu'on peut monter, un peu la vitesse et le saut, la chute sans se blesser. Gardé même après la mort et la déconnexion. Permission : comme `/horreur`.

### Outils du staff

### Atelier d'armes : `/arme`
Tiens un objet (épée, hache, armure, n'importe quoi) et tape `/arme`. L'atelier s'ouvre : l'objet en haut, quatre onglets, et à droite l'**aperçu** de l'objet tel qu'il sera (nom, lore, stats, effets), mis à jour pendant que tu écris.
- **Général** : son nom, incassable, brillance. Pastilles de couleur : un clic ajoute le code (`&c` rouge, `&6` or…), plus `&l` gras, `&o` italique, `&n` souligné, `&r` normal. **Durabilité** : restante et maximum (le bouton *Max* la remplit) ; un objet sans durabilité en reçoit une si on lui met un maximum (il ne s'empile plus). Cases vides : on n'y touche pas.
- **Lore** : une grande zone de texte, jusqu'à **16 lignes** (Entrée pour changer de ligne), avec les mêmes pastilles de couleur.
- **Stats** : dégâts, vitesse d'attaque (comme affichés sous l'objet : une main nue = 1 et 4), armure, robustesse, résistance au recul, vitesse (%), PV max (+), portée (+). Une case vide = rien.
- **Effets %** : jusqu'à 8 lignes. Pour chacune :
  - **Quand** : *Frappe* (quand celui qui le tient tape) ou *Défense* (quand celui qui le porte ou le tient est touché) ;
  - **l'effet** : clic dessus ouvre la liste de **tous les effets du jeu et de tous les mods installés**, avec leur icône, qu'on fait défiler à la molette, et une **barre de recherche** (Entrée prend le premier). En tête, les actions de l'atelier : enflamme, foudre, recul, vol de vie, dégâts ±, épines, soin, gel ;
  - **Sur** : *Autre* (celui qu'on frappe, ou celui qui nous frappe) ou *Soi* ;
  - la **chance en %**, la **force** (niveau de l'effet, ou points, ou % pour le vol de vie), les **secondes**.
  - Sous l'objet : « ✦ 5 % en frappant → l'autre : Poison II (5 s) ».

### Warps

| Commande | Effet |
| --- | --- |
| `/warp <nom>` | Aller au warp (les majuscules ne comptent pas : `Taverne`, `taverne`, `TAVERNE`). Tout le monde (`warpPermission`, 0). |
| `/warp <nom> <joueurs>` | Envoie ces joueurs au warp (un pseudo ou un sélecteur : `@a`, `@a[distance=..10]`…). Maîtres du jeu (`setwarpPermission`). |
| `/warplist` | La liste des warps ; un clic sur un nom y emmène. Tout le monde. |
| `/warpgrp <portée> <nom>` | Envoie au warp **tous les joueurs** à moins de *portée* blocs de toi (toi compris) : `/warpgrp 15 taverne`. Maîtres du jeu. |
| `/back [joueurs]` | Ramène le joueur là où il était **avant sa dernière téléportation** (`/tp`, warp, `/warpgrp`, `/back` : deux `/back` font l'aller-retour). Gardé même après une mort ou une déconnexion. Maîtres du jeu (`backPermission`, 2). |
| `/setwarp <nom>` | Crée le warp là où tu es (même direction du regard), ou le déplace. Maîtres du jeu (`setwarpPermission`, 2). |
| `/delwarp <nom>` | Supprime le warp. Maîtres du jeu. |

Les warps sont gardés avec le monde, dans toutes les dimensions.

### Métiers : la meule

Clic droit sur une **meule** avec une arme (ou tout objet qui a une durabilité) abîmée : le mini-jeu d'affûtage
s'ouvre. Maintiens le clic gauche (ou Espace) pour appuyer la lame contre la pierre et garde la **pression dans la bande
verte**, qui se déplace ; trop léger, rien ne se passe, trop fort, la lame chauffe et, si elle bleuit, c'est raté.

- réussite critique : **+200** de durabilité ; réussite : **+100** ; échec : **−50** (l'arme ne se casse jamais) ;
- chaque arme ne peut repasser à la meule que **12 heures** (temps réel) après, avec un message en jeu de rôle ;
- une arme sans usure ne lance pas le mini-jeu.

Maîtres du jeu (`repairPermission`) : `/meule interdire` (tous les objets du type tenu en main),
`/meule interdire objet` (cet objet seulement), `/meule autoriser [objet]`, `/meule liste`, `/meule reset` (l'objet
tenu peut repasser tout de suite). Une arme interdite affiche « *Cette arme ne semble pas réparable.* ».

Minage sans pioche : « *Il semble qu'une pioche soit nécessaire pour récupérer ce minerai…* ».

### Métiers : stations

Les stations (table de craft, forge, alambic…) n'ont plus de bouton « Utilisation classique » : on ne s'en sert que par
les recettes du métier. Les cartes de recettes sont plus compactes (jusqu'à six visibles d'un coup).

### `/invsee` et `/repair`

- `/invsee <joueur>` : ouvre l'inventaire du joueur (en ligne) dans un coffre. Lignes 1 à 3 : son sac ; ligne 4 : sa
  barre d'objets ; ligne 5 : casque, plastron, jambières, bottes, main gauche. On peut prendre et mettre des objets :
  son inventaire change tout de suite. `invseePermission` (2).
- `/repair full` : la durabilité de l'objet en main remise au maximum ; `/repair <n>` : remise à *n* (de 1 au maximum de
  l'objet). `repairPermission` (2).

### Porter quelqu'un
Accroupi + clic droit main vide sur un joueur : il est tout de suite porté dans les bras (sans demande), allongé, devant le porteur, qui marche plus lentement. Accroupi + clic droit main vide (ou `/porter lacher`) : on le pose. Le porté descend en s'accroupissant. Staff : `/porter <porteur> <porté>`.

### `/vitesse` et `/hp`
`/vitesse <joueurs> <fois>` (1 = normal, 0.5 = moitié, 2 = double) ou `normale`. `/hp <joueurs> <points>` (20 = normal = 10 cœurs) ou `normal`. Gardés après la mort et la déconnexion. Les **effets** qu'un joueur a quand il meurt (donnés par `/effect` ou autrement) lui sont rendus quand il réapparaît, avec le temps qui leur restait.

### Jauge de peur : `/peur jauge`
Chaque joueur a une peur de 0 à 100 %. Elle **monte** dans le noir (une lampe torche allumée, une torche ou une lanterne en main comptent comme de la lumière), seul, la nuit dehors, blessé, avec des monstres autour, et d'un coup quand le staff lui envoie quelque chose (`/horreur` sons, murmure, coupure, sang, ballon, `/peur`, coups reçus). Elle **descend** à la lumière et à plusieurs. À partir d'un tiers : le cœur bat, de plus en plus vite, les bords de l'écran s'assombrissent au rythme du cœur ; au-delà des deux tiers : la vue tremble et se brouille un peu. Un petit cœur et un tracé de moniteur en bas à gauche la montrent.
**Le staff** (ceux qui peuvent utiliser `/horreur`), et les joueurs en créatif ou spectateur, **ne ressentent rien**.
- `/peur jauge on` / `/peur jauge off` : l'active ou la coupe pour tout le serveur (gardé avec le monde) ;
- `/peur jauge` ou `/peur jauge info` : l'état, et la peur de chaque joueur ;
- `/peur jauge <joueurs> <0-100>` : la régler à la main.
- `/peur jauge test` : **toi (staff), tu la ressens comme un joueur** pour l'essayer (à nouveau : arrêter) ; `/peur jauge test <0-100>` règle la tienne directement (même si la jauge est coupée).

### Commandes de jeu de rôle

| Commande | Effet |
| --- | --- |
| `/setname Prénom Nom` | Le nom du personnage remplace le pseudo partout (au-dessus de la tête, liste des joueurs, tout ce qui est dit). |
| *(chat normal)* | Parole entendue autour (20 blocs) : « Bill Denbrough dit : « … » ». |
| `/me [portée] action` | Action en violet italique, dans le chat (rien au-dessus de la tête). 25 blocs par défaut. |
| `/do [portée] description` | Description de la scène (ambre). 20 blocs par défaut. |
| `/roll [portée] [100]` | « Bill Denbrough a lancé un dé sur 100 : 42 » (dé sur 100 par défaut ; aussi `1d20`, `2d6`…). Avec une portée : `/roll 30 1d20`. |
| `/try [portée] action` | Tente quelque chose : réussite ou échec. |
| `/chuchoter [portée] message` | 4 blocs ; un peu plus loin on voit seulement que quelqu'un chuchote. |
| `/crier [portée] message` | 48 blocs. |
| `/hrp [portée] message` | Hors personnage, à tout le monde (ou dans la portée) ; seul endroit où le pseudo apparaît. |

La **portée** est un nombre de blocs mis avant le texte : `/me 40 lève la main` est vu à 40 blocs (200 au plus,
`maxRange`). Un texte qui commence par un nombre est donc lu comme une portée (`/me 3 pommes…` : écrire `/me trois pommes…`).
| `/desc texte` · `/examiner joueur` | Ce que les autres voient du personnage. |
| `/voir joueur` · `/voir tous` · `/voir personne` | N'être **vu et entendu que** par ce(s) joueur(s) : pour les autres le personnage n'existe plus (ni dessiné, ni entendu, ni touchable), ses paroles ne leur parviennent pas, et avec Simple Voice Chat sa voix non plus. Réservé aux maîtres du jeu par défaut (`voirPermission`). |
| `/pseudos cacher` · `/pseudos afficher` | Les pseudos au-dessus des têtes sont **cachés par défaut** ; `/pseudos afficher` les réaffiche (et `/pseudos cacher` les recache), pour tout le monde : passe par une équipe du tableau des scores (« nom jamais visible »), que le jeu de base respecte sur chaque client, avec ou sans le mod (Essential compris, qui s'accroche au nom du jeu). Les joueurs sont alors dans l'équipe `eraphones_rp` (un joueur ne peut être que dans une équipe). Maîtres du jeu. |
| `/rp` · `/rp qui` · `/rp nom joueur Prénom Nom` | Aide ; pour les maîtres du jeu : qui joue qui, nommer un personnage. |

Réglages serveur (`serverconfig/arcanes_oublies-roleplay-server.toml`) : chat local oui/non, portées (parole, `/me`, actions,
chuchotement, cri, portée maximale choisie par un joueur), permissions de `/voir`, des warps, de `/invsee` et de
`/repair`, noms au-dessus des têtes, bulles au-dessus des têtes (désactivées par défaut).

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
dans `serverconfig/arcanes_oublies-roleplay-server.toml`.

## Custom NPCs avec Epic Fight

Si **Epic Fight** et **Custom NPCs** (NeoForge 1.21.1) sont installés avec Arcanes Oubliés, les PNJ humains de
Custom NPCs (types `customnpc`, `customnpc64x32`, `customnpcalex`, `customnpcclassic`) prennent le corps d'Epic Fight :

- **démarche** : attente, marche, course, chute, mort et réactions aux coups d'Epic Fight, avec la tenue des armes à
  deux mains (épée à deux mains, lance, épée longue, tachi, uchigatana) ;
- **attaques** : l'IA de Custom NPCs décide toujours qui le PNJ attaque, comment il se déplace et quand il frappe (tout
  ce qui est réglé dans ses menus reste valable). Quand il frappe, son coup Minecraft est supprimé (ni dégâts, ni geste
  du bras) et l'animation d'attaque Epic Fight de son arme se joue (enchaînement de coups). Le seul coup porté est celui
  de l'animation : c'est un coup Epic Fight, avec la force de mêlée et les effets réglés dans Custom NPCs, et il ne
  touche que l'ennemi du PNJ ;
- **apparence** : le corps suit la skin (bras normaux, bras fins « Alex », anciennes skins 64×32 et « classic »), les
  calques cachés dans le menu du modèle restent cachés, la taille réglée dans Custom NPCs est gardée, et l'arme et
  l'armure s'affichent ;
- **PNJ déguisés en mob** (menu du modèle de Custom NPCs) : en zombie, husk, noyé, zombie-villageois, illageois
  (vindicateur, pillard, évocateur, illusionniste) ou sorcière, le PNJ est dessiné avec le corps Epic Fight de ce mob,
  donc avec sa vraie texture, ses animations et son arme. Déguisé en un autre mob (animal, araignée, squelette,
  piglin…), il garde le rendu normal de Custom NPCs, avec son arme.

- **réglage par PNJ** : dans le menu d'édition du PNJ (houe NPC), un bouton **« Combat Epic Fight : Oui / Non »** à côté
  du panneau de Custom NPCs. « Non » : le PNJ se bat comme dans Custom NPCs (coup et geste Minecraft classiques, rendu de
  Custom NPCs). Réservé aux opérateurs et aux joueurs en créatif. Le choix est gardé avec le PNJ (c'est le tag
  `arcanes_sans_epicfight`, qu'on peut aussi poser avec `/tag`).

Un PNJ qu'Epic Fight ne peut pas animer (déguisé en animal, araignée, squelette, piglin…, ou avec « Basic Limited
Renderer ») se bat toujours comme dans Custom NPCs : ainsi aucun coup n'est porté sans animation visible. Le corps
Epic Fight d'un PNJ humain est choisi d'après la taille de sa skin (une skin 64×32 prend le corps à l'ancienne mise en
page, quel que soit le type de PNJ).

Rien n'est à installer en plus : c'est un pack de données intégré à Arcanes, activé tout seul. Sans l'un des deux mods,
rien ne change.

Limites connues : avec l'option **« Basic Limited Renderer »** de Custom NPCs, le PNJ déguisé est dessiné par Custom NPCs
sans les animations d'Epic Fight, et il se bat comme dans Custom NPCs (son arme s'affiche) ; laisse-la désactivée pour
avoir les animations. Les parties
ajoutées dans Custom NPCs (oreilles, queue, ailes, tailles de membres) ne s'affichent pas sur le corps Epic Fight.
