# Validation 0.59.7

Attribution : By FastedCorsi.

## Changement

- Import paste et Réorganiser dans le panneau droit, sans déplacer les commandes Équipe actuelle / Supprimer ni supprimer Annuler.
- En mode Réorganiser, glisser un nom dans la liste : aperçu sous le curseur, ligne d’insertion, huit équipes par page, changement de page par molette ou maintien au bord.
- La liste n’est modifiée et sauvegardée qu’au dépôt valide. Échap, clic droit, dépôt hors liste, perte de focus, redimensionnement et fermeture abandonnent le glissement.
- Le déplacement conserve l’identité, les sets et les positions PC de l’équipe. Aucun changement des transferts, paquets ou temporisations d’équipement.

## Vérifications

- `./gradlew build -PofficialDependenciesOnly` : compilation et tests avec les dépendances officielles.
- 76 tests JUnit : six nouveaux tests sur le seuil de glissement, l’annulation, la réinitialisation du geste, les limites d’insertion, les pages partielles, les destinations dans les deux directions, les sources invalides et la sauvegarde de l’ordre.
- Les tests existants de réorganisation vérifient également la préservation des sets et positions PC, les noms identiques et les listes de 120 équipes.
- Contrôles de confidentialité sur les sources et les JAR finaux, avec 14 auto-tests sur données fictives.
- Contrôle des coordonnées : huit lignes terminées avant la pagination ; pile des boutons droits sous les informations Pokémon. L’avertissement d’un Pokémon non possédé défile sur une ligne au-dessus des boutons.

## Limite

Pas de validation interactive en jeu effectuée pour cette version. À vérifier en jeu : les deux sens du glissement, le changement de page pendant le maintien, les annulations, puis la réouverture du mod pour constater l’ordre sauvegardé.
