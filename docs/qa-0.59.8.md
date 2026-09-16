# Validation 0.59.8

Attribution : By FastedCorsi.

- Correction ciblée du glissement : la molette démarre le geste dès le clic maintenu, sans seuil de déplacement préalable.
- L’équipe saisie et sa position dans les données restent inchangées pendant la navigation. Seul un dépôt valide modifie et sauvegarde l’ordre.
- Trois tests supplémentaires couvrent les deux sens de la molette, le curseur immobile, les événements sans clic ou sans mouvement vertical et le dépôt sur une autre page sans mutation préalable des données.
- Build avec dépendances officielles uniquement, 79 tests JUnit et contrôles de confidentialité des sources/JAR (14 auto-tests sur données fictives).
- Aucun changement aux automatismes, transferts, délais ou paquets d’équipement. Test interactif en jeu restant à effectuer.
