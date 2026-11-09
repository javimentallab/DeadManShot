# Política de privacidad — DeadManShot

*Última actualización: 8 de noviembre de 2026*

*English version: [PRIVACY.md](PRIVACY.md)*

## 1. Responsable del tratamiento

La aplicación DeadManShot (en adelante, «la Aplicación») se publica en Google Play por [REDACTED], con domicilio en [REDACTED] (en adelante, «el Titular»), con el nombre de desarrollador javimentallab. El Titular es el responsable del tratamiento de los datos personales a que se refiere el apartado 7, y el Usuario puede dirigirse a él en [REDACTED].

## 2. Ausencia de tratamiento de datos personales en la Aplicación

La Aplicación no recoge datos personales del Usuario. No dispone de cuentas de usuario, publicidad, herramientas de analítica ni sistemas de informe de fallos, y no envía ningún dato al Titular ni a ningún servidor de este. Los únicos datos personales que trata el Titular son los relativos a la compra de la Aplicación, que se describen en el apartado 7.

## 3. Datos que la Aplicación transmite y sus destinatarios

La Aplicación se conecta únicamente a los servidores que configura el Usuario, y solo para llevar a cabo las acciones que este configura:

- **Servidor de correo del Usuario (SMTP):** para enviar el correo redactado por el Usuario, con sus adjuntos, a los destinatarios que este elija, cuando vence el plazo o cuando el Usuario envía una prueba. La conexión está siempre cifrada (TLS) y se comprueba el certificado del servidor.
- **Ordenador del Usuario:** mediante Wake on LAN y SSH, para ejecutar las acciones que el Usuario haya activado: su propio comando, la destrucción de una clave, la destrucción de una carpeta y el apagado del ordenador.

La Aplicación no elige estos servidores, cuyos operadores tratan los datos conforme a sus propias condiciones. Los destinatarios del correo reciben lo que el Usuario haya escrito y adjuntado. El Titular no accede a estos datos ni interviene en su tratamiento.

## 4. Frecuencia cardiaca

Si el Usuario activa la función *Pulso*, la Aplicación lee las últimas mediciones de frecuencia cardiaca de la exportación de la base de datos de la aplicación Gadgetbridge que el Usuario elija. Las mediciones se leen en el propio dispositivo, no se envían a ninguna parte y solo se conservan el último valor y su hora, para mostrárselos al Usuario. El Titular no accede a estos datos.

## 5. Datos almacenados en el dispositivo

La Aplicación almacena en el almacenamiento privado del dispositivo: los ajustes del plazo; el correo (destinatarios, asunto, texto y lista de archivos adjuntos); la cuenta de envío; los datos del ordenador (direcciones, rutas, comando y usuario SSH); la huella del servidor SSH, y la última medición de frecuencia cardiaca. Las contraseñas del correo y de SSH se almacenan cifradas con una clave que no sale nunca del Android Keystore. Los archivos adjuntos no se copian: la Aplicación conserva únicamente el permiso para leerlos en su ubicación. La copia de seguridad de la Aplicación está desactivada y no se activará, por lo que ninguno de estos datos sale del dispositivo en las copias de seguridad de Android. La desinstalación de la Aplicación los elimina todos.

## 6. Permisos

La Aplicación solicita al Usuario el permiso de notificaciones, a partir de Android 13, para los recordatorios, las alarmas y los resultados, y el de alarmas exactas («Alarmas y recordatorios»), para que el plazo se cumpla a su hora. Los demás permisos que utiliza la Aplicación se conceden al instalarla y figuran en su ficha de Google Play.

## 7. Datos de la compra

La compra de la Aplicación se realiza a través de Google Play, que gestiona el pago conforme a sus propias condiciones y a su política de privacidad. El Titular no recibe los datos de pago del Usuario. De cada pedido, Google facilita al Titular datos limitados, como el número de pedido, la fecha, el importe y el país del comprador.

Por exigencia del artículo 13 del Reglamento (UE) 2016/679 del Parlamento Europeo y del Consejo, de 27 de abril de 2016, General de Protección de Datos, se señala lo siguiente:

- **Finalidad:** gestionar los pedidos y los reembolsos y cumplir las obligaciones contables y tributarias del Titular.
- **Base legal:** la ejecución del contrato de compra (artículo 6.1.b del Reglamento General de Protección de Datos) y el cumplimiento de las obligaciones legales del Titular (artículo 6.1.c).
- **Plazo de conservación:** el que exija la legislación tributaria; con carácter general, cuatro años (artículo 66 de la Ley General Tributaria). Transcurrido dicho plazo, los datos se suprimen.
- **Destinatarios:** los datos no se ceden a terceros, salvo obligación legal.

## 8. Derechos del Usuario

El Usuario puede ejercer los derechos de acceso, rectificación, supresión, limitación del tratamiento, oposición y portabilidad, en los términos de los artículos 15 a 22 del Reglamento General de Protección de Datos, dirigiéndose al Titular en [REDACTED]. Asimismo, puede presentar una reclamación ante la Agencia Española de Protección de Datos (www.aepd.es).

## 9. Contacto

Las consultas relativas a esta política podrán dirigirse al Titular en [REDACTED] o mediante una incidencia en https://github.com/javimentallab/DeadManShot/issues.
