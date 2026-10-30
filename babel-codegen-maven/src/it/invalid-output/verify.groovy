assert new File(basedir, 'build.log').getText('UTF-8').contains('Cannot create target directory')
assert new File(basedir, 'output-file').getText('UTF-8') == 'The target path is a regular file.\n'
return true
