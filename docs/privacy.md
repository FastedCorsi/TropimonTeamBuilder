# Confidentialité — By FastedCorsi

La règle permanente du projet est conservée dans `AGENTS.md`. Les crédits et licences tiers restent protégés.

## Vérification avant livraison

Avec Java 21, exécuter :

```text
./gradlew build -PofficialDependenciesOnly
```

Le build et la CI exécutent automatiquement :

- `privacyGuardTest` : cas fictifs (identité, chemin, adresse, secret, constante compilée, archive imbriquée, attribution et expiration d'une revue).
- `privacyCheckSources` : contenu versionné et nouveaux fichiers non ignorés ; si un fichier privé est suivi malgré `.gitignore`, le contrôle échoue.
- `privacyCheckArtifacts` : les deux JAR remappés de la version courante, leurs constantes de classe, ressources, métadonnées et archives imbriquées. Le JAR binaire et le JAR de sources doivent porter l'attribution exacte.

Le contrôleur est autonome, basé sur le JDK et uniquement utilisé à la compilation. Il n'est pas embarqué dans le mod. Les tâches d'archivage excluent aussi explicitement les configurations locales, captures, sauvegardes, journaux, `.env` et dossiers `.git`. Les données rejetées ne sont ni effacées sur disque ni affichées dans les diagnostics.

## Termes privés et faux positifs

Les règles génériques détectent notamment les chemins de comptes locaux, adresses à examiner et formats courants de secrets. L'attribution est contrôlée strictement. Le nom du compte local est ajouté en mémoire, sauf comptes techniques génériques.

Des noms ou références privés supplémentaires peuvent être fournis uniquement via `TROPIMON_PRIVACY_TERMS`, séparés par des points-virgules ou retours à la ligne, dans l'environnement local ou un secret de CI. Ne jamais inscrire ces valeurs dans Git. La CI lit ce secret s'il a été configuré ; sa création distante n'est pas automatique.

Un cas fictif d'userinfo d'URL dans le test Poképaste a été examiné. `tools/privacy/reviewed-fixtures.tsv` conserve uniquement le chemin du test, la catégorie, l'empreinte normalisée et le motif de revue. Toute modification de ce fichier invalide l'exception. Aucune exception ne permet d'ignorer une identité privée ou un secret détecté. Les adresses de crédits tiers présents dans les notices d'archives tierces sont préservées.

## Limites et traces préexistantes

Ce contrôle n'est pas une garantie d'anonymisation absolue : les noms non fournis, secrets de formats inconnus, textes dans des images ou contenus chiffrés nécessitent une revue humaine. Les captures personnelles existantes ont été retirées du contenu versionné et des références de documentation ; leurs originaux locaux sont conservés.

Le nettoyage des fichiers courants ne supprime pas les traces des anciens commits, anciennes versions locales, anciens artefacts CI ou copies déjà distribuées. Aucune réécriture d'historique ni suppression distante n'est exécutée. Avant un commit expressément autorisé, vérifier séparément auteur et committer effectifs avec le pseudonyme demandé et une adresse GitHub noreply valide vérifiée, sans modifier la configuration globale.
