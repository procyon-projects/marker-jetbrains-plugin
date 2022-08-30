package com.github.procyonprojects.marker.metadata.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.procyonprojects.marker.metadata.ProcessorResource;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessorList extends ProcessorResource {

    private Map<String, Processor> processors;

    public ProcessorList() {

    }

    public ProcessorList(Map<String, Processor> processors) {
        this.processors = processors;
    }

    public Map<String, Processor> getProcessors() {
        return processors;
    }

    public void setProcessors(Map<String, Processor> processors) {
        this.processors = processors;
    }
}
