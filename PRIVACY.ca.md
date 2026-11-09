# Política de privadesa — DeadManShot

*Última actualització: 9 de novembre de 2026*

*[English](PRIVACY.md) · [Español](PRIVACY.es.md) · [Deutsch](PRIVACY.de.md) · [Français](PRIVACY.fr.md) · [Italiano](PRIVACY.it.md) · [Nederlands](PRIVACY.nl.md) · [Polski](PRIVACY.pl.md) · [Português](PRIVACY.pt.md) · [Русский](PRIVACY.ru.md) · [Українська](PRIVACY.uk.md) · [简体中文](PRIVACY.zh-CN.md) · [日本語](PRIVACY.ja.md) · [한국어](PRIVACY.ko.md) · [ไทย](PRIVACY.th.md) · [हिन्दी](PRIVACY.hi.md) · [Català](PRIVACY.ca.md) · [Galego](PRIVACY.gl.md) · [Euskara](PRIVACY.eu.md)*

## 1. Responsable del tractament

L'aplicació DeadManShot (l'«Aplicació») és publicada a Google Play per Javier Comyn Pérez, amb domicili a calle Olivos, número 36, 28670 Villaviciosa de Odón, Madrid, España (el «Prestador»), sota el nom de desenvolupador javimentallab. El Prestador és el responsable del tractament de les dades personals a què es refereix l'apartat 7, i l'Usuari pot contactar amb el Prestador a l'adreça jotacomyn@gmail.com.

## 2. Absència de tractament de dades personals a l'Aplicació

L'Aplicació no recull dades personals de l'Usuari. No disposa de comptes d'usuari, publicitat, eines d'analítica ni sistemes d'informe d'errors, i no envia cap dada al Prestador ni a cap servidor d'aquest. Les úniques dades personals que tracta el Prestador són les relatives a la compra de l'Aplicació, descrites a l'apartat 7.

## 3. Dades que l'Aplicació transmet i els seus destinataris

L'Aplicació es connecta únicament als servidors que configura l'Usuari, i només per dur a terme les accions que aquest configura:

- **Servidor de correu de l'Usuari (SMTP):** per enviar el correu redactat per l'Usuari, amb els seus adjunts, als destinataris que aquest triï, quan venç el termini o quan l'Usuari envia una prova. La connexió sempre és xifrada (TLS) i es comprova el certificat del servidor.
- **Ordinador de l'Usuari:** mitjançant Wake on LAN i SSH, per executar les accions que l'Usuari hagi activat: la seva pròpia ordre, la destrucció d'una clau, la destrucció d'una carpeta i l'apagada de l'ordinador.

L'Aplicació no tria aquests servidors, els operadors dels quals tracten les dades d'acord amb les seves pròpies condicions. Els destinataris del correu reben el que l'Usuari hagi escrit i adjuntat. El Prestador no accedeix a aquestes dades ni intervé en el seu tractament.

## 4. Freqüència cardíaca

Si l'Usuari activa la funció *Pols*, l'Aplicació llegeix els últims mesuraments de freqüència cardíaca de l'exportació de la base de dades de l'aplicació Gadgetbridge que triï l'Usuari. Els mesuraments es llegeixen al mateix dispositiu, no s'envien enlloc i només es conserven l'últim valor i la seva hora, per mostrar-los a l'Usuari. El Prestador no accedeix a aquestes dades.

## 5. Dades emmagatzemades al dispositiu

L'Aplicació emmagatzema a l'emmagatzematge privat del dispositiu: la configuració del termini; el correu (destinataris, assumpte, text i llista de fitxers adjunts); el compte d'enviament; les dades de l'ordinador (adreces, rutes, ordre i usuari SSH); l'empremta del servidor SSH, i l'últim mesurament de freqüència cardíaca. Les contrasenyes del correu i d'SSH s'emmagatzemen xifrades amb una clau que no surt mai de l'Android Keystore. Els fitxers adjunts no es copien: l'Aplicació conserva únicament el permís per llegir-los a la seva ubicació. La còpia de seguretat de l'Aplicació està desactivada i no s'activarà, de manera que cap d'aquestes dades no surt del dispositiu en les còpies de seguretat d'Android. La desinstal·lació de l'Aplicació les elimina totes.

## 6. Permisos

L'Aplicació sol·licita a l'Usuari el permís de notificacions, a partir d'Android 13, per als recordatoris, les alarmes i els resultats, i el d'alarmes exactes («Alarmes i recordatoris»), perquè el termini es compleixi a l'hora. Els altres permisos que utilitza l'Aplicació es concedeixen en instal·lar-la i figuren a la seva fitxa de Google Play.

## 7. Dades de compra

L'Aplicació s'adquireix a través de Google Play, que gestiona el pagament d'acord amb les seves pròpies condicions i la seva política de privadesa. El Prestador no rep les dades de pagament de l'Usuari. Per a cada comanda, Google facilita al Prestador dades limitades, com ara el número de comanda, la data, l'import i el país del comprador.

D'acord amb el que exigeix l'article 13 del Reglament (UE) 2016/679 del Parlament Europeu i del Consell, de 27 d'abril de 2016 (Reglament general de protecció de dades), es facilita la informació següent:

- **Finalitat:** gestionar les comandes i els reemborsaments i complir les obligacions comptables i fiscals del Prestador.
- **Base jurídica:** l'execució del contracte de compra (article 6.1.b) del Reglament general de protecció de dades) i el compliment de les obligacions legals del Prestador (article 6.1.c)).
- **Termini de conservació:** el termini exigit per la normativa tributària; amb caràcter general, quatre anys (article 66 de la Llei general tributària espanyola). Transcorregut aquest termini, les dades se suprimeixen.
- **Destinataris:** les dades no es comuniquen a tercers, excepte quan ho exigeixi la llei.

## 8. Drets de l'Usuari

L'Usuari pot exercir els drets d'accés, rectificació, supressió, limitació del tractament, oposició i portabilitat, d'acord amb els articles 15 a 22 del Reglament general de protecció de dades, adreçant-se al Prestador a jotacomyn@gmail.com. L'Usuari també pot presentar una reclamació davant l'Agència Espanyola de Protecció de Dades (www.aepd.es).

## 9. Contacte

Les consultes sobre aquesta política es poden adreçar al Prestador a jotacomyn@gmail.com o obrint una incidència a https://github.com/javimentallab/DeadManShot/issues.
