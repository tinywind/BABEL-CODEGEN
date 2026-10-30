def output = new File(basedir, 'target/generated-resources/static/js')
def compiled = new File(output, 'sample.js').getText('UTF-8')
assert compiled.contains('React.createElement')
assert compiled.contains('한글')
assert !compiled.contains('react/jsx-runtime')
assert new File(output, 'nested/sample.js').isFile()
assert !new File(output, 'ignored.js').exists()
assert !new File(output, 'nested/ignored.js').exists()
assert !new File(output, 'vendor').exists()
def jar = new java.util.jar.JarFile(new File(basedir, 'target/success-1.jar'))
try {
    assert jar.getEntry('static/js/sample.js') != null
    assert jar.getEntry('static/js/nested/sample.js') != null
    assert jar.getEntry('static/js/ignored.js') == null
} finally {
    jar.close()
}
return true
