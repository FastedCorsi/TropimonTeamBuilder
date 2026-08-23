# Changelog

## 0.59.3

- Dans le sélecteur d'attaques, chaque groupe est maintenant trié par taux d'utilisation Ranked décroissant pour la saison sélectionnée.
- Les attaques sans statistique d'utilisation conservent un ordre alphabétique stable.

## 0.59.2

- Le libellé IV qui chevauchait l'en-tête PV/HP dans la liste Possédés a été retiré.
- Les IV utilisent une échelle plus lisible : rouge de 0 à 10, noir de 11 à 20, vert de 21 à 30 et jaune pour 31.

## 0.59.1

- L'onglet Possédés affiche les IV réels dans les colonnes PV, Atq, Déf, Atq Spé, Déf Spé et Vit ; l'onglet Tous conserve les statistiques de base.
- Les IV sont colorés selon leur qualité et restent triables dans les deux directions depuis les en-têtes.
- Les surnoms apparaissent en priorité dans la liste, avec l'espèce, la boîte et le niveau sur la ligne secondaire.
- L'infobulle affiche le surnom, l'espèce, le talent et le détail complet des six IV avec leur total sur 186.
- La recherche retrouve également un Pokémon à partir de son surnom.

## 0.59.0

- Un style de génération Monotype permet de choisir parmi les 18 types et construit six Pokémon partageant ce type, avec des doubles-types et des rôles aussi variés que possible.
- Les Pokémon verrouillés incompatibles avec le type choisi sont refusés avant la génération afin d'éviter une composition invalide.
- Le Team Doctor analyse désormais le set complet : attaques et catégories, talent, objet, nature et EV.
- Les poseurs et contrôleurs reconnus sont affichés explicitement en vert, par exemple `Pose de hazards : Carchacrok` pour Piège de Roc.
- Les conseils deviennent ciblés : il indique quel Pokémon peut recevoir quelle attaque pour corriger la pose ou le retrait des hazards.
- Floréclat avec Dépôt Toxique mais sans retrait reçoit par exemple la recommandation d'ajouter Toupie Éclat.
- Les incohérences Veste de Combat, Dé Pipé, objets Choix, Cognobidon et orientation EV/nature sont expliquées par Pokémon.
- Le style sélectionné est contrôlé : deux poseurs Trick Room, noyau météo, bénéficiaires, Voile Aurore, soin, pivot et condition de victoire.
- Les principales immunités de talents et d'objets sont prises en compte dans les faiblesses communes.
- Les compositions monotypes sont reconnues automatiquement : leur type commun n'est plus signalé comme une répétition incohérente.
- La fenêtre Team Doctor est agrandie et peut afficher neuf recommandations longues avec défilement lisible.

## 0.58.11

- Le Team Doctor reconnaît maintenant les identifiants d'attaques Cobblemon avec namespace, notamment Toupie Éclat sur Floréclat.
- La pose de hazards couvre aussi Dépôt Toxique, Hache de Pierre, Vagues à Lames et G-Max Steelsurge.
- Le retrait et le contrôle couvrent Anti-Brume, Tour Rapide, Toupie Éclat, Grand Nettoyage, Change-Côté, G-Max Wind Rage et Miroir Magik.
- Dépôt Toxique ne fait pas passer Floréclat pour un retrait : Toupie Éclat doit bien être présente dans son set.

## 0.58.10

- Le sélecteur charge maintenant les learnsets complets présents dans les données locales Cobblemon : niveau, reproduction, tuteur, évolution, changement de forme et CT.
- Les listes non synchronisées par Cobblemon 1.7, notamment les CT, sont restaurées depuis les fichiers d'espèces du modpack sans dépendance serveur supplémentaire.
- Le titre principal est masqué sous la fenêtre d'attaques et celle-ci possède une barre d'en-tête plus haute afin d'éviter tout chevauchement.
- L'équipement reste sécurisé : une attaque configurée mais pas encore connue est remplacée par une attaque disponible et signalée au joueur.

## 0.58.9

- Les six valeurs d'EV de l'éditeur sont maintenant des champs saisissables directement au clavier.
- Maintenir les boutons `−` ou `+` répète et accélère progressivement l'ajustement jusqu'aux limites autorisées.
- Les saisies et les boutons respectent automatiquement 252 EV par statistique et 510 EV au total, y compris lorsqu'il ne reste pas un multiple de quatre disponible.

## 0.58.8

- Suppression de l'ancienne limite fixe de 96 teams pour la création, la sauvegarde, la duplication et la migration interserveur.
- La pagination s'adapte maintenant librement au nombre de teams enregistrées sans tronquer les sauvegardes existantes.

## 0.58.7

- L'interaction d'objet démarre dès que Cobblemon déclare le Pokémon disponible, sans l'attente fixe de six ticks ni la double confirmation locale.
- Le rappel est demandé plus tôt et automatiquement renvoyé si le serveur ignore la première demande, afin d'accélérer la file sans laisser un Pokémon dehors.
- Fermer l'interface pendant l'équipement ne l'annule plus : la team continue de s'appliquer en arrière-plan et le résultat est annoncé dans le chat.
- Une application lancée via l'ouverture distante du PC referme automatiquement l'interface et libère proprement le lien PC après son achèvement.

## 0.58.6

- Les objets sont maintenant appliqués strictement un Pokémon après l'autre : le mod n'envoie plus toute l'équipe simultanément et la file ne reste plus bloquée après le premier objet.
- Chaque Pokémon automatisé est rappelé avant de passer au suivant, ce qui garantit que les six emplacements peuvent être traités.
- L'animation de Poké Ball liée au bref envoi automatique est masquée localement, comme le modèle du Pokémon et ses sons.

## 0.58.5

- Un slot déjà lié recharge désormais le talent actuel, la nature effective après Mint, les EV réels et les attaques actives du Pokémon lorsqu'on édite la team.
- Sélectionner directement un Pokémon possédé conserve ces mêmes données réelles dans le Team Builder.
- Lorsqu'un modèle `All` est associé, seules les attaques réellement actives ou en réserve sont retenues ; les attaques prévues manquantes restent signalées.
- Les différences avec la nature et les EV attendus sont annoncées avant que le slot adopte les valeurs réelles du Pokémon capturé.
- Suppression de l'ancien fallback de mouvements sauvegardés devenu incohérent avec les slots liés.

## 0.58.4

- Les deux boutons de pagination de la liste des teams passent de 22 à 18 pixels de large.
- La flèche droite est maintenant ancrée sur la limite intérieure du panneau et ne dépasse plus du cadre.
- Le compteur de pages est recentré selon la largeur réelle de la colonne latérale.

## 0.58.3

- Dans l'onglet `Owned`, les talents normaux sont désormais affichés en cyan et les talents cachés en or, comme dans `All`.
- La distinction est appliquée dans la colonne Talent et dans l'infobulle détaillée du Pokémon.
- Un Pokémon dont la forme ne possède qu'un seul talent reste correctement considéré comme ayant un talent normal.

## 0.58.2

- Une régénération complète conserve uniquement les slots verrouillés et exclut de la variante les espèces présentes dans les slots déverrouillés à remplacer.
- Les formes alternatives d'une espèce verrouillée ne peuvent plus réapparaître dans un autre slot.
- Le reroll individuel exclut également l'ancienne espèce et toutes les espèces déjà présentes dans les cinq autres slots.
- Un avertissement rouge discret rappelle que la génération constitue une base indicative et que ses informations et sets doivent être vérifiés.
- Les modèles 3D des Pokémon sont agrandis dans l'éditeur et dans l'affichage des teams sauvegardées, sans masquer les objets ni les états.

## 0.58.1

- `Associer tous` ne quitte plus l'éditeur et n'ouvre plus le catalogue lorsqu'il existe plusieurs exemplaires compatibles.
- Chaque slot choisit directement le meilleur Pokémon de la bonne espèce et de la bonne forme, avec le talent prévu obligatoire.
- Les exemplaires sont ensuite départagés par nature correspondante, total d'IV, attaques prévues déjà apprises puis niveau.
- Les slots sans correspondance compatible restent inchangés et sont comptés comme choix manuels.

## 0.58.0

- L'interface principale gagne 60 pixels en largeur et 30 pixels en hauteur tout en conservant les cadres Cobblemon en 9-slice, sans déformer les assets.
- Les six cartes Pokémon, leurs modèles 3D et les actions principales profitent du nouvel espace.
- La liste latérale affiche désormais jusqu'à sept équipes par page.
- Le sélecteur d'attaques est agrandi et affiche huit attaques légales par page.
- Le catalogue d'attaques propose toutes les attaques légales de la forme Cobblemon, même si le Pokémon possédé ne les a pas encore apprises.
- Les quatre emplacements du set restent configurables quel que soit le nombre d'attaques actuellement actives.
- À l'équipement, seules les attaques réellement apprises sont appliquées ; les autres sont remplacées par les attaques actives disponibles et signalées en rouge dans le chat.

## 0.57.0

- Les champs d'usage Tropimon ne sont plus assemblés aveuglément : objet, nature, EV, setup et catégories d'attaques passent par une validation de cohérence commune.
- Un set physique avec `Belly Drum`, EV Attaque et nature physique ne peut plus recevoir uniquement des attaques spéciales.
- `Loaded Dice` exige désormais au moins une attaque multi-coups légale ; sinon une attaque compatible est ajoutée ou l'objet est remplacé.
- `Assault Vest`, `Choice Band` et `Choice Specs` sont contrôlés par rapport aux attaques réellement retenues.
- Lorsqu'un assemblage Tropimon complet reste incohérent, un set complet Showdown Gen 9 cohérent est utilisé ; hors ligne, le set est réparé localement.
- Le remplissage automatique favorise maintenant les attaques de setup correspondant réellement à l'orientation physique ou spéciale du set.

## 0.56.8

- Les titres latéraux `MY TEAMS` et `NEW TEAM` descendent d'un pixel pour finaliser leur centrage.

## 0.56.7

- Le titre principal `Tropimon Team Builder` remonte de deux pixels pour finaliser son centrage dans le bandeau noir.

## 0.56.6

- `Link all` ouvre désormais toujours le catalogue `Owned`, limité aux Pokémon réellement capturés.
- L'onglet `All` est désactivé pendant une association afin de ne plus proposer de formes non possédées.
- Le choix `All/Owned` mémorisé reste inchangé pour la création et le remplacement classiques.

## 0.56.5

- `Régénérer` force maintenant le remplacement d'au moins un Pokémon non verrouillé dans l'unique génération de variante.
- Le slot forcé tourne entre les membres disponibles à chaque nouvelle variante.
- La régénération complète est désactivée lorsque les six slots sont verrouillés.
- L'interface reconstruit ainsi des cartes réellement différentes au lieu de réafficher le même roster.

## 0.56.4

- Le titre principal `Tropimon Team Builder` descend de cinq pixels supplémentaires dans son bandeau noir.

## 0.56.3

- Les titres latéraux `NEW TEAM` et `MY TEAMS` descendent de quatre pixels pour être centrés dans leur bandeau.

## 0.56.2

- Le titre `Tropimon Team Builder` descend de cinq pixels pour être centré verticalement dans le bandeau noir.

## 0.56.1

- Le générateur Ranked construit désormais exactement une seule team par action.
- La recherche d'une composition différente ne peut plus relancer silencieusement jusqu'à huit générations complètes.
- `Régénérer` reste disponible et lance une nouvelle team uniquement lorsque le joueur le demande.
- Suppression de l'ancien système de propositions multiples et de sa navigation devenue inutile.

## 0.56.0

- L'association d'un Pokémon capturé exige désormais le talent prévu par le set ; un exemplaire au mauvais talent est refusé clairement dans l'interface et le chat.
- Les différences de nature, d'EV et les attaques encore manquantes restent des avertissements non bloquants après l'association.
- À l'équipement, les attaques prévues mais non apprises sont remplacées temporairement par des attaques déjà connues, puis les attaques manquantes sont signalées au joueur.
- Le sélecteur d'attaques rassemble maintenant les attaques actives, celles en réserve et tout le catalogue légal de la forme Cobblemon.
- Le sélecteur d'objets affiche tout le catalogue PvP et classe les propositions selon les types, statistiques, talent et attaques du Pokémon.
- Le titre principal remonte dans le bandeau noir, `MY TEAMS` est recentré et le compteur de pages du catalogue est repoussé à droite.

## 0.55.1

- La génération initiale ne calcule plus trois teams complètes avant d'afficher la première : les variantes sont produites à la demande avec Régénérer.
- La recherche par branches utilise d'abord les données légères d'usage, puis télécharge les statistiques et sets détaillés uniquement pour les six membres retenus.
- La passe de réparation n'hydrate plus jusqu'à 144 candidats : elle évalue les remplacements localement et ne télécharge au maximum que deux finalistes.
- Une génération fraîche passe ainsi d'environ 100 fiches détaillées à 6–8 requêtes au maximum, souvent servies directement par le cache disque.
- Les fiches Ranked déjà présentes sur disque sont réutilisées immédiatement jusqu'à quatorze jours au lieu d'être retéléchargées après dix minutes.
- Une limite stricte de quinze secondes annule proprement une génération si un service distant ne répond plus.

## 0.55.0

- Les vues `Mes teams` et création/édition passent de 349 × 205 à 405 × 225 pixels tout en réutilisant les cadres et textures du PC Cobblemon.
- La zone centrale bleue gagne 56 pixels en largeur et 20 pixels en hauteur sans étirer les bordures : le cadre et l'overlay utilisent désormais un découpage en neuf zones.
- Les six cartes Pokémon passent de 50 × 46 à 68 × 56 pixels, avec des modèles 3D plus grands et davantage d'espace autour des objets et états.
- Le champ de nom, le résumé d'état, les boutons Équiper/Modifier/Dupliquer et la navigation entre propositions exploitent toute la nouvelle largeur.
- La liste latérale affiche six teams par page et les commandes inférieures profitent de la hauteur supplémentaire.
- Les fenêtres Objet, Attaques, Set et Team Doctor sont recentrées dans le nouveau cadre.
- Le catalogue Pokémon conserve ses proportions et ses colonnes indépendamment de l'agrandissement des vues principales.

## 0.54.1

- Corrige le crash interne du générateur Ranked lorsqu'il construit une équipe sans aucun Pokémon de départ.
- Le premier membre est maintenant évalué sans « Pokémon précédent », puis les coéquipiers suivants reprennent normalement la logique de synergie.

## 0.54.0

- Les calculs Ranked annulés sont maintenant réellement interrompus et ne peuvent plus saturer la file de génération après plusieurs changements rapides de saison, de style ou de proposition.
- Les requêtes HTTP Ranked utilisent une file distincte afin d'éviter un interblocage lorsque plusieurs générations attendent les données réseau.
- Changer de style ou de mode de génération invalide les anciennes propositions, et les boutons concernés sont verrouillés pendant un calcul en cours.
- L'application des objets ne reste plus bloquée lorsqu'un Pokémon présorti a déjà été rappelé avant la confirmation attendue.
- L'annulation ne renvoie plus accidentellement un Pokémon déjà dans sa Poké Ball.
- L'association d'un Pokémon plafonne toujours son preset à quatre attaques, même si une ancienne sauvegarde contient une valeur invalide.
- Suppression des anciens helpers de génération, de l'état d'application et de l'accesseur de sauvegarde devenus inutilisés.
- La compilation active tous les avertissements Java et ne laisse plus aucun avertissement provenant du code du mod.

## 0.53.2

- Le titre `Tropimon Team Builder` remonte de quatre pixels uniquement dans la page de création `New Team` afin de rester centré dans son bandeau noir.

## 0.53.1

- Le titre du catalogue Pokémon descend dans le bandeau noir et utilise une taille plus discrète.
- Le compteur de pages est aligné verticalement avec le titre et repoussé vers le bord droit du bandeau.
- Le résumé Ranked de la saison et l'indication de navigation sont réduits et tiennent désormais entièrement dans le cadre noir inférieur.

## 0.53.0

- Le générateur explore désormais plusieurs compositions complètes en parallèle au lieu de choisir chaque Pokémon isolément.
- Chaque candidat est évalué avec un vrai set indivisible : données Tropimon complètes en priorité, puis preset Pokémon Showdown Gen 9.
- Les principales menaces de la saison influencent la couverture offensive et les faiblesses défensives communes de l'équipe entière.
- Les rôles indispensables sont contrôlés selon le style : hazards, retrait, vitesse, breakers, soin, poseurs météo, bénéficiaires, Aurora Veil et deux poseurs Trick Room.
- Les synergies concrètes sont récompensées, notamment Future Sight + breaker physique, VoltTurn, hazards + Knock Off, Toxic Spikes + Hex et Wish + partenaire bulky.
- Une passe de réparation remplace automatiquement le membre le moins utile lorsqu'elle améliore réellement la cohérence de la composition.
- Les trois propositions utilisent maintenant des profils distincts Méta, Mixte et Original tout en respectant le style choisi.
- Les détails Ranked complets ne sont téléchargés que pour les branches conservées afin de limiter le temps de génération et les requêtes inutiles.

## 0.52.0

- Les équipes Trick Room générées exigent désormais au moins deux poseurs légaux au lieu d'un seul.
- Le générateur garantit que deux sets non verrouillés contiennent réellement `Trick Room` lorsque le style est sélectionné.
- Les équipes Pluie, Soleil, Sable, Neige et Aurora Veil exigent au moins deux Pokémon qui bénéficient réellement de la météo, en plus du poseur.
- Les bénéficiaires sont identifiés plus strictement par leur type, talent ou attaques compatibles : Swift Swim, Chlorophyll, Sand Rush, Slush Rush, Thunder, Solar Beam, etc.

## 0.51.2

- Le compteur de pages du catalogue Pokémon quitte le pied de liste et s'affiche maintenant en haut à droite du cadre.
- Le pied de liste conserve uniquement l'indication de navigation à la molette.
- Le résumé Ranked au survol utilise un cyan clair avec ombre, lisible sur le bandeau sombre.

## 0.51.1

- Le sélecteur de Pokémon mémorise le dernier onglet choisi : `Tous` rouvre sur `Tous` et `Possédés` rouvre sur `Possédés`.
- Ce choix reste conservé après la sélection d'un Pokémon et lors de la réouverture de l'éditeur pendant la session de jeu.

## 0.51.0

- Le projet et le mod sont renommés publiquement **Tropimon Team Builder** ; les nouveaux JAR utilisent le nom `TropimonTeamBuilder`.
- Le titre principal de l'interface devient `Tropimon Team Builder` en français comme en anglais et descend de trois pixels dans son bandeau.
- L'identifiant interne historique `tropimon_team_saver` et les dossiers de sauvegarde restent inchangés afin de préserver toutes les équipes existantes.

## 0.50.3

- Les résumés et messages d'état sont placés sur un bandeau sombre utilisant le style du bouton `Release` de Cobblemon.
- Le texte devient blanc contrasté avec une ligne d'état verte, jaune ou rouge, et conserve son défilement lorsqu'il est trop long.

## 0.50.2

- La grille des six Pokémon est recentrée précisément dans les 174 pixels de l'écran bleu du PC.
- Le déplacement commun inclut les cartes, modèles 3D, objets, indicateurs et zones de clic de l'éditeur comme de l'aperçu.

## 0.50.1

- Refonte de la colonne gauche de l'éditeur avec une timeline plus compacte et une séparation nette entre progression et actions.
- Les étapes utilisent les emplacements de Party et le séparateur du résumé Cobblemon afin de rester cohérentes avec l'interface native.
- Le style affiche uniquement sa valeur (`Trick Room`, `Pluie`, etc.) : les libellés longs ne sont plus inutilement tronqués.
- Les boutons d'association, de style et de Team Doctor tiennent entièrement dans le panneau, avec des hauteurs et espacements homogènes.

## 0.50.0

- Trois modes de génération : Méta, Mixte et Original, avec respectivement priorité aux usages, trois piliers méta ou deux piliers méta.
- Trois propositions de composition peuvent être parcourues avant la sauvegarde, sans note artificielle.
- Le générateur couvre désormais de vrais rôles compétitifs : hazards, retrait, pivot, contrôle de vitesse, condition de victoire et récupération.
- Les faiblesses et résistances Gen 9 de toute l'équipe influencent chaque nouveau choix afin d'éviter les faiblesses communes non couvertes.
- Les Pokémon sont évalués avec un set compétitif cohérent avec le style ; la priorité d'application reste Tropimon, puis Pokémon Showdown Gen 9, puis le secours local légal.
- Les styles offensif, bulky offense, stall, Trick Room, météo et Aurora Veil conservent leurs contraintes tout en bénéficiant de l'analyse globale.

## 0.49.1

- La priorité des sets devient stricte : set Tropimon complet, sinon preset Pokémon Showdown Gen 9, puis génération locale légale en dernier recours.
- Un preset Showdown correspondant mieux au style ne remplace plus un set Tropimon complet.

## 0.49.0

- Nouveau style `Aurora Veil`, distinct de `Neige` : poseur de neige, utilisateur légal d'Aurora Veil puis partenaires compatibles.
- Nouveau bouton `Original` : la génération conserve au moins deux piliers parmi les Pokémon les plus méta de la saison et complète avec des choix moins joués mais cohérents.
- Intégration des sets officiels Pokémon Showdown Gen 9 (OU, UU, RU, NU, PU, ZU, Ubers et National Dex), conservés dans un cache disque actualisé quotidiennement.
- Un set Showdown est choisi comme bloc cohérent lorsque son rôle correspond fortement au style ou que les données Tropimon sont incomplètes.
- Chaque talent, objet et attaque importé est contrôlé contre la forme Cobblemon ; les éléments incompatibles sont remplacés par les données Ranked ou un set compétitif légal de secours.
- Le dernier recours construit désormais objet, nature, EV et quatre attaques cohérentes avec les statistiques, le STAB et le style du Pokémon.

## 0.48.0

- Les styles deviennent des contraintes de composition avant l'application des usages et affinités Ranked, au lieu de simples bonus légers.
- Offensive exige un noyau d'au moins quatre attaquants ; Bulky offense impose deux profils résistants et trois attaquants ; Stall recherche trois soigneurs résistants et au moins quatre profils bulky.
- Trick Room sélectionne d'abord un poseur compatible puis au moins quatre Pokémon lents.
- Pluie, Soleil, Sable et Neige recherchent d'abord un poseur de météo puis au moins quatre membres compatibles par leur type ou leur talent.
- Les talents et attaques cohérents avec le style sont prioritaires dans les sets Ranked lorsque l'API les a observés.
- Les usages, coéquipiers, diversité des types et variations continuent de départager les candidats après le respect des contraintes du style.

## 0.47.1

- Recentrage du bandeau, des étapes et de tous les boutons du panneau gauche sur la largeur intérieure réelle du PC.
- Les actions `Associer tous`, `Style` et `Team Doctor` disposent maintenant d'espacements réguliers sans toucher l'étape de sauvegarde ni le bas du cadre.
- La liste des équipes utilise le même alignement que l'éditeur pour une transition visuelle homogène.
- Un clic sur un autre Pokémon change maintenant simplement la sélection ; seuls les vrais glisser-déposer réordonnent l'équipe.
- Le bouton `Modifier le set` suit correctement le Pokémon sélectionné pendant la création et l'édition.

## 0.47.0

- La capacité passe de 24 à 96 équipes sauvegardées sans modifier le format de sauvegarde existant.
- La liste indique la page active et accepte désormais la molette pour parcourir rapidement les équipes.
- Pendant l'équipement, la sélection d'équipe, les cartes Pokémon, les objets, l'édition, la duplication, la suppression, l'annulation historique et la sortie vers les boîtes sont verrouillés.
- Le bouton `Annuler` reste disponible et la touche Échap annule proprement l'application en cours au lieu de fermer l'écran derrière la file d'actions.

## 0.46.1

- Les teams historiques sans `formId` utilisent désormais la forme réelle du Pokémon au lieu d'être rejetées comme forme incompatible.
- Une attaque déjà active ou en réserve est toujours acceptée, même si le learnset du serveur diffère du catalogue Cobblemon local.
- La prévalidation stricte reste active pour les attaques non apprises réellement incompatibles et pour les nouvelles formes explicitement sélectionnées.

## 0.46.0

- Le catalogue Cobblemon (espèces, formes, types, talents, statistiques, objets et attaques légales) est construit une seule fois puis réutilisé.
- Les Pokémon de l'équipe et des boîtes sont indexés par UUID et espèce ; les recherches, aperçus et associations ne reparcourent plus tout le PC.
- Le navigateur ne charge que les modèles 3D visibles et réutilise les mêmes widgets pendant le défilement, la recherche et le tri.
- Le cache Ranked est conservé sur disque pendant 14 jours : les dernières données s'affichent immédiatement puis sont actualisées en arrière-plan.
- Les requêtes Ranked obsolètes sont annulées et leurs réponses ignorées lorsqu'une saison ou une régénération plus récente les remplace.
- Nouvelle prévalidation avant sauvegarde et équipement : associations, disponibilité des objets, attaques apprises et légales, talents, formes, doublons et limite d'EV.
- L'application publie sa progression par phase, propose `Annuler` et `Réessayer`, et retente automatiquement deux fois un déplacement Cobblemon non confirmé.
- Nouveau bouton `Associer tous` : les correspondances uniques sont liées automatiquement ; les exemplaires ambigus restent inchangés et ouvrent le comparateur.
- Suppression des méthodes, modèles et traductions devenus inutilisés, sans modification du format de sauvegarde.

## 0.45.1

- L'association d'un modèle accepte désormais un exemplaire de la même espèce comme solution de repli lorsque les identifiants de forme Cobblemon/Showdown ne coïncident pas exactement.
- Lors du rattachement, seules les attaques prévues réellement actives ou en réserve sont conservées ; les emplacements restants sont complétés avec les attaques actuelles du Pokémon.
- Les attaques prévues mais non apprises sont listées dans un avertissement rouge dans l'interface et dans un message exclusivement local du chat.
- Le Team Doctor affiche les icônes officielles des types issues de `cobblemon:textures/gui/types.png` pour les répétitions et faiblesses communes.

## 0.45.0

- Ajout de cadenas par slot : les Pokémon verrouillés conservent leur position lors d'une régénération complète.
- Chaque slot déverrouillé peut être régénéré individuellement selon la saison, le style et les cinq autres membres.
- Neuf styles de génération pondèrent réellement les candidats : équilibré, offense, bulky offense, stall, Trick Room et quatre climats.
- Nouveau `Team Doctor` : composition incomplète, types trop répétés, faiblesses défensives communes, hazards, retrait, vitesse et équilibre physique/spécial.
- Les infobulles expliquent désormais les choix Ranked avec l'usage, le meilleur coéquipier statistique et le style actif.
- Nouvel éditeur de set unifié pour l'objet, le talent, la nature, les EV et les quatre attaques ; les talents cachés restent signalés en or.
- En création, le sélecteur d'objets inclut aussi le catalogue compétitif, même si l'objet n'est pas encore dans l'inventaire.
- Les modèles créés dans `Tous` détectent les Pokémon compatibles capturés plus tard. `Associer` conserve le set prévu, privilégie talent et attaques compatibles et signale les attaques encore manquantes.

## 0.44.1

- Une équipe complète produite par l'assistant affiche désormais un bouton `↻ Régénérer` à la place de `Sets Ranked`.
- La régénération conserve les Pokémon du noyau choisi avant la première génération et recalcule tous les autres slots avec les données de la saison active.
- L'équipe affichée reste intacte si l'API Ranked échoue ; elle n'est remplacée qu'après réception d'une nouvelle proposition valide.
- Les contrôles anti-répétition tiennent compte de la taille du noyau pendant une régénération afin d'éviter aussi les variantes trop proches.

## 0.44.0

- La variation Ranked ne repart plus à zéro à chaque réouverture de l'écran.
- À chaque étape, le générateur alterne parmi les trois meilleurs coéquipiers compatibles ; le premier Pokémon d'une team vide varie parmi les cinq meilleurs usages.
- Les compositions déjà sauvegardées ou déjà générées pendant la session sont détectées par leur roster de six Pokémon.
- Une proposition identique est régénérée automatiquement jusqu'à huit fois ; avec au moins deux slots à remplir, les variantes partageant cinq Pokémon sur six sont aussi refusées.
- Les usages, coéquipiers et sets restent entièrement dépendants de la saison sélectionnée.

## 0.43.2

- La vue Pokémon élargie retrouve un véritable cadre Cobblemon issu du panneau central de `pc_base.png`.
- Le cadre utilise un découpage neuf-zones : coins, encoches et épaisseur restent pixel-perfect, seules les zones planes sont étendues.
- Suppression du cadre provisoire composé de rectangles unis, tout en conservant les dimensions adaptatives de la 0.43.0.

## 0.43.1

- Les usages Ranked sont désormais chargés et affichés dans le mode `Owned` en conservant la provenance Box/Équipe et le niveau.
- Le sélecteur de saison est disponible dans `Owned` comme dans `All`.
- Le résumé permanent et l'infobulle affichent aussi rang, usage, win rate et volume pour les Pokémon possédés.
- La ligne compacte défile si nécessaire afin de montrer Box, niveau et usage sans tronquer définitivement une information.

## 0.43.0

- Le navigateur Pokémon/Ranked utilise désormais presque toute la fenêtre disponible sans réduire la taille du texte.
- Affichage adaptatif de 6 à 8 Pokémon par page selon la résolution, contre 5 auparavant.
- Colonnes Nom et Talent élargies, champ de recherche agrandi et coordonnées de clic/tri recalées sur la nouvelle géométrie.
- Ajout d'un résumé Ranked permanent au survol avec saison, rang, usage, win rate et nombre d'utilisations.
- Les modales de talent, la liste des saisons, la pagination et le scroll restent correctement confinés à la vue élargie.

## 0.42.0

- Nouveau générateur Ranked en chaîne : chaque ajout privilégie les coéquipiers statistiques du Pokémon ajouté juste avant, puis la synergie avec toute l'équipe.
- Un ou deux Pokémon déjà placés servent automatiquement de noyau autour duquel l'assistant construit la composition.
- Règle de diversité : pas de troisième représentant d'un même type tant qu'une alternative compatible existe, avec pénalités supplémentaires sur les profils trop répétés.
- Les générations sans noyau peuvent varier parmi plusieurs points de départ à fort usage au lieu de reproduire systématiquement la même équipe.
- Le générateur cherche désormais le premier objet et talent Ranked réellement compatibles et parcourt tous les moves utilisés afin de remplir quatre attaques légales.
- Correction renforcée des formes standard afin que les Pokémon au-dessus de 10 % d'usage soient bien associés, affichés et classés.

## 0.41.2

- La modale de sélection du talent bloque désormais entièrement les tooltips et le défilement du catalogue situé derrière.
- Le voile de fond de la modale est plus opaque afin de garder uniquement le choix en cours lisible.

## 0.41.1

- Ajout d'une liste déroulante permettant de choisir la saison Ranked dans le catalogue `Tous`.
- Le changement de saison recharge les usages et reclasse immédiatement les Pokémon ; l'assistant utilise ensuite la même saison.
- La ligne compacte affiche désormais l'usage et le win rate, tandis que le survol détaille la saison, le rang et le nombre d'utilisations.
- Correction de l'association Ranked des formes standard qui pouvait masquer leurs statistiques et fausser leur classement.

## 0.41.0

- Ajout d'un assistant connecté à l'API publique de Ranked Tropimon pour compléter une composition avec les Pokémon réellement joués ensemble.
- Les sets proposés reprennent le talent, l'objet, les quatre attaques, la nature et la répartition EV les plus utilisés pendant la saison active.
- Le catalogue `Tous` est désormais classé par taux d'usage Ranked, puis par la logique d'évolution existante ; les tris manuels par statistique gardent la priorité.
- Le taux d'usage et la saison sont affichés dans le catalogue, avec un cache de dix minutes et un retour propre au classement Cobblemon si l'API est indisponible.
- Les recommandations restent des presets client-side : nature, EV et talent sont informatifs, tandis que les objets et attaques suivent les règles d'application existantes.

## 0.40.1

- Un objet de preset absent de l'inventaire ne bloque plus l'équipement de la team ni l'application des attaques.
- Les objets disponibles continuent d'être équipés et le statut final indique le nombre de Pokémon dont l'objet n'a pas été trouvé.

## 0.40.0

- Le catalogue `Tous` liste désormais séparément les formes standard, régionales, Méga, Gigamax et autres formes Cobblemon implémentées.
- Chaque forme utilise ses propres sprite, aspects, types, statistiques, talents et attaques légales.
- La forme choisie est enregistrée dans le preset avec migration transparente des anciennes sauvegardes.
- Sans tri de statistique, `Tous` classe d'abord les évolutions finales et les Pokémon sans évolution, puis les stades intermédiaires, puis les premiers stades.
- Avec un tri de statistique actif, le groupe évolutif sert de second critère.

## 0.39.0

- Les Pokémon du catalogue peuvent désormais recevoir un preset de quatre attaques avant d'être possédés.
- Le sélecteur catalogue affiche toutes les attaques légales du learnset Cobblemon.
- Après remplacement, chaque attaque prévue affiche une coche verte si elle est apprise ou une croix rouge si elle manque.
- Les attaques déjà apprises, actives ou en réserve, continuent d'être appliquées automatiquement lors de l'équipement.
- Ajout d'un bouton `Talent` pour modifier le talent prévu d'un Pokémon catalogue sans le recréer.

## 0.38.2

- Restauration de la couleur dorée individuelle des talents cachés dans le catalogue `Tous`.
- Une espèce ne possédant qu'un seul talent distinct l'affiche désormais comme talent normal.
- La même règle est appliquée aux Pokémon possédés, au sélecteur et aux infobulles.
- Tous les textes défilants utilisent désormais la même animation synchronisée.

## 0.38.1

- Le mode `Tous` affiche désormais toutes les espèces du catalogue Cobblemon, indépendamment des Pokémon possédés.
- La recherche du catalogue indexe tous les talents normaux et cachés de chaque espèce.
- Cliquer une espèce du catalogue ouvre un sélecteur de talents façon team builder Showdown.
- Le talent choisi est sauvegardé dans le preset et affiché dans les informations du Pokémon modèle.
- Le mode `Possédés` conserve uniquement les véritables Pokémon du joueur et leur talent actuel.

## 0.38.0

- Renommage public du mod en **Tropimon Team Manager**.
- Intégration complète à l'interface du PC Cobblemon.
- Ajout du navigateur `Possédés` / `Tous`, de la recherche persistante et du tri des statistiques.
- Ajout des presets d'attaques déjà apprises et du sélecteur d'objets compétitifs.
- Sauvegardes interserveurs par UUID avec positions PC isolées par serveur.
- Amélioration de l'application automatique des équipes, des objets et du rappel des Pokémon.
- Interface bilingue français/anglais et nombreuses corrections visuelles.
