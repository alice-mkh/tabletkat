TabletKat is an Xposed module that brings Android Tablet UI to Android 4.4.

Tablet UI was first introduced in Android 3.0. In 4.2 it was phased out in favor of phablet UI, which is basically phone UI stretched for large tablet screen. However, tablet UI was not removed, making it possible to restore it fairly easily.

However, in 4.4 it has been almost completely removed from code, making that impossible. So, TabletKat restores it!

Features
========

* Fully working System Bar, with navigation buttons on left side and system indicators, clock and notifications on the right

* Notification/Quick Settings popup

* IME switcher

* Compat mode

* Recents pinned to the left side

* KitKat Immersive mode and translucency are supported

Screenshots
===========

![Screenshot](/screenshots/2.png?raw=true)

![Screenshot](/screenshots/1.jpg?raw=true)

Installation
============

1. Install [Xposed Installer](xposed.info) if you don't have it yet.

2. Install TabletKat.

3. Enable TabletKat in Xposed Installer.

4. Reboot.

5. Enjoy!

Bugs
====

* After crashing, Home and Recents buttons will be invisible. Rotate the screen to fix that. However, I hope it doesn't crash anymore. ;)

* Compat mode doesn't seem to work well

* For now the code uses hidden and internal APIs. I will replace that with reflection where possible later.
