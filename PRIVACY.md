# Privacy policy — DeadManShot

*Last updated: 7 November 2026*

## 1. No collection of personal data

The DeadManShot application (the "App"), developed by javimentallab, does not collect personal data. It has no user accounts, advertising, analytics tools or crash reporting, and it sends nothing to the developer or to any server of the developer.

## 2. What the App sends, and where

The App connects only to the servers that the User configures, and only to carry out the actions that the User configures:

- **The User's mail server (SMTP)**, to send the email that the User has written, with its attachments, to the recipients that the User has chosen, when the switch fires or when the User sends a test. The connection is always encrypted (TLS) and the server's certificate is checked.
- **The User's computer**, by Wake on LAN and over SSH, to run the actions that the User has turned on: the User's own command, destroying a key, shredding a folder and shutting the computer down.

The App does not choose these servers, and their operators process the data according to their own terms. The recipients of the email receive what the User has written and attached.

## 3. Heart rate

If the User turns on the Pulse function, the App reads the latest heart-rate readings from the database export of the Gadgetbridge application, which the User chooses. The readings are read on the device, are not sent anywhere, and only the latest value and its time are kept, to show them to the User.

## 4. Data stored on the device

The App stores on the device, in its private storage: the settings of the timer, the email (recipients, subject, text and the list of attached files), the sending account, the computer (addresses, paths, command and SSH user), the fingerprint of the SSH server and the latest heart-rate reading. The mail and SSH passwords are stored encrypted with a key that never leaves the Android keystore. Attached files are not copied: the App keeps permission to read them where they are. The App's backup is disabled, so none of this leaves the device in Android backups. Uninstalling the App deletes it all.

## 5. Permissions

- **Internet:** only for the mail server and the computer of section 2.
- **Notifications:** reminders, alarms and results.
- **Exact alarms and start on boot:** so that the deadline is kept on time, also after a restart.
- **Visibility of Gadgetbridge:** to find it, open it, and ask it to sync and export.

## 6. Contact

Enquiries about this policy may be addressed to the developer by opening an issue at https://github.com/javimentallab/DeadManShot/issues.
