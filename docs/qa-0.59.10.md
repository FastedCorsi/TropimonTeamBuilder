# QA 0.59.10

## Comportements couverts

- Un clic simple sur une carte de l'aperçu continue d'afficher les informations du Pokémon.
- Un double-clic ouvre l'éditeur directement sur le sélecteur de remplacement du slot visé.
- Un glissement au-delà du seuil d'un slot occupé vers un autre échange leurs positions dans l'aperçu comme dans l'éditeur.
- Le déplacement direct est sauvegardé seulement au dépôt et propose l'annulation existante ; une erreur de sauvegarde restaure l'ordre précédent.
- Un exemplaire de même espèce et de même forme reprend le preset sauvegardé. Un changement d'espèce crée un nouveau set depuis le Pokémon choisi.
- Le Pokémon déjà utilisé dans le slot remplacé n'est plus proposé comme son propre remplacement.

## Vérifications

- Test de permutation et de sérialisation des sets complets dans `TeamModelsTest`.
- Tests existants de conservation des presets, association, validation et équipement.
- Build avec les dépendances officielles uniquement et contrôles de confidentialité sources/JAR.

## Limite

Le rendu interactif doit encore être confirmé en jeu sur les différentes résolutions d'interface. Aucun délai, paquet ou contrôle de transfert n'a été modifié.
