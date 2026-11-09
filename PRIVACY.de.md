# Datenschutzerklärung — DeadManShot

*Letzte Aktualisierung: 9. November 2026*

*[English](PRIVACY.md) · [Español](PRIVACY.es.md) · [Deutsch](PRIVACY.de.md) · [Français](PRIVACY.fr.md) · [Italiano](PRIVACY.it.md) · [Nederlands](PRIVACY.nl.md) · [Polski](PRIVACY.pl.md) · [Português](PRIVACY.pt.md) · [Русский](PRIVACY.ru.md) · [Українська](PRIVACY.uk.md) · [简体中文](PRIVACY.zh-CN.md) · [日本語](PRIVACY.ja.md) · [한국어](PRIVACY.ko.md) · [ไทย](PRIVACY.th.md) · [हिन्दी](PRIVACY.hi.md) · [Català](PRIVACY.ca.md) · [Galego](PRIVACY.gl.md) · [Euskara](PRIVACY.eu.md)*

## 1. Verantwortlicher

Die Anwendung DeadManShot (die „App“) wird auf Google Play von Javier Comyn Pérez, mit Anschrift calle Olivos, número 36, 28670 Villaviciosa de Odón, Madrid, España (der „Anbieter“), unter dem Entwicklernamen javimentallab veröffentlicht. Der Anbieter ist der Verantwortliche für die in Abschnitt 7 genannten personenbezogenen Daten, und der Nutzer kann den Anbieter unter jotacomyn@gmail.com kontaktieren.

## 2. Keine Verarbeitung personenbezogener Daten in der App

Die App erhebt keine personenbezogenen Daten des Nutzers. Sie verfügt weder über Nutzerkonten noch über Werbung, Analysewerkzeuge oder Systeme zur Absturzberichterstattung und sendet keinerlei Daten an den Anbieter oder an einen Server des Anbieters. Die einzigen vom Anbieter verarbeiteten personenbezogenen Daten sind diejenigen, die sich auf den Kauf der App beziehen und in Abschnitt 7 beschrieben sind.

## 3. Von der App übermittelte Daten und deren Empfänger

Die App verbindet sich ausschließlich mit den vom Nutzer eingerichteten Servern und nur, um die von ihm eingerichteten Aktionen auszuführen:

- **Mailserver des Nutzers (SMTP):** zum Senden der vom Nutzer verfassten E-Mail mit ihren Anhängen an die von ihm gewählten Empfänger, wenn die Frist abläuft oder wenn der Nutzer einen Test sendet. Die Verbindung ist stets verschlüsselt (TLS), und das Zertifikat des Servers wird geprüft.
- **Computer des Nutzers:** über Wake on LAN und SSH, um die vom Nutzer aktivierten Aktionen auszuführen: seinen eigenen Befehl, die Vernichtung eines Schlüssels, die Vernichtung eines Ordners und das Herunterfahren des Computers.

Die App wählt diese Server nicht aus; deren Betreiber verarbeiten die Daten nach ihren eigenen Bedingungen. Die Empfänger der E-Mail erhalten das, was der Nutzer geschrieben und angehängt hat. Der Anbieter greift auf diese Daten nicht zu und ist an ihrer Verarbeitung nicht beteiligt.

## 4. Herzfrequenz

Aktiviert der Nutzer die Funktion *Puls*, liest die App die letzten Herzfrequenzmessungen aus dem vom Nutzer gewählten Export der Datenbank der Anwendung Gadgetbridge. Die Messungen werden auf dem Gerät selbst gelesen und nirgendwohin gesendet, und es werden nur der letzte Wert und dessen Uhrzeit gespeichert, um sie dem Nutzer anzuzeigen. Der Anbieter greift auf diese Daten nicht zu.

## 5. Auf dem Gerät gespeicherte Daten

Die App speichert im privaten Speicher des Geräts: die Einstellungen zur Frist; die E-Mail (Empfänger, Betreff, Text und Liste der Anhänge); das Absenderkonto; die Daten des Computers (Adressen, Pfade, Befehl und SSH-Benutzer); den Fingerabdruck des SSH-Servers sowie die letzte Herzfrequenzmessung. Die Passwörter für E-Mail und SSH werden mit einem Schlüssel verschlüsselt gespeichert, der den Android Keystore niemals verlässt. Die Anhänge werden nicht kopiert: Die App behält lediglich die Berechtigung, sie an ihrem Speicherort zu lesen. Die Sicherung der App ist deaktiviert und wird nicht aktiviert, sodass keine dieser Daten im Rahmen von Android-Sicherungen das Gerät verlassen. Durch die Deinstallation der App werden sie alle gelöscht.

## 6. Berechtigungen

Die App fordert vom Nutzer ab Android 13 die Berechtigung für Benachrichtigungen für die Erinnerungen, die Alarme und die Ergebnisse an sowie die Berechtigung für genaue Alarme („Wecker und Erinnerungen“), damit die Frist pünktlich eingehalten wird. Die übrigen von der App verwendeten Berechtigungen werden bei der Installation erteilt und sind auf ihrer Seite bei Google Play aufgeführt.

## 7. Kaufdaten

Die App wird über Google Play erworben, das die Zahlung nach seinen eigenen Bedingungen und seiner eigenen Datenschutzerklärung abwickelt. Der Anbieter erhält die Zahlungsdaten des Nutzers nicht. Zu jeder Bestellung stellt Google dem Anbieter begrenzte Daten zur Verfügung, etwa die Bestellnummer, das Datum, den Betrag und das Land des Käufers.

Gemäß Artikel 13 der Verordnung (EU) 2016/679 des Europäischen Parlaments und des Rates vom 27. April 2016 (Datenschutz-Grundverordnung) werden folgende Informationen bereitgestellt:

- **Zweck:** Abwicklung von Bestellungen und Erstattungen sowie Erfüllung der buchhalterischen und steuerlichen Pflichten des Anbieters.
- **Rechtsgrundlage:** die Erfüllung des Kaufvertrags (Artikel 6 Absatz 1 Buchstabe b der Datenschutz-Grundverordnung) und die Erfüllung der rechtlichen Verpflichtungen des Anbieters (Artikel 6 Absatz 1 Buchstabe c).
- **Speicherdauer:** die steuerrechtlich vorgeschriebene Frist; in der Regel vier Jahre (Artikel 66 des spanischen Allgemeinen Abgabengesetzes). Nach Ablauf dieser Frist werden die Daten gelöscht.
- **Empfänger:** Die Daten werden nicht an Dritte weitergegeben, sofern dies nicht gesetzlich vorgeschrieben ist.

## 8. Rechte des Nutzers

Der Nutzer kann die Rechte auf Auskunft, Berichtigung, Löschung, Einschränkung der Verarbeitung, Widerspruch und Datenübertragbarkeit gemäß den Artikeln 15 bis 22 der Datenschutz-Grundverordnung ausüben, indem er sich unter jotacomyn@gmail.com an den Anbieter wendet. Der Nutzer kann außerdem eine Beschwerde bei der spanischen Datenschutzbehörde (www.aepd.es) einreichen.

## 9. Kontakt

Anfragen zu dieser Datenschutzerklärung können unter jotacomyn@gmail.com an den Anbieter gerichtet oder durch Eröffnen eines Issues unter https://github.com/javimentallab/DeadManShot/issues gestellt werden.
