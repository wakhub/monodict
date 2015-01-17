============
monodict
============

.. image:: https://raw.githubusercontent.com/wakhub/monodict/master/app/src/main/res/drawable-xxxhdpi/icon.png

**monodict** is a faster offline PDIC dictionary viewer app with TTS integration and flashcard.
The application has been developed based on
the source code of `aDice <https://github.com/jiro-aqua/aDice>`_

`monodict in Google Play Store <https://play.google.com/store/apps/details?id=com.github.wakhub.monodict>`_


TOC

- TODO
- Setup monodict Project
- Graphic Design Guideline
- LICENSE


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
  `Requests <http://docs.python-requests.org/en/latest/>`_
  `Wand <http://docs.wand-py.org/en/0.3.9/>`_

Optionals

- `Inkscape <http://www.inkscape.org/en/>`_

Command::

    $ git clone https://github.com/wakhub/monodict.git
    $ cd monodict
    $ pip install fabric requests wand
    $ fab init

Then check `sdk.dir` and `ndk.dir` in your `local.properties`


Design Guideline
=================

- **Basis**
  - Follow Material Design.
  - Don't use Material Design if the design was difficult to implement with supported API or the design was not important for the functionality.

- Graphic Design
  - Only use gray scale for theming.
    - No need to worry about the difference of colors between multiple displays.
    - Decrease battery usage.

  - Consider to add lines as a divider because Material Design doesn't use border as a divider normally.
  - Follow native shadow style and shade style.

- Navigation
  - Place primary actions at bottom of Activity.
  - Place secondary actions at ActionBar or Toolbar.


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


See also: app/src/main/res/raw/legal.txt

