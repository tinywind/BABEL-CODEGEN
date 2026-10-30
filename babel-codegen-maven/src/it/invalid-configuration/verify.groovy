assert new File(basedir, 'build.log').getText('UTF-8').contains('at least one nonblank source directory')
return true
