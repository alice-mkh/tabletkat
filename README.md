TabletKat is an Xposed module that brings Android Tablet UI to Android 4.4.

Tablet UI was first introduced in Android 3.0. In 4.2 it was phased out in favor of phablet UI, which is basically phone UI stretched for large tablet screen. However, tablet UI was not removed, making it possible to restore it fairly easily.

However, in 4.4 it has been almost completely removed from code, making that impossible. So, TabletKat restores it!

Features
========

* Tablet UI ported from Jelly Bean

* Recents pinned to the left side

* Overlay Recents: floating panel above the current app (4.1 behavior)

* Dual-pane settings

* Tablet layout for Launcher2

Screenshots
===========

![Screenshot](/screenshots/1.jpg?raw=true)

![Screenshot](/screenshots/2.jpg?raw=true)

Installation
============

1. Install [Xposed Installer](http://repo.xposed.info/module/de.robv.android.xposed.installer) if you don't have it yet.

2. Install TabletKat.

3. Enable TabletKat in Xposed Installer.

4. Reboot.

5. Enjoy!

Bugs
====

* Compat mode doesn't seem to work well

* For now the code uses hidden and internal APIs. I will replace that with reflection where possible later.

* Many more!
* 




























