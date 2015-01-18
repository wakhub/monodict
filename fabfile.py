"""
Copyright (C) 2014 wak

Licensed under the Apache License, Version 2.0 (the 'License');
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an 'AS IS' BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""
from __future__ import print_function
import os
import itertools
import json
import xml.etree
from xml.etree.ElementTree import ElementTree, Element
import shutil

from wand.image import Image
from fabric.api import *
from fabric.colors import *


ENCODING = 'UTF-8'
ROOT_DIR = os.path.dirname(os.path.abspath(__file__))
APP_ROOT_DIR = os.path.join(ROOT_DIR, 'app')
APP_RES_DIR = os.path.join(APP_ROOT_DIR, 'src/main/res')
DOWNLOADS_DIR = os.path.join(ROOT_DIR, 'downloads')
CREDENTIALS_DIR = os.path.join(ROOT_DIR, 'credentials')
RELEASE_KEYSTORE = os.path.join(CREDENTIALS_DIR, 'release.keystore')
DPI = {'xhdpi': 2, 'xxhdpi': 3, 'xxxhdpi': 4}
ICON_DPI = {'xhdpi': 2, 'xxhdpi': 3}
ICON_DP_LIST = [24, 36]
ICON_COLORS = ['white', 'gray', 'blak']
SYSTEM_ICON_NAMES = [
    'ic_{}_{}_{}dp.png'.format(name, color, dpi)
    for color, dpi, name in itertools.product(
        ['white', 'black', 'grey600'],
        ICON_DP_LIST,
        ['add',
         'book',
         'bookmark',
         'close',
         'done',
         'delete',
         'edit',
         'file_download',
         'folder', 'folder_open',
         'format_bold', 'format_color_text', 'format_italic', 'format_size',
         'help',
         'list',
         'menu',
         'more_horiz', 'more_vert',
         'navigate_before', 'navigate_next',
         'play_arrow',
         'public',
         'queue',
         'refresh',
         'search',
         'settings',
         'shuffle',
         'thumb_down', 'thumb_up',
         'undo',
         'volume_up',
         ]
    )
]
ACTION_ICON_NAMES = ['ic_action_{}.png'.format(i) for i in
                     ['about', 'accept', 'add_to_queue', 'bad', 'cancel', 'collection',
                      'discard', 'download', 'edit', 'expand', 'favorite', 'forward',
                      'good', 'help', 'new', 'next', 'next_item', 'overflow',
                      'pause', 'play', 'previous', 'previous_item',
                      'refresh', 'remove',
                      'search', 'shuffle', 'sort_by_size',
                      'volume_on', 'warning']]


@task
def init():
    clean()
    local('mkdir -p ' + DOWNLOADS_DIR)
    local('mkdir -p ' + CREDENTIALS_DIR)
#    download_resources()
    if not os.path.exists(RELEASE_KEYSTORE) and prompt('Create release.keystore? [y/n]') == 'y':
        generate_keystore(RELEASE_KEYSTORE)


@task
def prepare_for_commit():
    _sort_string_xml(os.path.join(APP_ROOT_DIR, 'src/main/res/values/strings.xml'))
    _sort_string_xml(os.path.join(APP_ROOT_DIR, 'src/main/res/values-ja/strings.xml'))
    for i in ['files/icons.svg', 'files/logo.svg', 'files/feature-graphic.svg']:
        _cleanup_inkscape_svg(os.path.join(ROOT_DIR, i))
    local('rm -rf %s' % os.path.join(APP_ROOT_DIR, 'build'))
    with lcd(APP_ROOT_DIR):
        local('gradle lintClean')
    validation(False)


@task
def validation(verbose=False):
    """ validation:{verbose=False} """
    print('Validating files...')
    fails = []
    for root, dirs, files in os.walk(os.path.join(APP_ROOT_DIR, 'src')):
        for filename in files:
            fullpath = os.path.join(root, filename)
            result = 'OK'
            run_validation = False
            try:
                run_validation = _validates_file(fullpath, verbose)
            except Exception as e:
                fails.append((fullpath, e))
    if not fails:
        print('All items are valid')
    for fail in fails:
        print(red('{} ... {}'.format(*fail)))
                    
                
@task
def download_resources():
    downloads = [
    ]
    for url in downloads:
        filename = url.split('/')[-1].split('?')[0]
        print(filename)
        dest = os.path.join(DOWNLOADS_DIR, filename)
        if os.path.isfile(dest):
            print('{} already exists'.format(dest))
        else:
            _download(url, dest)
        if dest.endswith('.zip'):
            with lcd(DOWNLOADS_DIR):
                local('unzip ' + filename)


@task
def regenerate_system_icons():
    """ regenerate_action_bar_icons """
    icons_root_dir = os.path.join(ROOT_DIR, 'submodules/material-design-icons')

    for root, dirs, files in os.walk(icons_root_dir):
        group, size = root.split('/')[-2:]
        if not size.startswith('drawable-'):
            continue
        dpi = size.split('-')[-1]
        if dpi == 'xxxhdpi':
            continue
        for filename in files:
            if filename in SYSTEM_ICON_NAMES:
                orig = os.path.join(root, filename)
                dest = os.path.join(APP_RES_DIR, 'drawable-' + dpi, filename)
                with settings(warn_only=True):
                    local("cp -rf '{}' '{}'".format(orig, dest))


@task
def regenerate_custom_icons():
    icons_src_dir = os.path.join(ROOT_DIR, 'files/drawable-xxhdpi')

    for root, dirs, files in os.walk(icons_src_dir):
        for filename in files:
            name, ext = os.path.splitext(filename)
            if ext != '.png':
                continue
            _generate_multiple_images(os.path.join(root, filename))


def _generate_multiple_images(src_path, src_dpi='xxhdpi'):
    assert src_dpi in ICON_DPI.keys()

    src_power = ICON_DPI[src_dpi]

    with Image(filename=src_path) as src_image:
        for dpi in ICON_DPI.keys():
            dest_path = os.path.join(APP_RES_DIR,
                                     'drawable-' + dpi,
                                     os.path.basename(src_path))
            if dpi == src_dpi:
                print('Copying to {}...'.format(dest_path))
                shutil.copy(src_path, dest_path)
            else:
                with src_image.clone() as clone_image:
                    print('Generating {}...'.format(dest_path))
                    power = float(ICON_DPI[dpi]) / float(src_power)
                    clone_image.resize(int(power * clone_image.width),
                                       int(power * clone_image.height))
                    clone_image.save(filename=dest_path)


@task
def generate_keystore(keystore):
    """ generate_keystore:{keystore} """
    if os.path.exists(keystore):
        print(yellow('{} is already exists'.format(keystore)))
        return
    if keystore:
        print('keystore: ' + keystore)
    else:
        keystore = prompt('keystore: ')
    alias = prompt('alias: ')
    local(('keytool -genkey -v'
           ' -keystore {keystore} -alias {alias}'
           ' -keyalg RSA -keysize 2048 -validity 10000').format(**locals()))
    props_path = keystore + '.properties'
    if os.path.exists(props_path):
        return
    with open(props_path, 'w') as f:
        f.write(("storeFile={keystore}\n"
                 "storePassword=dummy\n"
                 "keyAlias={alias}\n"
                 "keyPassword=dummy").format(keystore=keystore, alias=alias))


@task
def diff_strings(matches_only=False):
    """ diff_strings:{matches_only=False} """
    tree = ElementTree()

    strings = tree.parse(os.path.join(APP_ROOT_DIR, 'src/main/res/values/strings.xml'))
    strings_ja = tree.parse(os.path.join(APP_ROOT_DIR, 'src/main/res/values-ja/strings.xml'))

    ja_items = strings_ja.findall('string')

    template = unicode("{}\n"
                       "\ten: {}\n"
                       "\tja: {}")
    for string in strings.findall('string'):
        match = None
        for ja_item in ja_items:
            if ja_item.attrib['name'] == string.attrib['name']:
                match = ja_item
        if match is None:
            print(template.format(red(string.attrib['name']),
                                  string.text,
                                  yellow('----')))
        elif not matches_only:
            print(template.format(string.attrib['name'],
                                  string.text,
                                  match.text))


@task
def clean():
    local('{}/gradlew clean packageDebug'.format(ROOT_DIR))
    local('rm -rf {}'.format(os.path.join(ROOT_DIR, 'androidapp-androidapp/build/*')))
    _clean_garbages()
    _clean_gradle_caches()


def _clean_garbages():
    for ext in ('pyc', 'swp'):
        local('find ' + ROOT_DIR + " -type f -name '*." + ext + "' -exec rm -rf {} ';'")
    for filename in ('.DS_Store', ):
        local('find ' + ROOT_DIR + " -type f -name '" + filename + "' -exec rm -rf {} ';'")


def _clean_gradle_caches():
    """
    [Gradle 0.7.0] Build fails with: Duplicate files copied in APK META-INF/LICENSE.txt
    https://groups.google.com/forum/#!topic/adt-dev/bl5Rc4Szpzg
    """
    patterns = ['META-INF/' + i for i
                in ['NOTICE*', 'LICENSE*', 'notice.txt', 'license.txt*']]
    patterns.append('LICENSE.txt')

    for pattern in patterns:
        local("find ~/.gradle/caches/"
              " -iname \'*.jar\' -exec zip -d '{}' '" + pattern + "' \\;")


def _download(url, dest):
    import requests
    print('Downloading {} => {}'.format(url, dest))
    result = requests.get(url, stream=True)
    with open(dest, 'wb') as f:
        shutil.copyfileobj(result.raw, f)


def _copy(source, dest):
    print('copy {} => {}'.format(source, dest))
    local("cp -rf '{}' '{}'".format(source, dest))


def _validates_file(path, log=False):
    ext = os.path.splitext(path)[1]
    if ext in ['.svg', '.xml']:
        if log:
            print('validate as XML: ' + path)
        xml.etree.ElementTree.parse(path)
        return True
    if ext == '.json':
        if log:
            print('validate as JSON: ' + path)
        json.load(open(path, 'r'))
        return True
    return False


def _sort_string_xml(xml_path):
    print('sorting %s' % xml_path)
    tree = ElementTree()
    original_xml = tree.parse(xml_path)

    sorted_strings = sorted(original_xml.findall('string'),
                            cmp=lambda a, b: cmp(a.attrib['name'], b.attrib['name']))
    for i in sorted_strings:
        i.tail = "\n"
    sorted_tree = ElementTree(Element('resources'))
    sorted_xml = sorted_tree.getroot()
    sorted_xml.text = "\n"
    sorted_xml.extend(sorted_strings)
    sorted_tree.write(xml_path, encoding=ENCODING)


def _cleanup_inkscape_svg(svg_path):
    print('Cleaning inkscape svg...')
    ns_dict = {'osb': 'http://www.openswatchbook.org/uri/2009/osb',
               'dc': 'http://purl.org/dc/elements/1.1/',
               'cc': 'http://creativecommons.org/ns#',
               'rdf': 'http://www.w3.org/1999/02/22-rdf-syntax-ns#',
               'svg': 'http://www.w3.org/2000/svg',
               'xlink': 'http://www.w3.org/1999/xlink',
               'sodipodi': 'http://sodipodi.sourceforge.net/DTD/sodipodi-0.dtd',
               'inkscape': 'http://www.inkscape.org/namespaces/inkscape'}
    for key in ns_dict:
        xml.etree.ElementTree.register_namespace(key, ns_dict[key])

    tree = ElementTree()
    svg = tree.parse(svg_path)
    filename_attr = '{}export-filename'.format('{' + ns_dict['inkscape'] + '}')
    for element in svg.iter('*'):
        if filename_attr in element.attrib:
            element.attrib[filename_attr] = ''
    tree.write(svg_path, encoding=ENCODING)

