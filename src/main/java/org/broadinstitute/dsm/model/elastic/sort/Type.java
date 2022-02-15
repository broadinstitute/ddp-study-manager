package org.broadinstitute.dsm.model.elastic.sort;

import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.statics.ESObjectConstants;

@Getter
public enum Type {

    TEXT(StringUtils.EMPTY),
    ADDITIONALVALUE(ESObjectConstants.DYNAMIC_FIELDS),
    JSONARRAY(StringUtils.EMPTY);

    private String value;

    Type(String value) {
        this.value = value;
    }

    public static Type of(SortBy sortBy) {
      if ("ADDITIONALVALUE".equals(sortBy.getType())) {
          return ADDITIONALVALUE;
      } else {
          Type type = valueOf(sortBy.getType());
          type.value = Objects.isNull(sortBy.getOuterProperty()) ? StringUtils.EMPTY : sortBy.getOuterProperty();
          return type;
      }
      Type type;
      switch (sortBy.getType()) {
          case "ADDITIONALVALUE":
              type = ADDITIONALVALUE;

          case JSONARRAY:
      }
    };

}
