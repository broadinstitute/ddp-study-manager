package org.broadinstitute.dsm.model;

import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.statics.DBConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public class Filter {

    public static final Logger logger = LoggerFactory.getLogger(Filter.class);

    public static final String EQUALS = " = ";
    public static final String LARGER_EQUALS = " >= ";
    public static final String SMALLER_EQUALS = " <= ";
    public static final String AND = " AND ";
    public static final String IS_NOT_NULL = " IS NOT NULL";
    public static final String IS_NULL = " IS NULL";
    public static final String IS = "IS";
    public static final String NOT = "NOT";
    public static final String NULL = "NULL";
    public static final String LIKE = " LIKE ";
    public static final String OR = "OR";
    public static final String TRUE = "true";
    public static final String FALSE = "false";

    public static String TEXT = "TEXT";
    public static String OPTIONS = "OPTIONS";
    public static String DATE = "DATE";
    public static String ADDITIONAL_VALUES = "ADDITIONALVALUE";
    public static String NUMBER = "NUMBER";
    public static String BOOLEAN = "BOOLEAN";
    public static String CHECKBOX = "CHECKBOX";
    public static String COMPOSITE = "COMPOSITE";

    public boolean range = false;
    public boolean exactMatch = false;
    public boolean empty = false;
    public boolean notEmpty = false;
    public String type;
    public String parentName;
    public NameValue filter1;
    public NameValue filter2;
    public String[] selectedOptions;
    public ParticipantColumn participantColumn;

    public static String getQueryFilteringString(@NonNull Filter filter, DBElement dbElement) {
        String finalQuery = "";
        String query = "";
        String condition = "";
        if (filter.isEmpty() && !ADDITIONAL_VALUES.equals(filter.getType())) {
            finalQuery = AND + filter.getColumnName(dbElement) + IS_NULL + " ";

        }
        else if (filter.isNotEmpty() && !ADDITIONAL_VALUES.equals(filter.getType())) {
            finalQuery = AND + filter.getColumnName(dbElement) + IS_NOT_NULL + " ";
        }
        else {
            if (StringUtils.isBlank(filter.getType()) || TEXT.equals(filter.getType()) || COMPOSITE.equals(filter.getType())) {
                filter.getFilter1().setValue(replaceQuotes(filter.getFilter1().getValue()));
                if (filter.isExactMatch()) {
                    condition = EQUALS + "'" + filter.getFilter1().getValue() + "'";
                }
                else {
                    condition = " " + LIKE + " \'%" + filter.getFilter1().getValue() + "%\'";
                }
                query = AND + filter.getColumnName(dbElement);
                finalQuery = query + condition;
            }
            else if (NUMBER.equals(filter.getType())) {
                if (!filter.isRange()) {
                    query = AND + filter.getColumnName(dbElement);
                    condition = EQUALS + filter.getFilter1().getValue();

                    finalQuery = query + condition;
                }
                else {
                    String notNullQuery = AND + filter.getColumnName(dbElement) + IS_NOT_NULL;
                    if (filter.getFilter1().getValue() != null && StringUtils.isNotBlank(String.valueOf(filter.getFilter1().getValue()))) {
                        query = AND + filter.getColumnName(dbElement);
                        condition = LARGER_EQUALS + (int) Double.parseDouble(String.valueOf(filter.getFilter1().getValue()));
                    }
                    String query2 = "";
                    String condition2 = "";
                    if (filter.getFilter2() != null && filter.getFilter2().getValue() != null && StringUtils.isNotBlank(String.valueOf(filter.getFilter2().getValue()))) {
                        query2 = AND + filter.getColumnName(dbElement);
                        condition2 = SMALLER_EQUALS + (int) Double.parseDouble(String.valueOf(filter.getFilter2().getValue()));
                    }
                    finalQuery = query + condition + query2 + condition2 + notNullQuery;
                    if (StringUtils.isNotBlank(String.valueOf(filter.getFilter1().getValue())) && !StringUtils.isNotBlank(String.valueOf(filter.getFilter2().getValue()))) {
                        finalQuery = finalQuery + notNullQuery;
                    }
                }
            }
            else if (OPTIONS.equals(filter.getType())) {
                if (filter.getSelectedOptions().length < 1) {
                    return "";
                }
                finalQuery = AND + "( ";
                for (String selectedOption : filter.getSelectedOptions()) {
                    query = filter.getColumnName(dbElement);
                    condition = EQUALS + "'" + selectedOption + "'";
                    finalQuery = finalQuery + query + condition + " " + OR + " ";
                }
                finalQuery = finalQuery.substring(0, finalQuery.length() - 4);
                finalQuery += " ) ";
            }
            else if (DATE.equals(filter.getType())) {

                if (!filter.isRange()) {
                    if (String.valueOf(filter.filter1.getValue()).length() == 10) {
                        query = AND + filter.getColumnName(dbElement);
                        condition = EQUALS + "'" + filter.getFilter1().getValue() + "'";
                        finalQuery = query + condition;
                    }
                    else {
                        query = AND + filter.getColumnName(dbElement);
                        condition = LIKE + " '%" + filter.getFilter1().getValue() + "%'";
                        finalQuery = query + condition;
                    }
                }
                else {
                    filter = convertFilterDateValues(filter);
                    String notNullQuery = AND + filter.getColumnName(dbElement) + IS_NOT_NULL;
                    if (filter.getFilter1().getValue() != null && StringUtils.isNotBlank(String.valueOf(filter.getFilter1().getValue()))) {
                        query = AND + filter.getColumnName(dbElement);
                        condition = LARGER_EQUALS + "'" + filter.getFilter1().getValue() + "'";
                    }
                    String query2 = "";
                    String condition2 = "";
                    if (filter.getFilter2() != null && filter.getFilter2().getValue() != null && StringUtils.isNotBlank(String.valueOf(filter.getFilter2().getValue()))) {
                        query2 = AND + filter.getColumnName(dbElement);
                        condition2 = SMALLER_EQUALS + "'" + filter.getFilter2().getValue() + "'";
                    }
                    finalQuery = query + condition + query2 + condition2;
                    if (filter.getFilter1().getValue() != null && filter.getFilter2() != null && filter.getFilter2().getValue() != null && !filter.getFilter1().getValue().equals("") && !filter.getFilter2().getValue().equals("")) {
                        finalQuery = finalQuery + notNullQuery;
                    }
                }
            }
            else if (ADDITIONAL_VALUES.equals(filter.getType())) {
                query = AND + " JSON_EXTRACT ( " + filter.getParentName() + DBConstants.ALIAS_DELIMITER + dbElement.columnName + " , '$." + filter.getFilter2().getName() + "' ) ";
                if (filter.empty) {
                    finalQuery = query + IS_NULL + " ";

                }
                else if (filter.notEmpty) {
                    finalQuery = query + IS_NOT_NULL + " ";
                }
                else {
                    String notNullQuery = AND + filter.getParentName() + DBConstants.ALIAS_DELIMITER + dbElement.columnName + IS_NOT_NULL;
                    if (filter.getFilter1().getValue() != null && StringUtils.isNotBlank(String.valueOf(filter.getFilter1().getValue()))) {
                        query = AND + " JSON_EXTRACT ( " + filter.getParentName() + DBConstants.ALIAS_DELIMITER + dbElement.getColumnName() + " , '$." + filter.getFilter2().getName() + "' ) ";
                        if (filter.isExactMatch()) {
                            query += EQUALS + "'#'";
                            query = query.replaceAll("#", String.valueOf(filter.getFilter1().getValue()));
                        }
                        else {
                            query += " " + LIKE + " '%#%'";
                            query = query.replaceAll("#", String.valueOf(filter.getFilter1().getValue()));
                        }
                    }
                    finalQuery = notNullQuery + query;
                }
            }
            else if (CHECKBOX.equals(filter.getType())) { //1/0
                //                String notNullQuery = AND + filter.getParentName() + "." + dbElement.getColumnName() + IS_NOT_NULL;
                if (filter.getFilter1().getValue() != null && StringUtils.isNotBlank(String.valueOf(filter.getFilter1().getValue())) && TRUE.equals(filter.getFilter1().getValue())) {
                    query = AND + filter.getParentName() + DBConstants.ALIAS_DELIMITER + dbElement.getColumnName() + EQUALS + "'1'";
                }
                else if (filter.getFilter2().getValue() != null && StringUtils.isNotBlank(String.valueOf(filter.getFilter2().getValue())) && TRUE.equals(filter.getFilter2().getValue())) {
                    query = AND + NOT + " " + filter.getParentName() + DBConstants.ALIAS_DELIMITER + dbElement.getColumnName() + " <=> 1";
                }
                //                finalQuery = notNullQuery + query;
                finalQuery = query;
            }
            else if (BOOLEAN.equals(filter.getType())) { //true/false
                if (filter.getFilter1().getValue() != null && StringUtils.isNotBlank(String.valueOf(filter.getFilter1().getValue())) && TRUE.equals(filter.getFilter1().getValue())) {
                    query = AND + filter.getParentName() + DBConstants.ALIAS_DELIMITER + filter.getFilter1().getName() + EQUALS + filter.getFilter1().getValue();
                }
                else if (filter.getFilter2().getValue() != null && StringUtils.isNotBlank(String.valueOf(filter.getFilter2().getValue())) && TRUE.equals(filter.getFilter2().getValue())) {
                    query = AND + filter.getParentName() + DBConstants.ALIAS_DELIMITER + filter.getFilter1().getName() + EQUALS + FALSE;
                }
                finalQuery = query;
            }
        }

        logger.info(finalQuery); //TODO pegah need to be removed at the end!!!
        return finalQuery;
    }

    private static Filter convertFilterDateValues(Filter filter) {
        if (filter.getFilter1() != null && filter.getFilter1().getValue() != null && String.valueOf(filter.getFilter1().getValue()).length() != 10) {
            if (String.valueOf(filter.getFilter1().getValue()).length() == 4) {
                filter.getFilter1().setValue(filter.getFilter1().getValue() + "-01-01");
            }
            else if (String.valueOf(filter.getFilter1().getValue()).length() == 7) {
                filter.getFilter1().setValue(filter.getFilter1().getValue() + "-01");
            }
        }
        if (filter.getFilter2() != null && filter.getFilter2().getValue() != null && String.valueOf(filter.getFilter2().getValue()).length() != 10) {
            if (String.valueOf(filter.getFilter2().getValue()).length() == 4) {
                filter.getFilter1().setValue(filter.getFilter1().getValue() + "-01-01");
            }
            else if (String.valueOf(filter.getFilter2().getValue()).length() == 7) {
                filter.getFilter1().setValue(filter.getFilter1().getValue() + "-01");
            }
        }
        return filter;
    }

    private String getColumnName(DBElement dbElement) {
        if (dbElement == null) {
            String tmp = StringUtils.isNotBlank(this.parentName) ? this.parentName : this.getParticipantColumn().getTableAlias();
            return tmp + DBConstants.ALIAS_DELIMITER + this.filter1.getName();
        }
        if (StringUtils.isNotBlank(dbElement.getTableAlias())) {
            return dbElement.getTableAlias() + DBConstants.ALIAS_DELIMITER + dbElement.getColumnName();
        }
        else {
            String tmp = StringUtils.isNotBlank(this.parentName) ? this.parentName : this.getParticipantColumn().getTableAlias();
            return tmp + DBConstants.ALIAS_DELIMITER + dbElement.getColumnName();
        }
    }

    private static Object replaceQuotes(Object text) {
        if (text != null && ((String) text).contains("'")) {
            String tmp = ((String) text).replace("'", "");
            return replaceQuotes(tmp);
        }
        return text;
    }
}
