# Polityka prywatności — DeadManShot

*Ostatnia aktualizacja: 9 listopada 2026 r.*

*[English](PRIVACY.md) · [Español](PRIVACY.es.md) · [Deutsch](PRIVACY.de.md) · [Français](PRIVACY.fr.md) · [Italiano](PRIVACY.it.md) · [Nederlands](PRIVACY.nl.md) · [Polski](PRIVACY.pl.md) · [Português](PRIVACY.pt.md) · [Русский](PRIVACY.ru.md) · [Українська](PRIVACY.uk.md) · [简体中文](PRIVACY.zh-CN.md) · [日本語](PRIVACY.ja.md) · [한국어](PRIVACY.ko.md) · [ไทย](PRIVACY.th.md) · [हिन्दी](PRIVACY.hi.md)*

## 1. Administrator danych

Wydawcą aplikacji DeadManShot („Aplikacja”) w Google Play jest Javier Comyn Pérez, adres: calle Olivos, número 36, 28670 Villaviciosa de Odón, Madrid, España („Dostawca”), działający pod nazwą dewelopera javimentallab. Dostawca jest administratorem danych osobowych, o których mowa w punkcie 7, a Użytkownik może skontaktować się z Dostawcą pod adresem jotacomyn@gmail.com.

## 2. Brak przetwarzania danych osobowych w Aplikacji

Aplikacja nie zbiera żadnych danych osobowych Użytkownika. Nie posiada kont użytkowników, reklam, narzędzi analitycznych ani systemów raportowania awarii i nie wysyła żadnych danych do Dostawcy ani na żaden jego serwer. Jedynymi danymi osobowymi przetwarzanymi przez Dostawcę są dane dotyczące zakupu Aplikacji, opisane w punkcie 7.

## 3. Dane przekazywane przez Aplikację i ich odbiorcy

Aplikacja łączy się wyłącznie z serwerami skonfigurowanymi przez Użytkownika i wyłącznie w celu wykonania działań skonfigurowanych przez Użytkownika:

- **Serwer poczty Użytkownika (SMTP):** w celu wysłania wiadomości e-mail napisanej przez Użytkownika, wraz z załącznikami, do wybranych przez niego odbiorców, gdy upłynie termin lub gdy Użytkownik wyśle test. Połączenie jest zawsze szyfrowane (TLS), a certyfikat serwera jest sprawdzany.
- **Komputer Użytkownika:** za pomocą Wake on LAN i SSH, w celu wykonania działań włączonych przez Użytkownika: jego własnego polecenia, zniszczenia klucza, zniszczenia folderu i wyłączenia komputera.

Aplikacja nie wybiera tych serwerów, których operatorzy przetwarzają dane na podstawie własnych warunków. Odbiorcy wiadomości e-mail otrzymują to, co Użytkownik napisał i załączył. Dostawca nie uzyskuje dostępu do tych danych ani nie uczestniczy w ich przetwarzaniu.

## 4. Tętno

Jeżeli Użytkownik włączy funkcję *Tętno*, Aplikacja odczytuje ostatnie pomiary tętna z eksportu bazy danych aplikacji Gadgetbridge wybranego przez Użytkownika. Pomiary są odczytywane na samym urządzeniu, nie są nigdzie wysyłane, a przechowywane są wyłącznie ostatnia wartość i jej godzina, aby można je było wyświetlić Użytkownikowi. Dostawca nie uzyskuje dostępu do tych danych.

## 5. Dane przechowywane na urządzeniu

Aplikacja przechowuje w prywatnej pamięci urządzenia: ustawienia terminu; wiadomość e-mail (odbiorców, temat, tekst i listę załączonych plików); konto nadawcze; dane komputera (adresy, ścieżki, polecenie i użytkownika SSH); odcisk cyfrowy serwera SSH oraz ostatni pomiar tętna. Hasła do poczty i SSH są przechowywane w postaci zaszyfrowanej kluczem, który nigdy nie opuszcza Android Keystore. Załączone pliki nie są kopiowane: Aplikacja zachowuje wyłącznie uprawnienie do ich odczytu w miejscu, w którym się znajdują. Kopia zapasowa Aplikacji jest wyłączona i nie zostanie włączona, w związku z czym żadne z tych danych nie opuszczają urządzenia w kopiach zapasowych systemu Android. Odinstalowanie Aplikacji powoduje usunięcie ich wszystkich.

## 6. Uprawnienia

Aplikacja prosi Użytkownika o uprawnienie do powiadomień, począwszy od systemu Android 13, na potrzeby przypomnień, alarmów i wyników, oraz o uprawnienie do dokładnych alarmów („Alarmy i przypomnienia”), aby termin był dotrzymany punktualnie. Pozostałe uprawnienia używane przez Aplikację są przyznawane podczas jej instalacji i wymienione na jej stronie w Google Play.

## 7. Dane dotyczące zakupu

Aplikację nabywa się za pośrednictwem serwisu Google Play, który obsługuje płatność na podstawie własnych warunków i własnej polityki prywatności. Dostawca nie otrzymuje danych płatniczych Użytkownika. W odniesieniu do każdego zamówienia Google przekazuje Dostawcy ograniczony zakres danych, takich jak numer zamówienia, data, kwota i kraj nabywcy.

Zgodnie z art. 13 rozporządzenia Parlamentu Europejskiego i Rady (UE) 2016/679 z dnia 27 kwietnia 2016 r. (ogólne rozporządzenie o ochronie danych) podaje się następujące informacje:

- **Cel:** obsługa zamówień i zwrotów oraz wypełnianie obowiązków księgowych i podatkowych Dostawcy.
- **Podstawa prawna:** wykonanie umowy zakupu (art. 6 ust. 1 lit. b) ogólnego rozporządzenia o ochronie danych) oraz wypełnienie obowiązków prawnych ciążących na Dostawcy (art. 6 ust. 1 lit. c)).
- **Okres przechowywania:** okres wymagany przepisami prawa podatkowego, co do zasady cztery lata (art. 66 hiszpańskiej ogólnej ustawy podatkowej). Po upływie tego okresu dane są usuwane.
- **Odbiorcy:** dane nie są ujawniane podmiotom trzecim, z wyjątkiem przypadków wymaganych przez prawo.

## 8. Prawa Użytkownika

Użytkownik może wykonywać prawa dostępu, sprostowania, usunięcia, ograniczenia przetwarzania, sprzeciwu i przenoszenia danych, na podstawie art. 15–22 ogólnego rozporządzenia o ochronie danych, kontaktując się z Dostawcą pod adresem jotacomyn@gmail.com. Użytkownik może również wnieść skargę do Hiszpańskiej Agencji Ochrony Danych (www.aepd.es).

## 9. Kontakt

Zapytania dotyczące niniejszej polityki można kierować do Dostawcy pod adresem jotacomyn@gmail.com lub poprzez utworzenie zgłoszenia na stronie https://github.com/javimentallab/DeadManShot/issues.
