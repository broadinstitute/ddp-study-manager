package org.broadinstitute.dsm.model.elastic.filter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class AndOrFilterSeparatorTest {

    @Test
    public void parseFiltersByLogicalOperators() {

        String filter = "AND m.medicalRecordId = '15' " +
                "OR m.medicalRecordSomething LIKE '55555' " +
                "OR m.medicalRecordSomethingg = '55552' " +
                "AND t.tissueRecord IS NOT NULL " +
                "AND m.dynamicFields.ragac = '55' " +
                "AND ( t.tissue = 'review' OR t.tissue = 'no' OR t.tissue = 'bla' ) " +
                "OR m.medicalRecordName = '213' " +
                "OR m.mrNotes = 'MEDICAL_RECORD_NOTESS' " +
                "AND m.medicalMedical = 'something AND something' " +
                "AND ( oD.request = 'review' OR oD.request = 'no' OR oD.request = 'bla' ) " +
                "OR t.tissueRecord = '225' " +
                "AND JSON_EXTRACT ( m.additiona`l_values_json , '$.seeingIfBugExists' )";
        AndOrFilterSeparator andOrFilterSeparator = new AndOrFilterSeparator(filter);
        Map<String, List<String>> parsedFilters = andOrFilterSeparator.parseFiltersByLogicalOperators();
        for (Map.Entry<String, List<String>> eachFilter: parsedFilters.entrySet()) {
            if (eachFilter.getKey().equals("AND")) {
                Assert.assertArrayEquals(new ArrayList<>(List.of("m.medicalRecordId = '15'",
                                "t.tissueRecord IS NOT NULL" ,"m.dynamicFields.ragac = '55'",
                                        "( t.tissue = 'review' OR t.tissue = 'no' OR t.tissue = 'bla' )",
                                        "m.medicalMedical = 'something AND something'", "( oD.request = 'review' OR oD.request = 'no' OR " +
                                        "oD.request = 'bla' )",
                                "JSON_EXTRACT ( m.additiona`l_values_json , '$.seeingIfBugExists' )")).toArray(),
                        eachFilter.getValue().toArray());
            } else {
                Assert.assertArrayEquals(new ArrayList<>(List.of("m.medicalRecordSomething LIKE '55555'", "m.medicalRecordSomethingg = " +
                                "'55552'", "m.medicalRecordName = '213'", "m.mrNotes = 'MEDICAL_RECORD_NOTESS'", "t.tissueRecord = '225'")).toArray(),
                        eachFilter.getValue().toArray());
            }
        }
    }

    @Test
    public void parseFiltersByLogicalOperatorsSingle() {

        String filter = "AND oD.datePx = '15'";
        Map<String, List<String>> stringListMap = new AndOrFilterSeparator(filter).parseFiltersByLogicalOperators();
        Assert.assertEquals("oD.datePx = '15'", stringListMap.get("AND").get(0));


    }
}