============
monodict
============

.. image:: https://raw.githubusercontent.com/wakhub/monodict/master/app/src/main/res/drawable-xxxhdpi/icon.png

**monodict** is a faster offline PDIC dictionary viewer app with TTS integration and flashcard.
The application has been developed based on
the source code of `aDice <https://github.com/jiro-aqua/aDice>`_

`monodict in Google Play Store <https://play.google.com/store/apps/details?id=com.github.wakhub.monodict>`_


TODO
==========

- Refactoring activity state codes by using savedInstanceState
- Font settings
- History
- Refine browser
- Implement translaion ruby function that is like http://zurukko.jp/
- Bluetooth integration


Setup monodict Project
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


LICENSE
=======

::

    Copyright 2014 wak

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

