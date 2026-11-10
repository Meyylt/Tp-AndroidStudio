# 🏃 RunCorse - Application Native Android

**Auteur :** Melissa Hassani
**Contexte :** Master 2 Informatique (Université de Corse) - TP de migration d'une PWA vers une application Android 100% native.

Ce projet démontre l'utilisation avancée des composants matériels et logiciels exclusifs au système d'exploitation Android, en utilisant l'architecture recommandée par Google (MVVM + Jetpack Compose).

## 🚀 Fonctionnalités Natives Implémentées

Contrairement à une simple application web (PWA), cette application exploite l'écosystème natif du téléphone :

1. **Architecture & Persistance Locale :**
    - Utilisation de **Room** (avec KSP) pour stocker les entraînements en local.
    - Respect du pattern **MVVM** (Model-View-ViewModel) avec l'utilisation de `StateFlow` pour une interface réactive.
2. **Synchronisation en arrière-plan :**
    - Utilisation de **WorkManager** pour envoyer les données au serveur de manière asynchrone, même si l'application est fermée ou que le réseau coupe.
3. **Interface Utilisateur Moderne :**
    - Développement 100% en **Jetpack Compose** (sans XML).
4. **Capteurs Matériels :**
    - **GPS / Localisation :** Utilisation du `FusedLocationProviderClient` avec demande de permissions dynamique, et `Geocoder` (Reverse Geocoding) pour convertir les coordonnées brutes en nom de ville.
    - **Podomètre :** Utilisation du `SensorManager` (`TYPE_STEP_COUNTER`) pour compter les pas de l'utilisateur en temps réel grâce au coprocesseur de mouvement.
5. **Intégration Système :**
    - **Notifications Locales :** Création d'un `NotificationChannel` natif pour envoyer des rappels d'entraînement au niveau de l'OS.
    - **Deep Links :** Configuration d'un Intent Filter personnalisé (`runcorse://`) dans le Manifest pour intercepter des liens web externes et ouvrir l'application automatiquement.

---

## 🎬 Démonstration en vidéo

*(Glisser-déposer la vidéo ici lors de l'édition sur GitHub.com)*

---

## 📦 Comment tester l'application ?

### Option 1 : Installer l'application (APK)
Un fichier exécutable est disponible à la racine de ce dépôt : `RunCorse_MelissaHassani.apk`.
Transférez-le sur un téléphone Android et ouvrez-le pour l'installer.

### Option 2 : Compiler le code source
1. Clonez ce dépôt.
2. Ouvrez le projet avec **Android Studio** (Koala ou supérieur recommandé).
3. Lancez l'application sur un émulateur ou un téléphone physique (Play ▶️).

---

## 🔗 Tester le Deep Link (Lien profond)

Pour simuler la réception d'un lien de partage par SMS (qui ouvre l'application native et pré-remplit les données), utilisez l'outil ADB (Android Debug Bridge) avec la commande suivante dans le terminal :
(Si vous etes sur Mac, emplacez adb par le chemin complet ~/Library/Android/sdk/platform-tools/adb)
```bash
adb shell am start -W -a android.intent.action.VIEW -d "runcorse://partage?course=Marathon_de_Corte"
