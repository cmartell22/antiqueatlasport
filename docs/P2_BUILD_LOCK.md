# P2 Build-System and Dependency Lock

Status: complete build/dependency lock; production API migration has not begun

Target: Fabric / Minecraft 26.1.2 / Java 25

Prepared: 2026-08-27

## Locked build stack

| Component | Exact lock | Evidence |
|---|---|---|
| Java language/toolchain | 25 | `options.release`, source/target compatibility, and Gradle toolchain all set to 25 |
| Gradle | 9.5.1 | Wrapper URL plus SHA-256 `bafc141b619ad6350fd975fc903156dd5c151998cc8b058e8c1044ab5f7b031f` |
| Fabric Loom remap plugin | 1.17.19 | Binary SHA-256 `AD331736D7EE6CD5F21C45B19584B951C716BA5DE8ACE8662B42813D110452B8` |
| Minecraft | 26.1.2 | Exact Mojang release and P1 artifact/source hashes |
| Modern Yarn | 26.1.2+build.4 | v2 jar SHA-256 `B1CCDDD70ED300644F1637EB19CF9041D7854F2EE2CC285A7E0DFE906C59BA7E` |
| Fabric Loader | 0.19.3 | Jar SHA-256 `73EED8C34BBAD0320A2A3CBA5346351E822F74F82B3F3C060574068474132958` |
| Fabric API | 0.155.2+26.1.2 | Jar SHA-256 `7196F2D8700CF906676DB98CC13CD4B98267C4CA4BA3E8C516E62070B21A5A3A` |
| Surveyor | 1.2.4+26.1 | Official jar SHA-256 `D705677212A38B2071B621012478109ABD13E60A9678E11A6B90F1BF7A6EEFAC` |
| Kaleido Config | 0.3.3+1.3.2 | Jar SHA-256 `168B40CDE9A066679CE9544169D2BE5186D2A8B4CDF84CB3A5FB45782CD2CBC6` |
| Cloth Config | 26.1.154 | Jar SHA-256 `FB7A5D869CE10EAAC2C2FE9AB84B4FC011A1FBFA575A93BE9845A386A7F0C7D3` |
| Mod Menu | 18.0.0 | Jar SHA-256 `EA15F4EB0812F23EBE20CC6B0144CDD5455DA0A0A0B1F21B5B733970B20A4D7E` |
| GitHub Release plugin | 2.4.1 | Exact catalog version |
| Minotaur plugin | 2.9.0 | Dynamic `2.+` selector removed |
| CurseForgeGradle plugin | 1.1.28 | Dynamic `1.1.+` selector removed |

`mavenLocal()` was removed from Atlas dependency resolution. The target now resolves the official pinned Surveyor artifact rather than allowing a same-coordinate local artifact to shadow it.

## Mapping and Loom convention

The target uses the same modern mapping approach as current Surveyor:

- plugin id `net.fabricmc.fabric-loom-remap`;
- Modern Yarn from RelativityMC;
- Loom intermediate mappings enabled;
- the exact RelativityMC intermediary URL configured explicitly.

Modern Yarn 26.1.2+build.4 was selected because it is the latest immutable 26.1.2 mapping captured during P1 and preserves the named-source migration path. This is now the production mapping lock, superseding its P1 research-only status.

## Surveyor proof

Source pin `4b500855ea2e316480c1f3fb9e175ec6dc3060db` was built in a disposable detached worktree with build metadata only changed to:

- Gradle 9.5.1;
- Loom 1.17.19;
- Minecraft 26.1.2;
- Modern Yarn 26.1.2+build.4;
- Fabric Loader 0.19.3;
- Fabric API 0.155.2+26.1.2;
- Java 25 toolchain;
- proof-only version `1.2.4+26.1.2-p2-proof`.

`.\gradlew.bat clean build --offline --no-daemon --console=plain --stacktrace` passed in 26 seconds with nine tasks executed and no test sources. The remapped proof jar is SHA-256 `72185E0899ADC69383459E67B15D7273E7C1B781A06AE7E3025FDAD4715DB97B`; its sources jar is `2DA6272FAF5DC87C5C13F9B5B59E28B2D1EF6B6F70C06D67D4B4B04ABC06D59E`.

The proof artifact is evidence only and is not an Atlas dependency. Atlas uses the official `1.2.4+26.1` artifact. Its metadata permits Minecraft `>=26.1`, Loader `>=0.18.4`, and Fabric API `>=0.144.3+26.1`; Gradle resolves those lower transitive requests to the exact Atlas lock above.

## Optional integration selection

Artifact metadata, rather than major-version inference, was used:

- Cloth Config 26.1.154 declares Minecraft `>=26.1-` and provides both `cloth-config` and `cloth-config2` identities.
- Mod Menu 18.0.0 is the matching 26.1-era client release. Mod Menu 20.0.1 was rejected because its metadata requires Minecraft 26.2 through pre-26.3.
- Kaleido Config remains 0.3.3+1.3.2, matching Surveyor's current embedded dependency.

## Validation

| Check | Result |
|---|---|
| `dependencies --configuration modCompileClasspath` | PASS; exact graph resolves and Surveyor/Fabric lower requests converge on the Atlas lock |
| `clean processResources --offline` | PASS in 14 seconds |
| Expanded `fabric.mod.json` | PASS: Roleplayer's Atlas 1.1.2+26.1.2, Minecraft 26.1.2, Loader 0.19.3, Fabric API 0.155.2+26.1.2, Surveyor 1.2.4+26.1 |
| Production source/resource diff | PASS: none |
| `compileJava --offline` | Expected P3 boundary: dependency/toolchain configuration succeeds, then javac reaches its 100-error cap on target API changes |

The compile boundary begins with the exact P1 findings—Fabric key-mapping and convention-tag packages plus the moved `BufferAllocator`—and continues into mechanical Minecraft API changes such as `ChunkPos` accessors, sound construction, message signatures, and input records. These are not P2 build failures and were intentionally left untouched.

## Host execution note

This Codex host's JDK reports Unix-domain socket support but rejects the selector pipe connection. Gradle runs were therefore launched with `jdk.net.unixdomain.tmpdir` pointed at a nonexistent path, causing the JDK's documented pipe implementation to fall back to TCP loopback. This is a local execution-harness setting only; it is not stored in project configuration or required on a normal host.

## Gate

P2 changes are limited to `build.gradle`, `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties`, and `libs.versions.toml`, plus this evidence document and durable state. No Java, mixin, resource, serialization, identity, feature, or art implementation was changed.

P3 remains blocked until the human owner explicitly approves mechanical mappings and Minecraft/Fabric API uplift. That later approval does not implicitly authorize P4 Surveyor integration or P5 rendering/mixin migration.
