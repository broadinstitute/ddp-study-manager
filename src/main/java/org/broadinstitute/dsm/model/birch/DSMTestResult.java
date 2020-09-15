package org.broadinstitute.dsm.model.birch;

public class DSMTestResult {
    public boolean isCorrected;
    public String result;
    public String date;

    public DSMTestResult(String result, String date, boolean isCorrected){
        this.result = result;
        this.date = date;
        this.isCorrected = isCorrected;
    }
}
