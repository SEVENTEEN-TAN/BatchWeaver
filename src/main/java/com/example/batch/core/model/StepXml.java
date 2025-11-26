package com.example.batch.core.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.List;

public class StepXml {
    @JacksonXmlProperty(isAttribute = true)
    private String id;

    @JacksonXmlProperty(isAttribute = true)
    private String className;

    @JacksonXmlProperty(isAttribute = true)
    private String methodName;

    @JacksonXmlProperty(localName = "property")
    private List<PropertyXml> properties;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
    public List<PropertyXml> getProperties() { return properties; }
    public void setProperties(List<PropertyXml> properties) { this.properties = properties; }
}
