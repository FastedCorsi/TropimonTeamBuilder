# Vérification 0.59.4 — 30 août 2026

## Résultats établis

- `gradlew build -PofficialDependenciesOnly` : succès, 67 tests réussis.
- Tests de réutilisation et d'invalidation des caches : sets, ordre des membres, stockage, données Pokémon, inventaire, catalogue, langue et noms affichés ; équivalence du résultat Team Doctor pour chaque style.
- Test HTTP réel avec `scripts/PasteSmoke.java` : le lien Poképaste fourni est téléchargé et décodé en six sets complets, dont Samurott-Hisui. Ce test ne valide pas à lui seul toute l'interface d'import.
- `scripts/verify-learnsets.ps1` : 1 148 clés espèce/forme comparées, aucun écart entre l'ancienne union des ressources du profil et Cobblemon officiel plus nos compléments autonomes.
- Démarrage de production en profil isolé : initialisation du Team Builder et chargement des ressources réussis avec Cobblemon 1.7.2, Fabric Loader 0.17.2, Fabric API 0.116.6+1.21.1 et Fabric Language Kotlin 1.13.7+kotlin.2.2.21.
- Démarrage avec Catch Preview 0.6.0, Chat Filter 0.1.10 et Damage Calc 0.3.34 : initialisation et chargement des ressources réussis. Cela reste un test de démarrage, pas une recette complète de tous leurs écrans.

## Limites et arrêt des essais interactifs

- Le profil contenant tous les autres mods Tropimon bloque avant le jeu dans `tropimodclient.mixins.json:ClientPlayerEntityMixin` : classe `fr.tropimon.tropimoncore.data.town.Town` absente. Aucun correctif ni contournement n'a été ajouté au Team Builder pour dépendre de cette classe. La coexistence complète n'est donc pas certifiée.
- La capture du client de test a rencontré une erreur Windows, puis l'utilisateur a arrêté Computer Use avec Échap. La recette visuelle de l'import, des rafraîchissements en jeu et des transferts n'a pas été terminée. Aucun test sur le serveur du joueur n'est revendiqué.
- Aucun gain FPS n'a été mesuré. Les tests démontrent la réutilisation des calculs et l'identité de leurs résultats, pas un taux d'images par seconde.

## Périmètre et garanties du changement

- `ClientTeamApplier`, `ClientItemApplier`, `ClientMoveApplier`, `AutomationVisibility` et les quatre mixins de protection existants n'ont pas été modifiés. Les nouveaux mixins observent seulement la fin des mises à jour officielles pour incrémenter des révisions locales.
- Aucun changement des algorithmes de génération, des recommandations, des règles de validation ni des délais/ordres de paquets. Les validations préalables aux sauvegardes et équipements sont toujours exécutées en direct.
- Les deux surcharges privées inutilisées de `addPokemonModel` et l'ancienne méthode d'invalidation sans appelant ont été retirées après recherche des références. Les points d'entrée Fabric/mixin et les protections ont été conservés.
- Les ressources de learnsets propres remplacent la lecture des fichiers des autres mods. Elles reproduisent le profil audité ; une future modification privée d'un autre mod ne sera plus lue automatiquement. Les données publiques synchronisées par Cobblemon restent prises en compte.
- L'import prépare un brouillon et ne donne ni objet ni attaque au joueur. Les IV, le Téra, le niveau et les champs cosmétiques non gérés sont signalés comme ignorés.
- Aucun JAR installé remplacé, aucune sauvegarde du launcher modifiée, aucun commit ou push. Les fichiers/profils d'essai sont sous `build/qa`.
