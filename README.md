# API Guide Michelin

Projet d'examen Backend Web services (Spring Boot 4, Java 17).

## Lancement

Les clés S3 ne sont pas dans le code, il faut les définir en variables d'environnement avant de lancer l'application (par exemple dans la configuration de lancement d'IntelliJ) :

```
S3_ACCESS_KEY_ID=...
S3_SECRET_ACCESS_KEY=...
```

Sans ces variables l'application démarre, mais les routes des images ne fonctionnent pas.

Swagger : http://localhost:8080/swagger-ui.html

## Tests

```
./mvnw test
```
