# Privacybeleid — DeadManShot

*Laatst bijgewerkt: 9 november 2026*

*[English](PRIVACY.md) · [Español](PRIVACY.es.md) · [Deutsch](PRIVACY.de.md) · [Français](PRIVACY.fr.md) · [Italiano](PRIVACY.it.md) · [Nederlands](PRIVACY.nl.md) · [Polski](PRIVACY.pl.md) · [Português](PRIVACY.pt.md) · [Русский](PRIVACY.ru.md) · [Українська](PRIVACY.uk.md) · [简体中文](PRIVACY.zh-CN.md) · [日本語](PRIVACY.ja.md) · [한국어](PRIVACY.ko.md) · [ไทย](PRIVACY.th.md) · [हिन्दी](PRIVACY.hi.md) · [Català](PRIVACY.ca.md) · [Galego](PRIVACY.gl.md) · [Euskara](PRIVACY.eu.md)*

## 1. Verwerkingsverantwoordelijke

De applicatie DeadManShot (de “App”) wordt op Google Play gepubliceerd door Javier Comyn Pérez, met adres te calle Olivos, número 36, 28670 Villaviciosa de Odón, Madrid, España (de “Aanbieder”), onder de ontwikkelaarsnaam javimentallab. De Aanbieder is de verwerkingsverantwoordelijke voor de in paragraaf 7 bedoelde persoonsgegevens, en de Gebruiker kan contact opnemen met de Aanbieder via jotacomyn@gmail.com.

## 2. Geen verwerking van persoonsgegevens in de App

De App verzamelt geen persoonsgegevens van de Gebruiker. Zij heeft geen gebruikersaccounts, advertenties, analysetools of systemen voor crashrapportage en verzendt geen gegevens naar de Aanbieder of naar enige server van de Aanbieder. De enige persoonsgegevens die de Aanbieder verwerkt, zijn die welke betrekking hebben op de aankoop van de App, beschreven in paragraaf 7.

## 3. Gegevens die de App doorgeeft en de ontvangers daarvan

De App maakt uitsluitend verbinding met de servers die de Gebruiker instelt, en alleen om de acties uit te voeren die de Gebruiker instelt:

- **Mailserver van de Gebruiker (SMTP):** om de door de Gebruiker opgestelde e-mail, met de bijlagen daarvan, te verzenden aan de door de Gebruiker gekozen ontvangers, wanneer de termijn verstrijkt of wanneer de Gebruiker een test verzendt. De verbinding is altijd versleuteld (TLS) en het certificaat van de server wordt gecontroleerd.
- **Computer van de Gebruiker:** via Wake on LAN en SSH, om de door de Gebruiker ingeschakelde acties uit te voeren: zijn eigen opdracht, het vernietigen van een sleutel, het vernietigen van een map en het afsluiten van de computer.

De App kiest deze servers niet; de exploitanten ervan verwerken de gegevens overeenkomstig hun eigen voorwaarden. De ontvangers van de e-mail ontvangen wat de Gebruiker heeft geschreven en bijgevoegd. De Aanbieder heeft geen toegang tot deze gegevens en is niet betrokken bij de verwerking ervan.

## 4. Hartslag

Indien de Gebruiker de functie *Hartslag* inschakelt, leest de App de laatste hartslagmetingen uit de door de Gebruiker gekozen export van de database van de applicatie Gadgetbridge. De metingen worden op het apparaat zelf gelezen, worden nergens naartoe verzonden, en alleen de laatste waarde en het tijdstip daarvan worden bewaard, om deze aan de Gebruiker te tonen. De Aanbieder heeft geen toegang tot deze gegevens.

## 5. Op het apparaat opgeslagen gegevens

De App slaat in de privéopslag van het apparaat op: de instellingen van de termijn; de e-mail (ontvangers, onderwerp, tekst en lijst van bijlagen); het verzendaccount; de gegevens van de computer (adressen, paden, opdracht en SSH-gebruiker); de vingerafdruk van de SSH-server, en de laatste hartslagmeting. De wachtwoorden voor de e-mail en voor SSH worden versleuteld opgeslagen met een sleutel die de Android Keystore nooit verlaat. De bijlagen worden niet gekopieerd: de App behoudt uitsluitend de machtiging om ze op hun locatie te lezen. De back-up van de App is uitgeschakeld en zal niet worden ingeschakeld, zodat geen van deze gegevens het apparaat verlaat in de back-ups van Android. Bij de-installatie van de App worden zij allemaal verwijderd.

## 6. Machtigingen

De App vraagt de Gebruiker om de machtiging voor meldingen, vanaf Android 13, voor de herinneringen, de alarmen en de resultaten, en om de machtiging voor exacte alarmen (“Wekkers en herinneringen”), zodat de termijn op het juiste tijdstip verstrijkt. De overige machtigingen die de App gebruikt, worden bij de installatie verleend en staan vermeld op haar pagina op Google Play.

## 7. Aankoopgegevens

De App wordt aangekocht via Google Play, dat de betaling afhandelt overeenkomstig zijn eigen voorwaarden en privacybeleid. De Aanbieder ontvangt de betaalgegevens van de Gebruiker niet. Voor elke bestelling verstrekt Google aan de Aanbieder beperkte gegevens, zoals het bestelnummer, de datum, het bedrag en het land van de koper.

Overeenkomstig artikel 13 van Verordening (EU) 2016/679 van het Europees Parlement en de Raad van 27 april 2016 (algemene verordening gegevensbescherming) wordt de volgende informatie verstrekt:

- **Doel:** het beheren van bestellingen en terugbetalingen en het voldoen aan de boekhoudkundige en fiscale verplichtingen van de Aanbieder.
- **Rechtsgrond:** de uitvoering van de koopovereenkomst (artikel 6, lid 1, punt b), van de algemene verordening gegevensbescherming) en het voldoen aan de wettelijke verplichtingen van de Aanbieder (artikel 6, lid 1, punt c)).
- **Bewaartermijn:** de door de belastingwetgeving vereiste termijn; als algemene regel vier jaar (artikel 66 van de Spaanse Algemene Belastingwet). Na afloop van die termijn worden de gegevens gewist.
- **Ontvangers:** de gegevens worden niet aan derden verstrekt, behalve indien de wet dit vereist.

## 8. Rechten van de Gebruiker

De Gebruiker kan de rechten op inzage, rectificatie, wissing, beperking van de verwerking, bezwaar en overdraagbaarheid uitoefenen, op grond van de artikelen 15 tot en met 22 van de algemene verordening gegevensbescherming, door contact op te nemen met de Aanbieder via jotacomyn@gmail.com. De Gebruiker kan ook een klacht indienen bij de Spaanse gegevensbeschermingsautoriteit (www.aepd.es).

## 9. Contact

Vragen over dit privacybeleid kunnen aan de Aanbieder worden gericht via jotacomyn@gmail.com of door een issue te openen op https://github.com/javimentallab/DeadManShot/issues.
