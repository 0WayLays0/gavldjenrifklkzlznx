# Arcanes Oubliés

Addon pour **Iron's Spells 'n Spellbooks** (Minecraft 1.21.1, NeoForge 21.1.221+), pour notre serveur RP.
La dernière version pour Minecraft 1.20.1 (Forge) est la 0.9.0 (commit `6359b13` de l'historique).
La liste complète des écoles et des sorts prévus est dans [`ADDONS_SPELLS.txt`](ADDONS_SPELLS.txt).

## Contenu de la bêta 1.0.0 : écoles de l'Or, de l'Explosion, du Livre, d'Errethys, Caractéristique Physique et Magie de Boss

Quatre nouvelles écoles complètes dans Iron's Spells, chacune avec ses attributs « Puissance des sorts » et
« Résistance », ses parchemins et son livre de sorts 3D (+10 % puissance de l'école, +200 mana ; 10 emplacements,
12 pour le Grimoire Vivant), plus l'école d'invocation Errethys (5 sorts, pas encore de livre) et les techniques de
combat de Caractéristique Physique (37 techniques pour 9 styles d'armes, ce n'est pas de la magie).

**Onglets créatifs** : un onglet par école (« Arcanes Oubliés : Or », « … : Explosion », « … : Livre »,
« … : Errethys », « … : Caractéristique Physique », « … : Magie de Boss »), côte à côte. Chacun contient le livre de
l'école puis le parchemin de chacun de ses sorts à tous les niveaux, dans l'ordre (style par style pour
Caractéristique Physique).

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

Des animaux de mana bleu ciel transparent, aux veines de lumière qui brillent de l'intérieur. Ils se battent pour
le lanceur (ce qu'il attaque, ce qui l'attaque, et les monstres proches), ne touchent jamais ses alliés, ne laissent
rien en mourant et se dissolvent en étincelles quand leur temps est écoulé.

| Sort | Effet |
|---|---|
| **Rat de Mana** (Mana Rat) | Un rat rapide (queue qui ondule) qui suit le lanceur et mord ses ennemis, 30 s ou plus. |
| **Saumon de Mana** (Mana Salmon) | Un saumon qui ne vit que dans l'eau : il apparaît dans l'eau que regarde le lanceur (ou la plus proche, 6 blocs ; sinon le sort échoue), y nage comme un saumon, suit le lanceur le long de l'eau et percute les ennemis dans l'eau ou au bord. Échoué sur la terre ferme, il se débat puis se dissout au bout de 3 s. |
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
| **Dash** | 3 à 4 blocs d'un coup dans le sens où l'on se déplace déjà (en arrière ou de côté pour esquiver), sinon droit devant ; s'arrête net. |
| **Saut** (Leap) | Un saut environ deux fois plus haut qu'un saut normal (2,4 à 3 blocs, jamais de dégâts de chute en retombant au même niveau), en gardant l'élan ; pieds au sol seulement. |
| **Esquive** (Evade) | Un grand bond en arrière pour sortir d'un combat : 6 à 8 blocs, en arc bas, sans dégâts de chute ; pieds au sol seulement. |
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

Les sons sont des sons de Minecraft combinés. Les textures sont dessinées par `tools/gen_assets.py`
(`tools/explosion_assets.py` pour l'Explosion, `tools/book_assets.py` pour le Livre, `tools/errethys_assets.py`
pour Errethys, `tools/physical_assets.py` et `tools/physical_weapon_icons.py` pour Caractéristique Physique,
`tools/boss_assets.py` pour la Magie de Boss).

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
  `book_assets.py`, `errethys_assets.py`, `physical_assets.py`, `boss_assets.py`) puis lancer
  `python3 tools/gen_assets.py` (Pillow).
  Une texture faite à la main peut aussi remplacer directement le PNG du même nom.
- Animations des techniques et des lanceurs de Magie de Boss : `tools/physical_animations.py` et
  `tools/boss_animations.py`, écrites avec le petit squelette de `tools/anim_rig.py`
  (hanches, buste, bras qui suivent les épaules, main gauche qui saisit les armes à deux mains, une clé par tick).
  `python3 tools/anim_preview.py <fichier.json> <animation> <sortie.png>` dessine une animation tick par tick (de
  profil, de trois quarts et de dessus) comme Player Animator la joue, pour la vérifier avant de lancer le jeu.
