package com.example.batch.core.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.List;

@JacksonXmlRootElement(localName = "job")
public class JobXml {
    @JacksonXmlProperty(isAttribute = true)
    private String id;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "step")
    private List<StepXml> steps;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public List<StepXml> getSteps() { return steps; }
    public void setSteps(List<StepXml> steps) { this.steps = steps; }
}
