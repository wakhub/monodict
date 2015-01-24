============
monodict
============

.. image:: https://raw.githubusercontent.com/wakhub/monodict/master/app/src/main/res/drawable-xxxhdpi/icon.png

**EN**
monodict is a faster offline PDIC_ dictionary app witch is combined with Text-to-Speech and flashcards.
The application has been developed based on the source code of aDice_
and the source code under the `Apache License v2.0`_
https://github.com/wakhub/monodict


**JA**
monodict は高速なオフライン PDIC_ 辞書ビューア、テキスト読み上げ、単語帳を一つにまとめたアプリです。
このアプリは aDice_ のソースコードを元に開発を行い、 `Apache License v2.0`_ のライセンスで公開されています。
https://github.com/wakhub/monodict

:Release Notes:
    https://github.com/wakhub/monodict/blob/master/RELEASES.rst
:Google Play Store:
    https://play.google.com/store/apps/details?id=com.github.wakhub.monodict
:.apk files:
    http://goo.gl/bn4W1G


TODO
==========

- Refactoring activity state codes by using savedInstanceState
- Use com.timehop.stickyheadersrecyclerview on MainActivity
- History
- Refine browser
- Implement translaion ruby function that is like http://zurukko.jp/
- Bluetooth integration


Setup monodict project
========================

Requirements

- `Android Studio <https://developer.android.com/sdk/installing/studio.html>`_
- `Python <https://www.python.org/>`_,
  `Fabric <http://www.fabfile.org/>`_,
  `Wand <http://docs.wand-py.org/en/0.3.9/>`_

Optionals

- `Inkscape <http://www.inkscape.org/en/>`_

Command::

    $ git clone https://github.com/wakhub/monodict.git
    $ cd monodict
    $ pip install fabric wand
    $ fab init

Then check `sdk.dir` and `ndk.dir` in your `local.properties`


Design Guideline
=================

- **Basis**

  - Follow `Material Design`_.
  - Don't follow Material Design if the design was difficult to implement
    with supported API or the design was not important for the functionality.

- Graphic Design

  - Use sans-serif fonts as app's typography because Android doesn't support
    serif-like Japanese font (Mincho) as default fonts normally.
  - Only use gray scale for theming.

    - No need to worry about the difference of colors between multiple displays.
    - Decrease battery usage.

  - Consider to add borders as a divider because Material Design doesn't use
    borders as a divider normally.
  - Follow native shadow style and shade style.

- Navigation

  - Place primary actions at bottom of Activity.
    FloatingActionButton and Toolbar at bottom are useful for this case.
  - Place secondary actions at top of Activity.
    ActionBar and Toolbar at top are useful for this case.
  - The behaviours which involve swipe gestures have to behave with
    non-gesture actions (buttons, tabs, etc...) also.


Thanks
=========

- | PDIC Home Page
  | http://homepage3.nifty.com/TaN/

- | Aquamarine Networks
  | https://sites.google.com/site/aquamarinepandora/

- | PDIC Users Page 電子辞書と英語学習のページ
  | http://homepage1.nifty.com/yoshi_2000/

- | ＩＣＨＩＲＯ
  | http://rd.vector.co.jp/vpack/browse/person/an028955.html

- | 私の好きなインドネシア
  | http://www.geocities.jp/indo_ka/index.html


Links
==========

- `pdico <https://itunes.apple.com/jp/app/pdico/id346546622>`_
  -- PDIC viewer for iOS
- `英辞郎シリーズの辞書データ（Ver.143） <http://www.dlmarket.jp/products/detail/290249>`_
  -- JPY 2,700


LICENSE
=======

::

    Copyright 2015 wak

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


See also: `legal.txt <app/src/main/res/raw/legal.txt>`_



.. _PDIC: http://homepage3.nifty.com/TaN/
.. _aDice: https://github.com/jiro-aqua/aDice
.. _Material Design: http://www.google.com/design/spec/material-design/introduction.html
.. _Apache License v2.0: http://www.apache.org/licenses/LICENSE-2.0

