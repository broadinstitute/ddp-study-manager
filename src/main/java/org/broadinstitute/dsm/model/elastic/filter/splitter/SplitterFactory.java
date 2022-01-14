package org.broadinstitute.dsm.model.elastic.filter.splitter;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.Filter;
import org.broadinstitute.dsm.model.elastic.filter.Operator;

public class SplitterFactory {

    public static BaseSplitter createSplitter(Operator operator, String filterValue) {
        BaseSplitter splitter;
        switch (operator) {
            case MULTIPLE_OPTIONS:
                splitter = new MultipleOptionsSplitter();
                break;
            case DIAMOND_EQUALS:
                splitter = new DiamondEqualsSplitter();
                break;
            case EQUALS:
                splitter = new EqualsSplitter();
                break;
            case LIKE:
                splitter = new LikeSplitter();
                break;
            case GREATER_THAN_EQUALS:
                splitter = new GreaterThanEqualsSplitter();
                break;
            case LESS_THAN_EQUALS:
                splitter = new LessThanEqualsSplitter();
                break;
            case STR_DATE:
                splitter = new StrDateSplitter();
                break;
            case DATE:
                splitter = new DateSplitter();
                break;
            case DATE_GREATER:
                splitter = new DateGreaterSplitter();
                break;
            case DATE_LESS:
                splitter = new DateLowerSplitter();
                break;
            case IS_NOT_NULL:
                splitter = new IsNotNullSplitter();
                break;
            case JSON_EXTRACT:
                Operator decoratedOperator = Operator.extract(filterValue.replace(Filter.JSON_EXTRACT, StringUtils.EMPTY));
//                if (Operator.IS_NOT_NULL.compareTo(decoratedOperator) != 0) {
//                    splitter = new JsonExtractSplitter();
//                    break;
//                }
                BaseSplitter decoratedSplitter = createSplitter(decoratedOperator, filterValue);
                splitter = new JsonExtractSplitter(decoratedSplitter);
                break;
            default:
                throw new IllegalArgumentException(Operator.UNKNOWN_OPERATOR);
        }
        return splitter;
    }
}
