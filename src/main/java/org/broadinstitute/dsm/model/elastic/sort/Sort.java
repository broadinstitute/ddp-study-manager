package org.broadinstitute.dsm.model.elastic.sort;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.parse.TypeParser;
import org.broadinstitute.dsm.model.elastic.mapping.TypeExtractor;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.elasticsearch.search.sort.SortOrder;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Sort {

    private SortBy sortBy;
    private TypeExtractor<Map<String, String>> typeExtractor;

    public Sort(SortBy sortBy,
                TypeExtractor<Map<String, String>> typeExtractor) {
        this.typeExtractor = typeExtractor;
        this.sortBy = sortBy;
    }
    
    boolean isNestedSort() {
        return Alias.of(sortBy).isCollection();
    }

    String buildFieldName() {
        
        Alias alias = Alias.of(sortBy);
        Type type = Type.valueOf(sortBy.getType());

        String outerProperty = handleOuterPropertySpecialCase();
        String innerProperty = handleInnerPropertySpecialCase();

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
        return buildPath(alias.getValue(), outerProperty, innerProperty);
    }

    private String buildPath(String... args) {
        return Stream.of(args)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(DBConstants.ALIAS_DELIMITER));
    }

    private String getKeywordIfText(Type innerType) {
        if (isTextContent(innerType) && isFieldTextType()) {
            return DBConstants.ALIAS_DELIMITER + TypeParser.KEYWORD;
        }
        return StringUtils.EMPTY;
    }

    private boolean isFieldTextType() {
        this.typeExtractor.setFields(buildPath(Alias.of(sortBy).getValue(), handleOuterPropertySpecialCase(), handleInnerPropertySpecialCase()));
        return TypeParser.TEXT.equals(typeExtractor.extract().get(handleInnerPropertySpecialCase()));
    }

    private boolean isTextContent(Type innerType) {
        return innerType == Type.TEXT || innerType == Type.TEXTAREA || innerType == Type.RADIO || innerType == Type.OPTIONS;
    }

    String buildNestedPath() {
        if (isNestedSort()) {
            Type type = Type.valueOf(sortBy.getType());
            Alias alias = Alias.of(sortBy);
            if (isDoubleNested(type, alias)) {
                return buildPath(Alias.of(sortBy).getValue(), sortBy.getOuterProperty());
            }
            return buildPath(Alias.of(sortBy).getValue());
        }
        throw new UnsupportedOperationException("Building nested path on non-nested objects is unsupported");
    }

    private boolean isDoubleNested(Type type, Alias alias) {
        return type == Type.JSONARRAY || (alias == Alias.ACTIVITIES && ElasticSearchUtil.QUESTIONS_ANSWER.equals(sortBy.getOuterProperty()));
    }

    String handleOuterPropertySpecialCase() {
        Alias alias = Alias.of(sortBy);
        if (alias.equals(Alias.PARTICIPANTDATA)) {
            return ESObjectConstants.DYNAMIC_FIELDS;
        }
        return sortBy.getOuterProperty();
    }

    public String handleInnerPropertySpecialCase() {
        if (Alias.ACTIVITIES == Alias.of(sortBy)) {
            return sortBy.getInnerProperty();
        }
        return Util.underscoresToCamelCase(sortBy.getInnerProperty());
    }

    public SortOrder getOrder() {
        return SortOrder.valueOf(sortBy.getOrder().toUpperCase());
    }

    public Alias getAlias() {
        return Alias.of(sortBy);
    }

    public String getRawAlias() {
        return sortBy.getTableAlias();
    }

    public String[] getActivityVersions() {
        return sortBy.getActivityVersions();
    }
}
