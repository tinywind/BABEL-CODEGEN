assert new File(basedir, 'build.log').getText('UTF-8').contains('Skip BABEL-CODEGEN')
assert !new File(basedir, 'target/generated-resources').exists()
return true
