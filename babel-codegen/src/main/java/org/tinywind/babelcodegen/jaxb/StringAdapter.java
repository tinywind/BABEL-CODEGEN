package org.tinywind.babelcodegen.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public final class StringAdapter extends XmlAdapter<String, String> {
    @Override
    public String unmarshal(String value) {
        if (value == null) return null;
        String input = value.trim();
        String result = input;
        int cursor = 0;
        while (cursor < input.length()) {
            int start = input.indexOf("${", cursor);
            if (start < 0) break;
            int end = input.indexOf('}', start + 2);
            if (end < 0) break;
            String name = input.substring(start + 2, end);
            if (name.indexOf('\n') >= 0 || name.indexOf('\r') >= 0 || name.indexOf('\u0085') >= 0
                    || name.indexOf('\u2028') >= 0 || name.indexOf('\u2029') >= 0) {
                cursor = start + 2;
                continue;
            }
            String expression = input.substring(start, end + 1);
            result = result.replace(expression, System.getProperty(name, expression));
            cursor = end + 1;
        }
        return result;
    }

    @Override
    public String marshal(String value) {
        return value == null ? null : value.trim();
    }
}
