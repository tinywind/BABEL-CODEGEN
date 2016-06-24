/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2016 Jeon Jae-Hyeong
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.tinywind.jsxcodegen.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author tinywind
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Source")
public class Source {
    private String sourceDir;
    private String sourceEncoding;
    private String targetDir;
    private String targetEncoding;
    private Boolean overwrite;
    private String excludes;

    public String getSourceDir() {
        return sourceDir;
    }

    @XmlElement
    public void setSourceDir(String sourceDir) {
        this.sourceDir = sourceDir;
    }

    public String getSourceEncoding() {
        return sourceEncoding;
    }

    @XmlElement
    public void setSourceEncoding(String sourceEncoding) {
        this.sourceEncoding = sourceEncoding;
    }

    public String getTargetDir() {
        return targetDir;
    }

    @XmlElement
    public void setTargetDir(String targetDir) {
        this.targetDir = targetDir;
    }

    public String getTargetEncoding() {
        return targetEncoding;
    }

    @XmlElement
    public void setTargetEncoding(String targetEncoding) {
        this.targetEncoding = targetEncoding;
    }

    public Boolean getOverwrite() {
        return overwrite;
    }

    @XmlElement
    public void setOverwrite(Boolean overwrite) {
        this.overwrite = overwrite;
    }

    public String getExcludes() {
        return excludes;
    }

    @XmlElement
    public void setExcludes(String excludes) {
        this.excludes = excludes;
    }
}
