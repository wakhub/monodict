============
monodict
============

.. image:: https://googledrive.com/host/0B7-yqP_4DkNZTjY5QmlZUTR4a2M

**monodict** is a faster offline PDIC dictionary viewer app with TTS integration and flashcard.
The application has been developed based on
the source code of `aDice <https://github.com/jiro-aqua/aDice>`_

- `monodict in Google Play Store <https://play.google.com/store/apps/details?id=com.github.wakhub.monodict>`_
- `Screenshots <https://drive.google.com/folderview?id=0B7-yqP_4DkNZUGNoX0VHOExNT2c&usp=sharing&tid=0B7-yqP_4DkNZM1NIcWxhOWthMDQ>`_


Setup monodict Project
========================

Requirements

- `Android Studio <https://developer.android.com/sdk/installing/studio.html>`_
- `Python <https://www.python.org/>`_,
  `Fabric <http://www.fabfile.org/>`_,
  `Requests <http://docs.python-requests.org/en/latest/>`_

Optionals

- `Inkscape <http://www.inkscape.org/en/>`_

Command::

    $ git clone https://github.com/wakhub/monodict.git
    $ cd monodict
    $ pip install fabric requests
    $ fab init

Then check `sdk.dir` and `ndk.dir` in your `local.properties`


TODO
==========

- Lock rotation
- Eijiro syntax support
- Font settings


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

