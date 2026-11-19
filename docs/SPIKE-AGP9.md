# SPIKE: AGP 9.5.1 + Gradle 9.9.1 toolchain for :manager with miuix-glass 0.9.5-beta01

Branch: miuix (base liquid-glass HEAD 09d6f43). Repo: /home/zhang/test/shizuku/repo.
STATUS: BUILD SUCCESSFUL (verified 2026-11-19).

## Final toolchain (recipe actually used)
- Gradle wrapper: 9.9.1 (gradle/wrapper/gradle-wrapper.properties)
- AGP: 9.5.1 (settings.gradle pluginManagement)
- Kotlin: org.jetbrains.kotlin.android 2.4.21 + plugin.serialization 2.4.21 + plugin.compose 2.4.21
  (opt OUT of AGP built-in Kotlin; see below)
- rikka plugins: refine 4.4.0, autoresconfig 1.3.0, materialthemebuilder 1.6.0
- JDK: JAVA_HOME=/home/zhang/toolchains/jdk-21.0.13.1+2 (Java 21)

## compileSdk form
- Use plain ints: `compileSdk = 37` plus `compileSdkMinor = 2` (root build.gradle, subprojects
  plugins.withId("com.android.base") block).
- The spec form `compileSdk { version = release(37) { minorApiLevel = 2 } }` fails with
  `Value is null`; do not use.
- Evidence it is sufficient: miuix aar-metadata requires minCompileSdk=37 / minCompileMinorSdk=2.

## minSdk decision
- minSdk 24 is kept. Exactly one element added to manager/src/main/AndroidManifest.xml:
  `<uses-sdk tools:overrideLibrary="top.yukonga.miuix.kmp.blur" />`
- Do NOT put android:minSdkVersion on that element: AGP 9.0 rejects it with
  `Manifest merger failed : The <uses-sdk> tag ... is no longer allowed for controlling SDK versions (e.g., minSdkVersion).`
- Evidence: merged manifest manager/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml
  contains `<uses-sdk android:minSdkVersion="24" android:targetSdkVersion="37" />` and
  :manager:processDebugMainManifest passed.
- miuix-blur-android manifest minSdk=33 is what the overrideLibrary silences; miuix-glass-android minSdk=24.

## AGP 9 built-in Kotlin: dead end for miuix 0.9.5-beta01
- AGP 9.5.1 bundles org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10 (POM runtime dep) -> built-in
  compiler reports "compiler version 2.2.0 can read versions up to 2.3.0".
- miuix-glass-android:0.9.5-beta01 classes.jar carries `kotlin.Metadata mv=[2,4,0]` (javap on cached AAR)
  -> requires Kotlin compiler >= 2.4.0.
- `./gradlew :manager:dependencyInsight --configuration debugCompileClasspath --dependency kotlin-stdlib`
  shows miuix-*-android 0.9.5-beta01 depend on kotlin-stdlib:2.4.21 and conflict resolution picks 2.4.21.
- Result: :manager:compileDebugKotlin fails
  `Class 'kotlin.Unit' was compiled with an incompatible version of Kotlin. The actual metadata version is 2.4.0, but the compiler version 2.2.0 can read versions up to 2.3.0.`

## Opt-out recipe actually used (verified green)
1. gradle.properties:
   `android.builtInKotlin=false`
   `android.newDsl=false`
   (both required: opt-out of built-in Kotlin + legacy DSL so kotlin-android can apply)
2. settings.gradle pluginManagement plugins:
   `id "org.jetbrains.kotlin.android" version "2.4.21"`
   `id "org.jetbrains.kotlin.plugin.serialization" version "2.4.21"`
   `id "org.jetbrains.kotlin.plugin.compose" version "2.4.21"`
3. ROOT build.gradle plugins block MUST contain:
   `id "org.jetbrains.kotlin.android" apply false`
   Without this line the build fails with the same 2.2.0-vs-2.4.0 error even though settings.gradle
   declares 2.4.21: AGP puts KGP 2.2.10 on the root build classpath and the classpath-loaded plugin
   wins plugin-id resolution for subprojects. The root `apply false` forces KGP 2.4.21 onto the
   classpath; verified via `:manager:dependencyInsight --configuration kotlinCompilerClasspath
   --dependency kotlin-compiler-embeddable` (2.2.10 before, 2.4.21 after).
4. manager/build.gradle plugins: `id "org.jetbrains.kotlin.android"` BEFORE the serialization/compose
   plugin lines. Kotlin options form used: top-level
   `kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21) } }`
   (the android{} kotlin.compilerOptions form is built-in-Kotlin DSL; this top-level form compiled).
5. server/build.gradle plugins: `id "org.jetbrains.kotlin.android"` (server has 2 .kt files).
6. Keep: root build.gradle compileSdk 37 + compileSdkMinor 2, the manifest override element,
   the variant-API blocks below, miuix-glass + backdrop deps, compose build feature.

## AGP 9.5.1 variant-API rewrites (applied; build green)
- Never `tasks.named("assembleX")` inside androidComponents.onVariants: AGP registers assembleX anchor
  tasks AFTER onVariants callbacks. Observed: `Task with name 'assembleRelease' not found in project ':manager'.`
- shell dex copy -> `androidComponents { onVariants(selector().all()) { variant -> tasks.register("copyRishDex" + variant.name.capitalize()) { inputs.files(variant.artifacts.get(SingleArtifact.APK.INSTANCE)); ... } } }`
  plus afterEvaluate `tasks.named("assemble" + cap).configure { finalizedBy(copyDex) }`.
  In AGP 9 SingleArtifact.APK / OBFUSCATION_MAPPING_FILE are nested objects: `SingleArtifact.APK.INSTANCE`.
- manager preDebug/preRelease dependsOn shell -> `variant.lifecycleTasks.registerPreBuild(":shell:assemble" + cap)`
  (global preDebugBuild/preReleaseBuild tasks do not exist in AGP 9).
- outputFileName -> `variant.outputs.each { it.outputFileName.set("shizuku-v" + rootProject.ext.versionName + "-" + variant.name + ".apk") }`.
  Verified output: manager/build/outputs/apk/debug/shizuku-v13.7.0-thedjchi-debug.apk.
- collapseReleaseResourceNames: kept, via tasks.findByName("optimizeReleaseResources") + finalizedBy; aapt2
  path is now `project.android.sdkComponents.aapt2.executable` (android.sdkDirectory is gone in AGP 9).

## Removals / bumps with reasons
- org.jetbrains.kotlin.android: removed for the built-in route (AGP 9 fails: `The 'org.jetbrains.kotlin.android'
  plugin is no longer required for Kotlin support since AGP 9.0.`); RE-ADDED under the opt-out recipe.
- AGP 8.11.1 -> 9.5.1: miuix pulls androidx compose 1.12.1; aar-metadata requires AGP 9.1.0 or higher
  (`Dependency 'androidx.compose.foundation:foundation-android:1.12.1' requires Android Gradle plugin 9.1.0 or higher.`).
- Gradle wrapper 8.14 -> 9.9.1: AGP 9.5.1 minimum.
- autoresconfig 1.2.2 -> 1.3.0 and materialthemebuilder 1.5.1 -> 1.6.0: AGP 9.5.1-compatible releases.
- dev.rikka.tools.refine 4.4.0: kept (applies cleanly; repo has 0 @Refine annotations, so removable if it breaks).
- kotlinOptions{} -> top-level kotlin.compilerOptions{}: KGP modern form used under opt-out.

## Exact build command
`JAVA_HOME=/home/zhang/toolchains/jdk-21.0.13.1+2 ./gradlew :manager:assembleDebug`

## Flat fallback below API 33 (runtime, not visible at compile time)
tools:overrideLibrary only silences the manifest minSdk check. The miuix blur runtime references Android 13+
graphics symbols (RenderEffect / RuntimeShader) in top.yukonga.miuix.kmp.blur classes: DrawBackdropNode,
DrawBackdropNode$LevelTarget, DrawBackdropModifierKt, BackdropEffectsKt, BackdropEffectScopeKt,
BackdropEffectScopeImpl (grep on miuix-blur-android:0.9.5-beta01 classes.jar). Every glass surface must use a
flat (non-blur) fallback on API < 33 or it will crash at runtime.

## Verified green run
- BUILD SUCCESSFUL in 1m 14s, 257 actionable tasks: 257 executed.
- :manager:compileDebugKotlin passed (probe MiuixSpike.kt under manager/src/main/java/moe/shizuku/manager/ui/spike/
  compiles against MiuixTheme / Colors / TextStyles / rememberLayerBackdrop / glassSurface / GlassPopupDefaults).
- :shell:assembleDebug and :shell:copyRishDexDebug fire via the preBuild wire; rish dex lands at
  manager/src/main/assets/rish_shizuku.dex (6,123,412 bytes, identical to classes.dex inside shell-debug.apk).
