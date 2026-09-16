# Validation 0.59.6

Attribution : By FastedCorsi.

## Périmètre

- Bouton Réorganiser / Reorder dans Mes teams, avec Monter / Move up, Descendre / Move down et Terminé / Done.
- Les déplacements utilisent l’ordre de la liste JSON existante, sans nouveau fichier de configuration ni migration.
- La sélection suit la même équipe et sa nouvelle page. Les commandes de déplacement sont désactivées aux extrémités et pendant une application de team.
- Les six lignes de la liste restent au-dessus de la pagination ; les libellés longs conservent le défilement existant.
- Aucun changement des transferts, délais, paquets, sets, générations ou recommandations.

## Vérifications

- Build avec les dépendances officielles uniquement : `./gradlew build -PofficialDependenciesOnly`.
- 70 tests JUnit, dont trois nouveaux tests couvrant le déplacement, les limites, les noms identiques, les ensembles de 120 équipes, la préservation des sets et positions PC, le retour à l’ordre précédent et la sérialisation/relecture JSON.
- Contrôle de confidentialité des sources et JAR, incluant 14 auto-tests sur données fictives.
- Disposition contrôlée dans les coordonnées de l’interface ; pas de validation interactive en jeu effectuée pour ce changement.

## Test en jeu à compléter

Sélectionner une équipe, cliquer sur Réorganiser, utiliser Monter/Descendre jusqu’à changer de page, terminer avec Terminé ou Échap, puis rouvrir le mod. Vérifier l’ordre, la sélection, la navigation à la molette et l’absence de modification des Pokémon/sets.
