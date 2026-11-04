// Arquivo de build do projeto (raiz).
// Só declara as versões dos plugins; quem os aplica de fato é o app/build.gradle.kts.
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.24" apply false
}
