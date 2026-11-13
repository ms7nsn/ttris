# Tetris — édition Material You

Remaster de « TTTris » (Seavenois, 2012). Application réécrite en Kotlin + Jetpack Compose.

- Aucune vibration : pas de permission `VIBRATE`, retour haptique Compose désactivé
- Thème Material You (couleurs dynamiques Android 12+, repli sur un thème Material 3 avant)
- Moteur réécrit : rotations SRS avec « wall kicks », sac de 7 pièces, pièce fantôme, réserve (Garder), niveaux, délai de verrouillage
- Contrôles à l'appui avec répétition automatique, pause automatique en quittant l'app
- Top 10 des scores avec date, partage du score, réglages (fantôme, grille, thème)
- Français / English

## Compiler avec GitHub

1. Crée un dépôt GitHub et envoie-y le contenu de ce dossier (le dossier `.github` compris).
2. Onglet **Actions** → workflow « Build APK » (il se lance à chaque push, ou via « Run workflow »).
3. Quand c'est vert, ouvre l'exécution et télécharge l'artefact **tetris-material-apk**.

L'APK est signé avec `app/release.keystore` (inclus), donc les builds suivants se mettent à jour par-dessus la version installée.
