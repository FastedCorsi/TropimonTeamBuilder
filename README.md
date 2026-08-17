# Tropimon Team Manager

Gestionnaire d'équipes compétitives **100 % client-side** pour Minecraft 1.21.1 et Cobblemon 1.7.2.

Tropimon Team Manager ajoute un véritable onglet `Teams` à l'interface du PC Cobblemon. Il permet de créer, sauvegarder et appliquer plusieurs compositions sans base de données ni mod serveur.

![Vue d'une équipe sauvegardée](docs/screenshots/team-overview.png)

## Points forts

- Jusqu'à 24 équipes de 1 à 6 Pokémon, partagées entre les serveurs pour un même joueur.
- Sélection parmi les Pokémon possédés ou dans le Pokédex complet de Cobblemon.
- Recherche persistante, priorité aux niveaux 100 et tri ascendant/descendant/désactivé par statistique.
- Préconfiguration des objets, de l'ordre et des attaques déjà apprises par chaque Pokémon.
- État visuel immédiat : Pokémon et objet prêts, objet différent ou Pokémon à remplacer.
- Application de l'équipe avec retour des Pokémon remplacés dans leur ancien emplacement du PC.
- Création, modification, duplication, suppression et annulation immédiate.
- Interface française ou anglaise selon la langue de Minecraft.
- Raccourci configurable `K` pour consulter et gérer les équipes hors du PC.

## Sélecteur de Pokémon

Le navigateur de type Pokémon Showdown propose deux modes :

- `Possédés` : uniquement les Pokémon présents dans l'équipe ou les boîtes.
- `Tous` : toutes les espèces chargées par Cobblemon. Un Pokémon non possédé sert de modèle et doit être remplacé par un exemplaire réel avant d'équiper l'équipe.

![Navigateur de Pokémon](docs/screenshots/pokemon-browser.png)

## Attaques et objets

Chaque preset peut mémoriser jusqu'à quatre attaques déjà apprises. Les attaques du preset sont appliquées sans modifier leurs PP actuels.

![Gestion des attaques](docs/screenshots/move-manager.png)

Le sélecteur d'objets affiche les objets compétitifs réellement présents dans les 36 emplacements principaux de l'inventaire. Pour équiper automatiquement un objet provenant d'une shulker, il faut d'abord le placer dans l'inventaire du joueur.

![Sélecteur d'objets](docs/screenshots/item-picker.png)

## Installation

Prérequis :

- Minecraft `1.21.1`
- Fabric Loader `0.16.0` ou plus récent
- Fabric API
- Cobblemon `1.7.2`
- Fabric Language Kotlin

Téléchargez `TropimonTeamManager-<version>.jar` depuis les [releases GitHub](https://github.com/FastedCorsi/TropimonTeamManager/releases), puis placez-le uniquement dans le dossier `mods` du client.

## Utilisation

1. Ouvrez un PC Cobblemon et cliquez sur `Teams`, ou utilisez la touche `K`.
2. Créez une équipe, donnez-lui un nom et remplissez ses six emplacements.
3. Configurez si nécessaire les objets et les attaques de chaque Pokémon.
4. Enregistrez, sélectionnez la composition, puis cliquez sur `Équiper`.

L'accès distant dépend des fonctions autorisées par le serveur. Si une composition nécessite de déplacer des Pokémon entre l'équipe et les boîtes, le serveur doit fournir un accès PC distant compatible ; sinon, ouvrez un PC placé dans le monde.

## Sauvegardes

Aucune base de données externe n'est utilisée. Les équipes sont enregistrées localement par UUID de joueur et sont donc disponibles sur tous ses serveurs :

```text
<launcher>/config/tropimon-team-saver/players/<uuid-joueur>.json
```

Les positions de retour dans les boîtes restent isolées par serveur :

```text
<launcher>/config/tropimon-team-saver/return-slots/<identifiant-serveur>/<uuid-joueur>.json
```

Le nom historique `tropimon-team-saver` est conservé pour assurer la compatibilité avec les sauvegardes des versions précédentes.

## Développement

Le wrapper Gradle utilise Java 21 :

```powershell
.\gradlew.bat clean test build
```

Le build utilise automatiquement les dépendances du launcher Tropimon lorsqu'elles sont présentes, puis les dépôts Fabric et Modrinth en solution de repli. Le JAR est généré dans `build/libs/TropimonTeamManager-<version>.jar`.

## Licence

Tous droits réservés. Consultez [LICENSE](LICENSE).
