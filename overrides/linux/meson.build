project('gx-mod-downloader', 'c', version: '0.2.1', default_options: ['c_std=c17', 'warning_level=2'])

gtk = dependency('gtk+-3.0', version: '>=3.24')
webkit = dependency('webkit2gtk-4.1', version: '>=2.40')
json_glib = dependency('json-glib-1.0')
soup = dependency('libsoup-3.0')
archive = dependency('libarchive')

prefix = get_option('prefix')
datadir = join_paths(prefix, get_option('datadir'), 'gx-mod-downloader')

executable(
  'gx-mod-downloader',
  'src/main.c',
  dependencies: [gtk, webkit, json_glib, soup, archive],
  c_args: ['-DGXMOD_DATADIR="' + datadir + '"'],
  install: true,
)

install_data('../shared/gx-archive-downloader.user.js', install_dir: datadir)
install_data('../shared/wrapper-bootstrap.js', install_dir: datadir)
install_data('../shared/theme-catalog.js', install_dir: datadir)
install_data('../shared/themes.json', install_dir: datadir)
install_data('../assets/gx-icon-pack.zip', install_dir: datadir)
install_subdir('../opera-extension', install_dir: datadir)
install_data('data/dev.alastorkaneki.gxmods.desktop', install_dir: join_paths(prefix, get_option('datadir'), 'applications'))
install_data('data/dev.alastorkaneki.gxmods.metainfo.xml', install_dir: join_paths(prefix, get_option('datadir'), 'metainfo'))
install_data('../shared/gx-archive-downloader.user.js', install_dir: join_paths(datadir, 'opera-extension'))
install_data('../shared/wrapper-bootstrap.js', install_dir: join_paths(datadir, 'opera-extension'))
install_data('../shared/theme-catalog.js', install_dir: join_paths(datadir, 'opera-extension'))
