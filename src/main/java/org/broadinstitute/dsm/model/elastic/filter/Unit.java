package org.broadinstitute.dsm.model.elastic.filter;

import java.util.List;
import java.util.Objects;

public class Unit {

    protected List<String> splittedWords;
    protected String valueToSplit;

    public List<String> getSplittedWords() {
        return splittedWords;
    }

    public void setSplittedWords(List<String> splittedWords) {
        this.splittedWords = splittedWords;
    }

    public String getValueToSplit() {
        return valueToSplit;
    }

    public void setValueToSplit(String valueToSplit) {
        this.valueToSplit = valueToSplit;
    }


    public Unit(String valueToSplit) {
        this.valueToSplit = Objects.requireNonNull(valueToSplit);
    }

    public void accept(SpliteratorVisitor visitor) {
        visitor.visit(this);
    }

}
