# ATB Pay — Application SoftPOS Android

**ATB Pay** est une application Android de paiement sans contact (SoftPOS — *Software Point of Sale*) développée dans le cadre d'un stage de fin d'études (PFE) à l'ENET'COM Sfax, pour le compte d'ATB/UBCI. L'application transforme un smartphone Android équipé d'une puce NFC en terminal de paiement capable de lire une carte bancaire EMV sans contact et de traiter une transaction, sans matériel dédié.

## Fonctionnalités

- **Authentification sécurisée du commerçant** : code PIN haché (SHA-256 + sel aléatoire), vérification à deux facteurs par SMS, codes de récupération locaux à usage unique en cas de PIN oublié.
- **Lecture de carte bancaire EMV contactless réelle** (non simulée) via le protocole NFC :
  - `SELECT PPSE` → découverte des applications de paiement disponibles
  - `SELECT AID` (sélection par priorité) → application de paiement
  - `GET PROCESSING OPTIONS` (GPO) → initialisation de la transaction
  - `READ RECORD` → lecture des données de la carte (PAN, date d'expiration, libellé)
  - `GENERATE AC` → génération du cryptogramme (ARQC/TC/AAC) et lecture du CID réel
- **Gestion robuste des variations de cartes** : extraction du PAN et du Track 2 directement depuis la réponse GPO pour les cartes qui n'exposent pas ces données via `READ RECORD` (observé sur certaines cartes Visa qVSDC).
- **Calcul dynamique des indicateurs terminal** :
  - TVR (*Terminal Verification Results*) — reflète l'absence du PAN, le dépassement du plafond, etc.
  - TSI (*Terminal Status Indicator*) — reflète les contrôles effectivement réalisés (ex. vérification d'expiration).
  - CID (*Cryptogram Information Data*) — lu directement depuis la réponse `GENERATE AC` de la carte.
- **Contrôle de validité de la carte** : rejet automatique des cartes expirées.
- **Service d'autorisation bancaire simulé (architecture client-serveur)** : un `Service` Android lié (`bindService`) simule la décision d'un acquéreur bancaire de façon asynchrone (listener/callback), avec une interface (`AcquirerService`) pensée pour être remplacée plus tard par une intégration bancaire réelle sans modifier le reste de l'application.
- **Génération de reçu, historique des transactions et gestion des annulations.**

## Architecture

L'application est organisée en couches, chacune avec une responsabilité précise :

```
Interface utilisateur (Activities Android)
        │
Kernel EMV (orchestration du flux transactionnel)
        │
Couche protocole (construction des commandes / parsing TLV)
        │
Couche matérielle NFC (IsoDep / ISO 14443-4)

        +  Service d'autorisation bancaire (simulé, architecture client-serveur)
```

Cette séparation permet de faire évoluer un composant (par exemple remplacer le service d'autorisation simulé par un vrai acquéreur bancaire, ou le SMS mocké par un fournisseur SMS externe) sans impacter les autres couches, grâce à des interfaces (`AcquirerService`, `SmsSender`) appliquant le principe d'inversion de dépendance.

## Stack technique

| Domaine | Technologie |
|---|---|
| Langage | Java |
| Plateforme | Android (testé sur Android 11, Samsung Galaxy A70) |
| Protocole carte | EMV contactless, ISO 14443-4, APDU / BER-TLV |
| Sécurité | SHA-256 + sel, 2FA par SMS, codes de récupération |
| Architecture | Service Android lié + callbacks (client-serveur) |
| Build | Gradle (Kotlin DSL) |

## Structure du projet

```
app/src/main/java/com/atb/nfcapp/
├── EmvTransactionManager.java      # Cœur du kernel EMV (lecture carte, TVR/TSI/CID)
├── ApduCommandBuilder.java         # Construction des commandes APDU
├── TlvParser.java                  # Parsing BER-TLV défensif
├── EmvReadResult.java              # Résultat de la lecture carte
├── PinSecurity.java                # Hachage PIN + codes de récupération
├── SmsSender.java / MockSmsSender.java   # Abstraction 2FA SMS
├── AcquirerService.java            # Interface du service d'autorisation
├── AcquirerConnectionService.java  # Service Android lié (client-serveur)
├── MockAcquirerService.java        # Implémentation simulée de l'acquéreur
├── TransactionRequest.java / AuthorizationResult.java
├── MainActivity.java               # Authentification
├── DashboardActivity.java          # Tableau de bord
├── AmountActivity.java             # Saisie du montant
├── NfcScanActivity.java            # Détection et lecture NFC
├── WaitingActivity.java            # Attente de l'autorisation
├── ReceiptActivity.java            # Reçu de transaction
├── HistoryActivity.java            # Historique
├── ManageAccountActivity.java      # Gestion du compte / sécurité
└── SmsCodeActivity.java            # Vérification 2FA
```

## Avertissement

Ce projet a été développé à des fins **académiques et de démonstration technique** dans le cadre d'un stage de fin d'études. Le service d'autorisation bancaire (`AcquirerService`) est **simulé** (`MockAcquirerService`) et ne communique avec aucun véritable système monétique. L'application n'est pas certifiée PCI CPoC/MPoC et ne doit pas être utilisée pour traiter de vraies transactions.

## Auteur

Développé par **Anouar Braham** — ENET'COM Sfax, dans le cadre d'un stage de fin d'études (PFE).
