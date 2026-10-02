# Restricciones de instalación y desinstalación del usuario

Esta personalización permite limitar aplicaciones en tablets Samsung Android
8–16 enroladas como **Device Owner**, conservando la administración de APK desde
MDMesh. Requiere actualizar **servidor, consola y agente**. Cambiar solo la
consola no aplica restricciones en Android.

## Configuración

En Configurations → Restrictions:

| Campo | On | Off | Auto |
| --- | --- | --- | --- |
| Block user app installation | Bloquea fuentes desconocidas y oculta las tiendas configuradas | Retira los cambios propios del agente | Conserva la política anterior |
| Block user app uninstallation | Protege por paquete las aplicaciones instaladas | Retira las protecciones propias del agente | Conserva la política anterior |

**Auto no desbloquea.** Antes de retirar esta personalización, poner ambos
campos en Off y comprobar el resultado en cada dispositivo.

`App stores to block` acepta paquetes separados por comas. El valor por defecto
es `com.android.vending,com.sec.android.app.samsungapps` (Play Store y Galaxy
Store). `Use defaults` restaura ese valor. Borrar el texto y guardar envía una
lista explícitamente vacía: bloquea APK externos, pero no oculta tiendas.
Las tiendas ausentes no producen error. Agregar otras tiendas que existan en la
flota; no se detectan automáticamente todas las tiendas de confianza.

No incluir el agente, Ajustes, System UI ni instaladores del sistema en esa
lista. El agente rechaza componentes esenciales conocidos y los proveedores
registrados para abrir APK. Nunca oculta el instalador para bloquear al usuario.

MDMesh puede instalar y actualizar mientras la política está activa. Al quitar
una app desde MDMesh, libera **solo ese paquete**; las demás apps conservan su
protección. Si la operación falla, se cancela o vence el tiempo máximo de cinco
minutos, vuelve a proteger el paquete si aún existe. Un diario persistente
permite recuperar operaciones interrumpidas al arrancar el proceso o reiniciar.
La operación requiere liberar temporalmente la protección de ese paquete;
durante ese intervalo su eliminación no está bloqueada para el usuario tampoco.

Las políticas no utilizan `DISALLOW_INSTALL_APPS` ni `DISALLOW_UNINSTALL_APPS`,
que impedirían conservar el flujo de administración remota seleccionado.
Los cambios se serializan con las operaciones de instalación/eliminación y los
eventos de paquetes. Nuevas apps quedan protegidas tras la instalación de MDMesh;
los eventos de paquetes también programan reconciliación sin conexión.

Las tiendas ya ocultas antes de aplicar la política y las protecciones ya
existentes no se registran como cambios propios ni se retiran al desactivar.
El diario registra las mutaciones antes de ejecutarlas en Android. Si una
restauración falla, conserva el registro para poder reintentarlo.

## Compatibilidad y límites

- Nuevos campos de configuración nullable, sin activar bloqueos en las
  configuraciones existentes. La migración agrega tres columnas.
- Protocolo aditivo: `policies.userAppInstall` y `policies.userAppUninstall`
  mantienen `true = permitido`. `blockedAppStores` es una lista opcional.
  Si falta la lista se conserva la anterior; al comenzar se usa Play/Galaxy.
- Agentes antiguos reportan `unsupported` para las nuevas políticas. En el
  detalle se muestra `Partially supported`, incluso si la revisión coincide.
  Los contadores agregados del servidor mantienen su criterio anterior de
  revisión sincronizada: no usarlos como prueba de protección efectiva.
- Administra el usuario Android donde corre el agente. Usuarios secundarios,
  ADB habilitado, root y otras tiendas privilegiadas requieren controles
  adicionales. Ocultar las tiendas también deshabilita sus actualizaciones
  automáticas mientras permanezcan ocultas: distribuir APK administrados desde
  MDMesh. No sustituye Managed Google Play ni las APIs de Samsung Knox.
- Android y One UI pueden cambiar el resultado efectivo. La aceptación final
  necesita una tablet real de cada versión/modelo relevante.

## Compilación y enrolamiento de laboratorio

Seguir [CONTRIBUTING.md](../CONTRIBUTING.md) y [DEV.md](DEV.md) para las
herramientas y el servidor local. No ejecutar el instalador de producción para
preparar una instancia de prueba.

```bash
# JDK 17 + Maven
cp -n server/build.properties.example server/build.properties
mvn -pl common -am test
mvn -pl server -am package -DskipTests

# Node compatible con Vite + npm
cd web
npm ci
npm run build
node --test scripts/tests/configSync.test.cjs
cd ..

# JDK 17 + Android SDK 35
cd agent-android
./gradlew :proto:test :policy:testDebugUnitTest :core:testDebugUnitTest :app:assembleDebug detekt
```

Salidas: `server/target/launcher.war`, `web/dist/` y
`agent-android/app/build/outputs/apk/debug/app-debug.apk`.
El agente debug es `com.mdmesh.agent.debug`; no reemplaza al agente oficial.
Provisionar desde cero una tablet de laboratorio con el APK propio. El QR debe
usar el URL, paquete, componente administrador y checksum de ese APK; no usar
los datos del agente oficial. Consultar el flujo de enrolamiento del proyecto.

Para distribución posterior, generar y conservar **tu propia clave release**.
Configurar `MDM_RELEASE_STORE_FILE`, `MDM_RELEASE_STORE_PASSWORD`,
`MDM_RELEASE_KEY_ALIAS` y `MDM_RELEASE_KEY_PASSWORD`, y compilar con
`./gradlew :app:assembleRelease -PversionCode=<numero> -PversionName=<version>`.
Las actualizaciones deben conservar paquete y firma y aumentar versionCode.
No guardar claves ni contraseñas en Git. El primer APK propio define la firma
que deben conservar los agentes de esta prueba de concepto.

## Prueba de aceptación

Usar una configuración exclusiva de laboratorio en Android 8, Android 16 y al
menos una versión intermedia. Conservar registros por modelo y versión de One UI.

1. Confirmar Device Owner, comunicación y ambas capacidades nuevas anunciadas.
2. Con ambos controles Off, instalar una app de prueba desde MDMesh.
3. Activar los controles y esperar `applied` en **ambas** políticas.
4. Intentar instalar APK desde navegador/archivos y abrir Play Store y Galaxy
   Store. Comprobar que las vías configuradas están bloqueadas.
5. Intentar eliminar apps desde Ajustes, launcher y una app que solicite eliminación.
6. Instalar y actualizar una app desde MDMesh sin desactivar los bloqueos;
   comprobar que la nueva app queda protegida.
7. Eliminar una app desde MDMesh; confirmar que las demás siguen protegidas.
8. Interrumpir o provocar un error de eliminación y verificar restauración.
9. Reiniciar y actualizar el agente con APK de la misma firma; comprobar políticas
   y comunicación sin abrir manualmente la aplicación.
10. Cambiar lista de tiendas, probar una tienda ausente y comprobar que las
    tiendas retiradas de la lista se restauran solo si las ocultó esta política.
11. Seleccionar Auto tras On: el bloqueo debe continuar. Seleccionar Off y
    comprobar desbloqueo antes de retirar la personalización.
12. Revisar usuarios adicionales, depuración y tiendas distintas de las incluidas.

## Diagnóstico de los callbacks de enrolamiento

Si la instalación y el arranque normales funcionan pero el QR falla al preparar
la configuración, probar los callbacks en una tablet de laboratorio con el agente
debug como Device Owner. Esta prueba no reproduce la descarga, la verificación
de Samsung ni todo el flujo del asistente de configuración.

Compilar el APK de pruebas con la misma clave debug que firmó el agente instalado:

```bash
cd agent-android
./gradlew :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.mdmesh.agent.provisioning.ProvisioningSmokeTest \
  com.mdmesh.agent.debug.test/androidx.test.runner.AndroidJUnitRunner
```

La prueba de modo verifica `RESULT_OK`, Fully Managed y la conservación de los
extras. La de cumplimiento exige Device Owner y ejecuta el baseline real del QR
(permisos, token local de recuperación de contraseña y ubicación), esperando
`RESULT_OK`. No entrega un token de enrolamiento ni cambia el servidor configurado.
Un resultado `OK (2 tests)` valida estos callbacks en la tablet; no confirma
que el enrolamiento completo por QR funcione. Guardar también el logcat si falla.
El APK de pruebas no se distribuye a la flota.

Si ambos callbacks pasan, se puede aislar el atributo `debuggable` mediante un
APK de laboratorio que conserva el paquete `.debug`, la misma clave de firma,
los SDK y el código sin minificar:

```bash
./gradlew :app:assembleDebug -PprovisioningProbe=true -PversionCode=17
```

El resultado sigue en `app/build/outputs/apk/debug/app-debug.apk`. Esta opción
desactiva la depuración de la aplicación; no crea una release de producción.
Copiarlo con un nombre propio para evitar confundirlo con el APK debug anterior.
Puede reemplazar el archivo alojado para el QR manteniendo componente y checksum
de firma, después de verificar la firma y el nuevo SHA-256 del archivo servido.
Probar con un QR nuevo y una tablet restablecida. El resultado de este ensayo
determina si el atributo influye en el fallo; no es una reparación confirmada.

## Referencias Android

- [Restricciones del usuario](https://developer.android.com/reference/android/os/UserManager)
- [Ocultar aplicaciones](https://developer.android.com/reference/android/app/admin/DevicePolicyManager#setApplicationHidden(android.content.ComponentName,%20java.lang.String,%20boolean))
- [Protección por paquete](https://developer.android.com/reference/android/app/admin/DevicePolicyManager#setUninstallBlocked(android.content.ComponentName,%20java.lang.String,%20boolean))

## Verificación de esta implementación (2026-10-02)

- Java 17: 75 tests del módulo common correctos; servidor completo compilado y WAR generado.
- Consola: npm ci con package-lock.json, npm run build y seis pruebas del indicador correctos.
- Android: 13 tests proto, 7 policy y 81 core correctos (incluidos 13 tests de
  restricciones); detekt correcto; APK debug generado y firma verificada.
- Navegador Chromium contra fixtures: controles On/Off/Auto, restauración de
  tiendas por defecto y payload de guardado correctos, sin errores de página.
  [Captura de la consola](screenshots/app-restrictions.png).
- XML de migración y mapper y JSON del esquema validados estructuralmente.
  La migración no se ejecutó contra PostgreSQL: Docker no está activo en este equipo.
- No se realizaron pruebas de DevicePolicyManager en tablets ni un despliegue.
  Las pruebas JVM usan un backend simulado; no sustituyen el ensayo Samsung anterior.
