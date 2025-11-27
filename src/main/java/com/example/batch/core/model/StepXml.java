package com.example.batch.core.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.List;

public class StepXml {
    @JacksonXmlProperty(isAttribute = true)
    private String id;

    @JacksonXmlProperty(isAttribute = true)
    private String type; // tasklet | chunk

    @JacksonXmlProperty(isAttribute = true)
    private Integer commitInterval;

    @JacksonXmlProperty(isAttribute = true)
    private Integer pageSize;

    @JacksonXmlProperty(isAttribute = true)
    private String className;

    @JacksonXmlProperty(isAttribute = true)
    private String methodName;

    @JacksonXmlProperty(isAttribute = true)
    private String readerClass;

    @JacksonXmlProperty(isAttribute = true)
    private String processorClass;

    @JacksonXmlProperty(isAttribute = true)
    private String writerClass;

    @JacksonXmlProperty(localName = "property")
    private List<PropertyXml> properties;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getCommitInterval() { return commitInterval; }
    public void setCommitInterval(Integer commitInterval) { this.commitInterval = commitInterval; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
    public String getReaderClass() { return readerClass; }
    public void setReaderClass(String readerClass) { this.readerClass = readerClass; }
    public String getProcessorClass() { return processorClass; }
    public void setProcessorClass(String processorClass) { this.processorClass = processorClass; }
    public String getWriterClass() { return writerClass; }
    public void setWriterClass(String writerClass) { this.writerClass = writerClass; }
    public List<PropertyXml> getProperties() { return properties; }
    public void setProperties(List<PropertyXml> properties) { this.properties = properties; }
}
