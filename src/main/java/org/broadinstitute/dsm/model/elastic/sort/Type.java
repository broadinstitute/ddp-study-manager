package org.broadinstitute.dsm.model.elastic.sort;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.statics.ESObjectConstants;

@Getter
public enum Type {

    TEXT(StringUtils.EMPTY),
    ADDITIONALVALUE(ESObjectConstants.DYNAMIC_FIELDS),
    JSONARRAY(StringUtils.EMPTY),
    CHECKBOX(StringUtils.EMPTY);

    private String value;

    Type(String value) {
        this.value = value;
    }

}
