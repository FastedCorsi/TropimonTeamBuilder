# Validation 0.59.9

Attribution : By FastedCorsi.

## Régression corrigée

- Modifier une team associée relisait auparavant talent, attaques actives, nature effective et EV depuis le Pokémon vivant. En enregistrant ensuite l’édition, ces valeurs remplaçaient silencieusement le preset.
- L’association d’un Pokémon remplaçait également les attaques prévues par le set accessible et les EV/nature prévus par les valeurs actuelles.

## Nouveau comportement

- Un champ explicitement sauvegardé est toujours prioritaire et recopié défensivement dans l’éditeur.
- Lors de l’association, seuls les champs non définis héritent de l’état actuel du Pokémon.
- Les incompatibilités de nature, EV et attaques restent signalées. Lors de l’équipement, le réconciliateur n’applique que les attaques accessibles et complète temporairement avec le set actuel, sans modifier le preset.
- Aucun changement des transferts de Pokémon/objets, de l’ordre des paquets ou des temporisations.

## Vérifications

- Tests dédiés : preset explicite contre set vivant différent, repli champ par champ, copie défensive des attaques/EV et comportement de réconciliation des attaques manquantes.
- Build avec dépendances officielles, suite JUnit complète et contrôles de confidentialité des sources/JAR.
- Test interactif en jeu restant : recréer une team avec Coup Bas contre un Clamiral actuellement doté d’Eau Revoir, l’enregistrer, ouvrir/fermer Modifier, équiper une autre team puis revenir ; vérifier que le preset reste Coup Bas et que seule l’application utilise un repli si Coup Bas n’est pas appris.

## Limite

Les presets déjà écrasés par une ancienne version ne peuvent pas être distingués automatiquement d’un choix volontaire. Ils doivent être corrigés une fois dans l’éditeur après mise à jour.
