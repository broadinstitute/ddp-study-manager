package org.broadinstitute.dsm.model.elastic.sort;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.parse.TypeParser;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.statics.ESObjectConstants;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Sort {

    private SortBy sortBy;

    public Sort(SortBy sortBy) {
        sortBy.setInnerProperty(Util.underscoresToCamelCase(sortBy.getInnerProperty()));
        this.sortBy = sortBy;
    }
    
    boolean isNestedSort() {
        return Alias.of(sortBy.getTableAlias()).isCollection();
    }

    String buildFieldName() {
        
        Alias alias = Alias.of(sortBy.getTableAlias());
        Type type = Type.valueOf(sortBy.getType());

        String outerProperty = handleOuterPropertySpecialCase();
        String innerProperty = sortBy.getInnerProperty();

        switch (type) {
            case ADDITIONALVALUE:
                Type additionalValueInnerType = Type.valueOf(sortBy.getAdditionalType());
                outerProperty = Type.ADDITIONALVALUE.getValue(); 
                innerProperty += getKeywordIfText(additionalValueInnerType);
                break;
            case JSONARRAY:
                Type jsonArrayInnerType = Type.valueOf(sortBy.getAdditionalType());
                innerProperty += getKeywordIfText(jsonArrayInnerType); 
                break;
            default:
                innerProperty += getKeywordIfText(type);
                break;
        }
        return buildPath(ESObjectConstants.DSM, alias.getValue(), outerProperty, innerProperty);
    }

    private String buildPath(String... args) {
        return Stream.of(args)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(DBConstants.ALIAS_DELIMITER));
    }

    private String getKeywordIfText(Type innerType) {
        if (isTextContent(innerType)) {
            return DBConstants.ALIAS_DELIMITER + TypeParser.KEYWORD;
        }
        return StringUtils.EMPTY;
    }

    private boolean isTextContent(Type innerType) {
        return innerType == Type.TEXT || innerType == Type.TEXTAREA || innerType == Type.RADIO || innerType == Type.OPTIONS;
    }

    String buildNestedPath() {
        if (isNestedSort()) {
            Type type = Type.valueOf(sortBy.getType());
            if (type == Type.JSONARRAY) {
                return buildPath(ESObjectConstants.DSM, Alias.of(sortBy.getTableAlias()).getValue(), sortBy.getOuterProperty());
            }
            return buildPath(ESObjectConstants.DSM, Alias.of(sortBy.getTableAlias()).getValue());
        }
        throw new UnsupportedOperationException("Building nested path on non-nested objects is unsupported");
    }

    String handleOuterPropertySpecialCase() {
        Alias alias = Alias.of(sortBy.getTableAlias());
        if (alias.equals(Alias.PARTICIPANTDATA)) {
            return ESObjectConstants.DYNAMIC_FIELDS;
        }
        return sortBy.getOuterProperty();
    }
}
