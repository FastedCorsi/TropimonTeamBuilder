# Vérification confidentialité 0.59.5

By FastedCorsi

Contrôle effectué le 31 août 2026.

- Recompilation forcée avec Java 21 et dépendances officielles : succès.
- 67 tests du mod réussis, 14 contrôles du garde de confidentialité réussis avec données fictives.
- Sources destinées au partage contrôlées ; une adresse fictive d'userinfo dans un test de rejet d'URL est explicitement revue, avec empreinte qui expire si le fichier change.
- JAR binaire et JAR de sources contrôlés après remappage : métadonnées, ressources, constantes compilées et archives imbriquées. Attribution exacte `By FastedCorsi`, aucune occurrence des données privées recherchées, aucun secret avéré détecté.
- Les 109 classes du mod sont identiques à celles du JAR 0.59.4. Aucune modification de la logique, de la génération, des transferts ou des dépendances de fonctionnement dans ce nettoyage.
- Quatre captures retirées de l'index Git et des références de documentation ; originaux conservés localement et exclus des paquets.
- Règle permanente ajoutée aux consignes du projet, contrôle intégré au build et à la CI. Le contrôleur n'est pas une dépendance du mod.

## Traces restantes et limites

- Les deux commits de l'historique local contiennent encore des métadonnées d'identité personnelle. L'identité Git effective n'est pas conforme pour un prochain commit ; aucune configuration globale ou locale n'a été modifiée. Une adresse noreply devra être vérifiée avant un commit autorisé.
- Les quatre anciens JAR locaux examinés (binaires et sources des versions précédentes) portent toujours l'ancienne attribution. Ils ont été préservés, pas déclarés prêts à être rediffusés.
- L'API GitHub n'a retourné aucune release publique du dépôt à la date du contrôle. Les anciens artefacts CI et copies partagées ailleurs ne sont pas effacés ou certifiés par cet audit.
- Les limites du détecteur et la fourniture privée de termes complémentaires sont décrites dans `docs/privacy.md`. Aucune anonymisation absolue n'est promise.
- Aucun commit, push, publication, installation dans le launcher ou réécriture d'historique.
