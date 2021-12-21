package org.broadinstitute.dsm.model.elastic.filter;

public class SplitterFactory {

    public static BaseSplitter createSplitter(Operator operator) {
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
                splitter = new JsonExtractSplitter();
                break;
            default:
                throw new IllegalArgumentException("Unknown operator");
        }
        return splitter;
    }
}
