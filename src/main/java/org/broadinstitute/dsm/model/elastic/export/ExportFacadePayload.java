package org.broadinstitute.dsm.model.elastic.export;

import java.util.Objects;

import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.generate.GeneratorPayload;
import org.broadinstitute.dsm.util.PatchUtil;

public class ExportFacadePayload {

    private String index;
    private String docId;
    private GeneratorPayload generatorPayload;

    public ExportFacadePayload(String index, String docId, GeneratorPayload generatorPayload) {
        this.index = Objects.requireNonNull(index);
        this.docId = Objects.requireNonNull(docId);
        this.generatorPayload = Objects.requireNonNull(generatorPayload);
    }

    public String getIndex() {
        return index;
    }

    public String getDocId() {
        return docId;
    }

    public int getRecordId() {
        return generatorPayload.getRecordId();
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getColumnName() {
        DBElement dbElement = PatchUtil.getColumnNameMap().get(generatorPayload.getName());
        return dbElement.getColumnName();
    }

    public GeneratorPayload getGeneratorPayload() {
        return generatorPayload;
    }

    public String getFieldNameWithAlias() {
        return generatorPayload.getName();
    }
}
