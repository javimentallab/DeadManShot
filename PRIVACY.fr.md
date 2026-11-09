# Politique de confidentialité — DeadManShot

*Dernière mise à jour : 9 novembre 2026*

*[English](PRIVACY.md) · [Español](PRIVACY.es.md) · [Deutsch](PRIVACY.de.md) · [Français](PRIVACY.fr.md) · [Italiano](PRIVACY.it.md) · [Nederlands](PRIVACY.nl.md) · [Polski](PRIVACY.pl.md) · [Português](PRIVACY.pt.md) · [Русский](PRIVACY.ru.md) · [Українська](PRIVACY.uk.md) · [简体中文](PRIVACY.zh-CN.md) · [日本語](PRIVACY.ja.md) · [한국어](PRIVACY.ko.md) · [ไทย](PRIVACY.th.md) · [हिन्दी](PRIVACY.hi.md) · [Català](PRIVACY.ca.md) · [Galego](PRIVACY.gl.md) · [Euskara](PRIVACY.eu.md)*

## 1. Responsable du traitement

L'application DeadManShot (l'« Application ») est publiée sur Google Play par Javier Comyn Pérez, domicilié à calle Olivos, número 36, 28670 Villaviciosa de Odón, Madrid, España (le « Fournisseur »), sous le nom de développeur javimentallab. Le Fournisseur est le responsable du traitement des données à caractère personnel visées à la section 7, et l'Utilisateur peut contacter le Fournisseur à l'adresse jotacomyn@gmail.com.

## 2. Absence de traitement de données à caractère personnel dans l'Application

L'Application ne collecte aucune donnée à caractère personnel de l'Utilisateur. Elle ne comporte ni comptes d'utilisateur, ni publicité, ni outils d'analyse, ni systèmes de rapport de plantage, et elle n'envoie aucune donnée au Fournisseur ni à aucun serveur de celui-ci. Les seules données à caractère personnel traitées par le Fournisseur sont celles relatives à l'achat de l'Application, décrites à la section 7.

## 3. Données transmises par l'Application et leurs destinataires

L'Application se connecte uniquement aux serveurs que configure l'Utilisateur, et seulement pour exécuter les actions que celui-ci configure :

- **Serveur de messagerie de l'Utilisateur (SMTP) :** pour envoyer le courrier électronique rédigé par l'Utilisateur, avec ses pièces jointes, aux destinataires que celui-ci choisit, lorsque l'échéance est atteinte ou lorsque l'Utilisateur envoie un test. La connexion est toujours chiffrée (TLS) et le certificat du serveur est vérifié.
- **Ordinateur de l'Utilisateur :** au moyen de Wake on LAN et de SSH, pour exécuter les actions que l'Utilisateur a activées : sa propre commande, la destruction d'une clé, la destruction d'un dossier et l'arrêt de l'ordinateur.

L'Application ne choisit pas ces serveurs, dont les opérateurs traitent les données conformément à leurs propres conditions. Les destinataires du courrier électronique reçoivent ce que l'Utilisateur a écrit et joint. Le Fournisseur n'accède pas à ces données et n'intervient pas dans leur traitement.

## 4. Fréquence cardiaque

Si l'Utilisateur active la fonction *Pouls*, l'Application lit les dernières mesures de fréquence cardiaque à partir de l'exportation de la base de données de l'application Gadgetbridge que l'Utilisateur choisit. Les mesures sont lues sur l'appareil lui-même, ne sont envoyées nulle part et seules la dernière valeur et son heure sont conservées, afin de les présenter à l'Utilisateur. Le Fournisseur n'accède pas à ces données.

## 5. Données stockées sur l'appareil

L'Application stocke dans l'espace de stockage privé de l'appareil : les réglages de l'échéance ; le courrier électronique (destinataires, objet, texte et liste des pièces jointes) ; le compte d'envoi ; les données de l'ordinateur (adresses, chemins, commande et utilisateur SSH) ; l'empreinte du serveur SSH, et la dernière mesure de fréquence cardiaque. Les mots de passe de la messagerie et de SSH sont stockés sous forme chiffrée au moyen d'une clé qui ne quitte jamais l'Android Keystore. Les pièces jointes ne sont pas copiées : l'Application conserve uniquement l'autorisation de les lire à leur emplacement. La sauvegarde de l'Application est désactivée et ne sera pas activée ; par conséquent, aucune de ces données ne quitte l'appareil dans les sauvegardes d'Android. La désinstallation de l'Application les supprime toutes.

## 6. Autorisations

L'Application demande à l'Utilisateur l'autorisation relative aux notifications, à partir d'Android 13, pour les rappels, les alarmes et les résultats, et celle relative aux alarmes exactes (« Alarmes et rappels »), afin que l'échéance soit respectée à l'heure. Les autres autorisations utilisées par l'Application sont accordées lors de son installation et figurent sur sa fiche Google Play.

## 7. Données d'achat

L'Application est achetée via Google Play, qui gère le paiement conformément à ses propres conditions et à sa propre politique de confidentialité. Le Fournisseur ne reçoit pas les informations de paiement de l'Utilisateur. Pour chaque commande, Google fournit au Fournisseur des données limitées, telles que le numéro de commande, la date, le montant et le pays de l'acheteur.

Conformément à l'article 13 du règlement (UE) 2016/679 du Parlement européen et du Conseil du 27 avril 2016 (règlement général sur la protection des données), les informations suivantes sont fournies :

- **Finalité :** gérer les commandes et les remboursements et respecter les obligations comptables et fiscales du Fournisseur.
- **Base juridique :** l'exécution du contrat d'achat (article 6, paragraphe 1, point b), du règlement général sur la protection des données) et le respect des obligations légales du Fournisseur (article 6, paragraphe 1, point c)).
- **Durée de conservation :** la durée exigée par la législation fiscale ; en règle générale, quatre ans (article 66 de la loi générale fiscale espagnole). À l'issue de cette durée, les données sont supprimées.
- **Destinataires :** les données ne sont pas communiquées à des tiers, sauf obligation légale.

## 8. Droits de l'Utilisateur

L'Utilisateur peut exercer les droits d'accès, de rectification, d'effacement, de limitation du traitement, d'opposition et de portabilité, prévus aux articles 15 à 22 du règlement général sur la protection des données, en contactant le Fournisseur à l'adresse jotacomyn@gmail.com. L'Utilisateur peut également introduire une réclamation auprès de l'Agence espagnole de protection des données (www.aepd.es).

## 9. Contact

Toute question relative à la présente politique peut être adressée au Fournisseur à l'adresse jotacomyn@gmail.com ou en ouvrant un ticket sur https://github.com/javimentallab/DeadManShot/issues.
