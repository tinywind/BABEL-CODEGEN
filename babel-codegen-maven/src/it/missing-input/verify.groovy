assert new File(basedir, 'build.log').getText('UTF-8').contains('Cannot read source directory')
return true
