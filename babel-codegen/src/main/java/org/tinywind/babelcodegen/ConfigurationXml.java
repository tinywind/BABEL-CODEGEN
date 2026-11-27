package org.tinywind.babelcodegen;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.tinywind.babelcodegen.jaxb.Configuration;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

final class ConfigurationXml {
    private ConfigurationXml() {
    }

    static Configuration load(InputStream input) throws IOException {
        try {
            SAXParserFactory parsers = SAXParserFactory.newDefaultNSInstance();
            parsers.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            parsers.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            XMLReader reader = parsers.newSAXParser().getXMLReader();
            reader.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");

            SchemaFactory schemas = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schemas.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            schemas.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Unmarshaller unmarshaller = JAXBContext.newInstance(Configuration.class).createUnmarshaller();
            unmarshaller.setSchema(schemas.newSchema(ConfigurationXml.class.getResource("/xsd/" + BabelCodegen.BABEL_CODEGEN_XSD)));
            unmarshaller.setEventHandler(event -> false);
            return (Configuration) unmarshaller.unmarshal(new SAXSource(new ConfigurationNamespace(reader), new InputSource(input)));
        } catch (ParserConfigurationException | SAXException | JAXBException e) {
            throw new IOException("Cannot parse configuration XML", e);
        }
    }

    // Older configurations use an unqualified root or a previous schema URI.
    private static final class ConfigurationNamespace extends XMLFilterImpl {
        private final Map<String, String> rootPrefixes = new LinkedHashMap<>();
        private String rootNamespace;

        ConfigurationNamespace(XMLReader reader) {
            super(reader);
        }

        private String normalize(String uri) {
            return uri.equals(rootNamespace) ? BabelCodegen.REPO_BABEL_CODEGEN_XSD : uri;
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            if (rootNamespace == null)
                rootPrefixes.put(prefix, uri);
            else
                super.startPrefixMapping(prefix, normalize(uri));
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (rootNamespace == null) {
                rootNamespace = uri;
                for (var binding : rootPrefixes.entrySet())
                    super.startPrefixMapping(binding.getKey(), normalize(binding.getValue()));
            }
            super.startElement(normalize(uri), localName, qName, attributes);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(normalize(uri), localName, qName);
        }
    }
}
