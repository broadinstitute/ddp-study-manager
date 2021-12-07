package org.broadinstitute.dsm.model.elastic.export.process;

import java.util.Objects;

import org.broadinstitute.dsm.model.elastic.ESDsm;
import org.broadinstitute.dsm.model.elastic.export.generate.Collector;

public abstract class BaseProcessor implements Processor {


    protected ESDsm esDsm;
    protected String propertyName;
    protected int recordId;
    protected Collector collector;

    public BaseProcessor(ESDsm esDsm, String propertyName, int recordId, Collector collector) {
        this.esDsm = Objects.requireNonNull(esDsm);
        this.propertyName = Objects.requireNonNull(propertyName);
        this.recordId = recordId;
        this.collector = collector;
    }

    public BaseProcessor() {

    }

    public void setEsDsm(ESDsm esDsm) {
        this.esDsm = esDsm;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public void setCollector(Collector collector) {
        this.collector = collector;
    }
}
