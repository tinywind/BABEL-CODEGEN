assert new File(basedir, 'build.log').getText('UTF-8').contains('broken.jsx')
assert new File(basedir, 'existing/broken.js').getText('UTF-8') == 'preserved-content\n'
return true
