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
- **Primary side bar.** The catalog, the properties of the selected furniture and the
  furniture list are displayed in stacked sections: a click on the header of a section
  collapses or expands it, and dragging the sizer between two expanded sections resizes them.
  The properties of furniture are edited there instead of a dialog box, and each change
  is applied immediately. Successive changes of the same property are undone at once.
  The button on the edge of the window hides or shows the whole side bar.
- **Light color and power.** A light can be given a color or a color temperature that
  replaces the color of its sources, and its power can be entered in percentage or in
  lumens (100% = 800 lm) up to 10,000 lm. These settings are saved in the `lightColor`,
  `lightColorTemperature` and `powerUnit` attributes of lights, which the original
  program ignores.
- **Blender Cycles (GPU) renderer.** A third renderer in the *Create photo* and
  *Create video* dialogs, see [below](#blender-gpu-renderer).
- **Material opacity.** Each material of a model can be given an opacity in the
  *Materials* panel, to let light pass through it. It's saved in the `opacity` attribute
  of materials, which the original program ignores. In photos, a material with a chosen
  opacity keeps its surface and lets light pass in proportion to its transparency, in
  the three renderers, whereas the transparent materials of models are still rendered
  as glass. Light crossing a model meets each of its faces, so a closed shape stops
  more light than its opacity alone.
- **Furniture icons without screen capture under Linux.** The icon of an imported model
  is rendered off screen from the displayed view instead of being captured on screen,
  which Wayland doesn't allow without asking the user.
- **Linear light power in photos.** Photo renderers use a brightness proportional to the
  power of a light instead of its square, so homes created with the original program
  render differently unless their lights are at the default 50%: lights above 50% are
  dimmer and lights below 50% are brighter.

## Blender GPU renderer

"Blender Cycles (GPU)" is proposed beside SunFlow and YafaRay in the *Create photo* and
*Create video* dialogs, at the two highest quality levels, when Blender 4.0 or later can
be run. The home is exported once and rendered by a headless Blender process running
Cycles on the GPU (or on the CPU if Cycles finds no usable GPU). Without Blender, the
renderer simply isn't listed.

Optional System properties:

| Property | Default | Meaning |
|---|---|---|
| `com.eteks.sweethome3d.j3d.blenderExecutable` | `blender` | Blender executable |
| `com.eteks.sweethome3d.j3d.BlenderRenderer.lowQuality.samples` | 64 | Samples per pixel at the third quality level |
| `com.eteks.sweethome3d.j3d.BlenderRenderer.highQuality.samples` | 256 | Samples per pixel at the fourth quality level |
| `com.eteks.sweethome3d.j3d.BlenderRenderer.lowQuality.hiddenItemsBlockLight`, `...highQuality.hiddenItemsBlockLight` | `false` | What ceilings and levels hidden in the 3D view block without being seen, to view a floor from above lit as if the home was more complete: `sun` for the direct light of the sun only, `all` for all light (rooms without window nor lamp are then dark) |
| `com.eteks.sweethome3d.j3d.BlenderRenderer.lowQuality.exposure`, `...highQuality.exposure` | 0 | Exposure of the image in stops, each one doubling its brightness and a negative value darkening it. A view from inside a room lit by its windows is closer to a photo around 2 |

Light and sky intensities are constants at the top of
`src/com/eteks/sweethome3d/j3d/BlenderWorker.py`.

Limits:

- No preview while an image is computed; it appears when finished.
- Stopping a render ends Blender, so the next one reloads the scene.
- A video with animated furniture re-exports the whole home at each frame where something moved.
- Light sources of lamps rotated around a horizontal axis may be slightly misplaced.

The renderer comes from [sh3d-gpu-renderer](https://github.com/valsr/sh3d-gpu-renderer),
where it was a Java agent with classes in the `sh3d.gpurenderer` package and System
properties prefixed by that name.

Its worker script is tested inside Blender with:

    blender -b --factory-startup --python-exit-code 1 --python test/python/BlenderWorkerTest.py

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
| `clean` | deletes the files created by the other targets, except the JREs downloaded in `jre` |

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
