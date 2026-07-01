# 🌍 Guide d'utilisation - Sélection du Pays

## Comment choisir une exploitation/pays ?

### 1️⃣ Méthode 1: Via le dropdown (Recommandé)

En haut de la section **"Cartographie pays"**, vous trouverez un dropdown:

```
┌─────────────────────────┐
│ Pays / exploitation  ▼  │
│  ┌──────────────────┐   │
│  │ France (Angers) │   │
│  │ Belgique (Liège)│   │
│  │ Espagne (Bilbao)│   │
│  └──────────────────┘   │
└─────────────────────────┘
```

**Comment faire:**
1. Cliquez sur le dropdown
2. Sélectionnez le pays/exploitation souhaité
3. Les données se mettent à jour automatiquement ✅

---

### 2️⃣ Méthode 2: Cliquer sur une carte

Vous verrez une **grille de cartes** avec chaque pays/exploitation:

```
┌─────────────────┬─────────────────┐
│   FRANCE        │   BELGIQUE      │
│   🔴 ONLINE     │   🟢 ONLINE     │
│                 │                 │
│ Capteur: ✔ Actif│ Capteur: ✘ Inac │
│ Temp: 18.5°C    │ Temp: 15.2°C    │
│ Lots: 12        │ Lots: 8         │
│ Critiques: 1    │ Critiques: 0    │
└─────────────────┴─────────────────┘
```

**Comment faire:**
1. Localisez la carte du pays souhaité
2. Cliquez dessus
3. Elle se met en avant et affiche les détails ✅

---

## 📑 Après avoir sélectionné un pays, vous accédez à 4 onglets:

### 🌡️ Onglet "Capteurs"
- Affiche la **température en temps réel**
- Affiche l'**humidité en temps réel**
- Montre les **courbes d'historique** (dernières 24h)
- Tableau détaillé des mesures

### 📦 Onglet "Stocks"  
- **Liste de tous les lots** triés par date de stockage
- **État de chaque lot** (Nominal/Vigilance/Critique)
- **Cliquez sur un lot** pour voir les détails:
  - Date de stockage
  - Variété
  - Poids
  - Qualité
  - Température/Humidité moyenne

### 🚚 Onglet "Expéditions"
- Liste des **expéditions en cours**
- Statut (En transit/Livrée/Annulée)
- Détails de livraison:
  - Client
  - Destination
  - Livreur
  - Lots expédiés

### ⚠️ Onglet "Alertes"
- Toutes les **alertes du pays sélectionné**
- Triées par niveau (Critique/Vigilance)
- Avec timestamp et message détaillé

---

## 🎮 Modo "Terrain" vs "Siège"

### 👨‍🔧 Mode TERRAIN (défaut)
- Vue simplifiée du **pays sélectionné seulement**
- Accès rapide aux alertes **de votre exploitation**
- Interface optimisée pour les **utilisateurs terrain/entrepôt**

### 🏢 Mode SIÈGE
- Vue d'ensemble **consolidée de tous les pays**
- Alertes critiques **globales** (top 5)
- Interface pour **supervision et consolidation**
- Indicateurs réseau et santé globale

**Basculer de mode:** Cliquez sur les boutons en haut à gauche
```
Mode : [👨‍🔧 Terrain] [🏢 Siège]
```

---

## 💡 Conseils d'utilisation

✅ **Commencez par lancer un scan:**
- Cliquez sur "Relancer le scan" pour mettre à jour les données
- Le système rafraîchit toutes les 5 minutes automatiquement

✅ **Consultez la santé du réseau:**
- L'orbite en haut affiche le **% de disponibilité**
- Vert (100%) = tous les entrepôts en ligne ✔
- Orange = problème sur certains nœuds
- Rouge = tous les nœuds hors ligne ❌

✅ **Réagissez aux alertes:**
- La banneau rouge en haut affiche les **alertes urgentes**
- Cliquez sur l'onglet "Alertes" pour plus de détails
- Fermez la banneau si vous avez pris connaissance ✕

---

## 📱 Responsive Design

L'interface s'adapte à:
- 💻 Ordinateurs de bureau
- 📱 Tablettes
- 📵 Smartphones

Vous pouvez naviguer depuis n'importe où! 🚀
