# Configuration des URLs d'API

Le frontend ne contient plus d'URLs en dur. Voici comment configurer les endpoints pour différents environnements:

## Développement local

**Fichier**: `src/assets/config/env.json`

```json
{
  "production": false,
  "backendMotherUrl": "http://localhost:3200",
  "apiTimeout": 30000,
  "refreshInterval": 300
}
```

Démarrer le frontend avec le proxy Angular:
```bash
npm start
```

## Production / Docker

Modifier les variables d'environnement lors du build ou du démarrage du container:

### Option 1: Via fichier `env.json` (recommandé)
```json
{
  "production": true,
  "backendMotherUrl": "https://api.production.com:3200",
  "apiTimeout": 30000,
  "refreshInterval": 300
}
```

### Option 2: Via variables d'environnement (Dockerfile)
```dockerfile
ENV BACKEND_MOTHER_URL=https://api.production.com:3200
ENV API_TIMEOUT=30000
ENV REFRESH_INTERVAL=300
```

Le fichier `public/env.js` peut être généré dynamiquement:
```javascript
window.__env = {
  backendMotherUrl: process.env.BACKEND_MOTHER_URL || 'http://localhost:3200'
};
```

## Intégration Continue / Déploiement

### GitHub Actions / Jenkins
```yaml
build:
  environment:
    BACKEND_MOTHER_URL: ${{ secrets.BACKEND_MOTHER_URL }}
    BUILD_ENV: "production"
```

### Kubernetes
```yaml
env:
  - name: BACKEND_MOTHER_URL
    valueFrom:
      configMapKeyRef:
        name: frontend-config
        key: backendMotherUrl
```

## Points clés

✅ **Pas d'URLs en dur** dans le code  
✅ **ConfigService** centralise la gestion des configurations  
✅ **Fallback automatique** en cas d'erreur de chargement  
✅ **Initialisation au démarrage** via `APP_INITIALIZER`  
✅ **Support proxy Angular** pour le développement  
✅ **Adaptable** à tous les environnements (dev, staging, prod)
