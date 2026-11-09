# Privacy policy — DeadManShot

*Last updated: 8 November 2026*

*Versión en español: [PRIVACY.es.md](PRIVACY.es.md)*

## 1. Data controller

The DeadManShot application (the "App") is published on Google Play by [REDACTED], with address at [REDACTED] (the "Provider"), under the developer name javimentallab. The Provider is the controller of the personal data referred to in section 7, and the User may contact the Provider at [REDACTED].

## 2. No processing of personal data in the App

The App does not collect any personal data of the User. It has no user accounts, advertising, analytics tools or crash reporting systems, and it sends no data to the Provider or to any server of the Provider. The only personal data processed by the Provider are those relating to the purchase of the App, described in section 7.

## 3. Data transmitted by the App and their recipients

The App connects only to the servers that the User configures, and only to carry out the actions that the User configures:

- **The User's mail server (SMTP):** to send the email written by the User, with its attachments, to the recipients chosen by the User, when the deadline expires or when the User sends a test. The connection is always encrypted (TLS) and the server's certificate is checked.
- **The User's computer:** by Wake on LAN and SSH, to run the actions that the User has enabled: the User's own command, destroying a key, destroying a folder and shutting the computer down.

The App does not choose these servers, whose operators process the data under their own terms. The recipients of the email receive what the User has written and attached. The Provider has no access to these data and takes no part in their processing.

## 4. Heart rate

If the User enables the *Pulse* function, the App reads the latest heart-rate readings from the database export of the Gadgetbridge application chosen by the User. The readings are read on the device itself, are not sent anywhere, and only the latest value and its time are kept, to show them to the User. The Provider has no access to these data.

## 5. Data stored on the device

The App stores in the device's private storage: the deadline settings; the email (recipients, subject, text and the list of attached files); the sending account; the computer details (addresses, paths, command and SSH user); the fingerprint of the SSH server; and the latest heart-rate reading. The mail and SSH passwords are stored encrypted with a key that never leaves the Android Keystore. Attached files are not copied: the App only keeps permission to read them where they are. The App's backup is disabled and will not be enabled, so none of these data leave the device in Android backups. Uninstalling the App deletes them all.

## 6. Permissions

The App asks the User for the notifications permission, from Android 13 onwards, for reminders, alarms and results, and for the exact alarms permission ("Alarms & reminders"), so that the deadline is kept on time. The other permissions used by the App are granted when it is installed and are listed on its Google Play page.

## 7. Purchase data

The App is purchased through Google Play, which handles the payment under its own terms and privacy policy. The Provider does not receive the User's payment details. For each order, Google provides the Provider with limited data, such as the order number, the date, the amount and the buyer's country.

As required by Article 13 of Regulation (EU) 2016/679 of the European Parliament and of the Council of 27 April 2016 (General Data Protection Regulation), the following information is provided:

- **Purpose:** to manage orders and refunds and to comply with the Provider's accounting and tax obligations.
- **Legal basis:** the performance of the purchase contract (Article 6(1)(b) of the General Data Protection Regulation) and compliance with the Provider's legal obligations (Article 6(1)(c)).
- **Retention period:** the period required by tax law; as a general rule, four years (Article 66 of the Spanish General Taxation Act). After that period, the data are deleted.
- **Recipients:** the data are not disclosed to third parties, except where required by law.

## 8. User's rights

The User may exercise the rights of access, rectification, erasure, restriction of processing, objection and portability, under Articles 15 to 22 of the General Data Protection Regulation, by contacting the Provider at [REDACTED]. The User may also lodge a complaint with the Spanish Data Protection Agency (www.aepd.es).

## 9. Contact

Enquiries about this policy may be addressed to the Provider at [REDACTED] or by opening an issue at https://github.com/javimentallab/DeadManShot/issues.
