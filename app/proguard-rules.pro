# =====================================================================
# JP Secure Lock — regras de R8/ProGuard (build release)
# Autor: Joaquim Pedro de Morais Filho
# =====================================================================

# Componentes declarados no manifesto (Activity, Services, Receivers) já são
# preservados pelo R8. O DeviceAdminReceiver é acionado pelo sistema via
# callbacks — garantimos que a classe e seus métodos permaneçam intactos.
-keep class br.com.joaquimpedro.jpsecure.JpDeviceAdminReceiver { *; }
-keep class br.com.joaquimpedro.jpsecure.BootReceiver { *; }

# androidx.security-crypto usa Google Tink; preserve o runtime de criptografia.
-keep class com.google.crypto.tink.** { *; }
-keep class androidx.security.crypto.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn javax.annotation.**

# Remove logs de depuração do binário de produção (reduz superfície e ruído).
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
}

# Mantém atributos úteis para stack traces legíveis em relatórios de crash.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Otimização agressiva do restante do código (ofusca nomes internos).
-allowaccessmodification
-repackageclasses 'jp'
