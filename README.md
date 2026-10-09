# Sweet Home 3D 7.5 (Java 25 fork)

> **This is a fork of the original Sweet Home 3D source code.**
> Sweet Home 3D is developed by Emmanuel Puybaret / Space Mushrooms. The original
> application, its documentation and its downloads are at
> **<http://www.sweethome3d.com/>**.
> This fork is not affiliated with or supported by the original project.

Sweet Home 3D is an interior design Java application for drawing the plan of a home,
arranging furniture in it and viewing the result in 3D.

This repository starts from the Sweet Home 3D 7.5 source archive and updates it to
build and run on current Java.

## What differs from the original 7.5

- **Java 25.** Classes are compiled for Java 25 (the original targets Java 5 to 8).
  The build was run with JDK 26.
- **No applets.** The applet and viewer (`com.eteks.sweethome3d.applet`) and their
  build targets are removed, because the Applet API no longer exists in the JDK.
- **64 bit only.** The 32 bit Linux installer, Windows launcher and portable launchers
  are removed, along with the Mac OS X 10.4 bundle.
- **Java 3D 1.6.2 only.** The legacy Java 3D 1.5.2 / JOGL 1.x fallback is removed.
- **Bundled Java runtime.** Installer targets download the latest Eclipse Temurin 25
  JRE instead of copying a JRE installed on the build machine.
- **Updated libraries:**

  | Library | Original | This fork |
  |---|---|---|
  | JOGL / GlueGen | 2.5.0 | 2.6.0 |
  | Batik SVG path parser (subset) | 1.7 | 1.19 |
  | PDF export | iText 2.1.7 | OpenPDF 3.0.5 |
  | FreeHEP VectorGraphics (patched) | 2.1.1c | 2.4a |
  | Java 3D, Sunflow, JMF, YafaRay, JeksParser | unchanged | unchanged |

- **Smoother 3D navigation under Linux.** The plan is repainted partially and in an
  in-memory image, and the 3D view is capped at the screen refresh rate.
- **Tabbed furniture modification panel**, with *General*, *Color and texture* and
  *Light* tabs.
- **Light color and power.** A light can be given a color or a color temperature that
  replaces the color of its sources, and its power can be entered in percentage or in
  lumens (100% = 800 lm) up to 10,000 lm. These settings are saved in the `lightColor`,
  `lightColorTemperature` and `powerUnit` attributes of lights, which the original
  program ignores.
- **Linear light power in photos.** Photo renderers use a brightness proportional to the
  power of a light instead of its square, so homes created with the original program
  render differently unless their lights are at the default 50%: lights above 50% are
  dimmer and lights below 50% are brighter.

## Building

Requirements:

- JDK 25 or later
- [Apache Ant](https://ant.apache.org/)
- an internet connection and the `tar` command, for the installer targets only

Run Ant in the root of the repository:

```sh
ant
```

The default target builds `install/SweetHome3D-7.5.jar`, an executable jar that
contains the application and its libraries for Linux, Windows and macOS:

```sh
java -jar install/SweetHome3D-7.5.jar
```

Other targets (see the header of `build.xml` for the full list):

| Target | Result |
|---|---|
| `jarExecutable` (default) | `install/SweetHome3D-7.5.jar` |
| `linux64Installer` | `install/SweetHome3D-7.5-linux-x64.tgz` with a bundled JRE |
| `portableArchive` | `install/portable/SweetHome3D-7.5-portable/` for Windows and Linux |
| `windowsInstaller` | `install/SweetHome3D-7.5-windows.exe` (Windows, Launch4j and Inno Setup required) |
| `macosxInstaller` | `install/SweetHome3D-7.5-macosx.dmg` (macOS required) |
| `sourceArchive`, `javadoc` | source and Javadoc archives |

The bundled JRE archives are downloaded once into `jre/` and reused. Delete that
directory to fetch a newer release. Change `bundledJavaVersion` in `build.xml` to
bundle another Java version.

Signed installers and the `javaWebStart` target need a signature alias and a
certificate that are not part of this repository.

### Status of the build targets

Only the targets that can run on Linux have been tested in this fork: the executable
jar, the Linux 64 bit installer and the portable archive. The Windows and macOS
installer targets and their launcher configurations were updated but **not run**.
Java Web Start files are kept from the original but Java Web Start does not exist
in Java 25.

## Running from an IDE

Add the jars of `lib` and `lib/java3d-1.6` to the classpath, run
`com.eteks.sweethome3d.SweetHome3D`, and pass these VM arguments:

```
--add-opens=java.desktop/java.awt=ALL-UNNAMED
--add-opens=java.desktop/sun.awt=ALL-UNNAMED
--enable-native-access=ALL-UNNAMED
```

together with the native library path of your system:

| System | VM argument |
|---|---|
| Linux | `-Djava.library.path=lib/java3d-1.6/linux/amd64:lib/yafaray/linux/x64` |
| Windows | `-Djava.library.path=lib/java3d-1.6/windows/amd64;lib/yafaray/windows/x64` |
| macOS | `-Djava.library.path=lib/java3d-1.6/macosx:lib/yafaray/macosx` |

On macOS also add `--add-opens=java.desktop/com.apple.eio=ALL-UNNAMED` and
`--add-opens=java.desktop/com.apple.eawt=ALL-UNNAMED`.

## Repository layout

| Path | Content |
|---|---|
| `src` | application source code and resources |
| `test` | JUnit tests |
| `lib` | libraries and native libraries shipped with the application |
| `libtest` | libraries used only to compile and test |
| `install` | launcher scripts and installer configurations |
| `deploy` | Java Web Start files of the original project |
| `*-src-diff.zip` | sources of the third party libraries modified for Sweet Home 3D |

## License

Sweet Home 3D, Copyright (c) 2024 Space Mushrooms.
Distributed under the GNU General Public License; see `COPYING.TXT`.

Read `LICENSE.TXT` and the `THIRDPARTY-LICENSE-*` files for the licenses that apply
to this software and to the included materials developed by third parties.
