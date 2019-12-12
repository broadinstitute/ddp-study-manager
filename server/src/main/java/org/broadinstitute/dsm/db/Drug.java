package org.broadinstitute.dsm.db;

import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.statics.DBConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;
import java.util.ArrayList;
import java.util.List;

// Initially we'll retrieve the display name saved in the drug_list table, may use other fields later

public class Drug {

    private static final Logger logger = LoggerFactory.getLogger(Drug.class);

    private final int drugId;
    private String displayName;
    private String genericName;
    private String brandName;
    private String chemocat;
    private String chemoType;
    private int studyDrug;
    private String treatmentType;
    private String chemotherapy;
    private long dateAdded;
    private long active;

    public Drug(int drugId, String displayName, String genericName, String brandName, String chemocat, String chemoType,
                int studyDrug, String treatmentType, String chemotherapy, long dateAdded, long active) {
        this.drugId = drugId;
        this.displayName = displayName;
        this.genericName = genericName;
        this.brandName = brandName;
        this.chemocat = chemocat;
        this.chemoType = chemoType;
        this.studyDrug = studyDrug;
        this.treatmentType = treatmentType;
        this.chemotherapy = chemotherapy;
        this.dateAdded = dateAdded;
        this.active = active;
    }


    private static final String SQL_SELECT_DRUG_LIST = "SELECT drug_id, display_name FROM drug_list ORDER BY display_name asc";
    private static final String SQL_SELECT_DRUG_DATA = "SELECT * FROM drug_list ORDER BY display_name asc";

    // Display names only (original method to show in MBC followup survey)
    public static List<String> getDrugList() {
        List<String> drugList = new ArrayList<>();

        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();

            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_DRUG_LIST)) {
                if (stmt != null) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            drugList.add(rs.getString(DBConstants.DISPLAY_NAME));
                        }
                    }
                }
                else {
                    throw new RuntimeException("Drug list is empty");
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting drug list ", results.resultException);
        }
        logger.info("Drug list has " + drugList.size() + " drugs ");

        return drugList;
    }


    /**
     * Read full drug data, for DSM list/edit
     * @return List<Drug>
     * @throws Exception
     */
    public static List<Drug> getFullDrugData() {
        List<Drug> drugList = new ArrayList<>();

        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_DRUG_DATA)) {
                if (stmt != null) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {

                            // make the drug
                            int thisDrugId = rs.getInt(DBConstants.DRUG_ID);
                            String thisDisplayName= rs.getString(DBConstants.DISPLAY_NAME);
                            String thisGenericName= rs.getString(DBConstants.GENERIC_NAME);
                            String thisBrandName= rs.getString(DBConstants.BRAND_NAME);
                            String thisChemocat= rs.getString(DBConstants.CHEMOCAT);
                            String thisChemoType= rs.getString(DBConstants.CHEMO_TYPE);
                            int thisStudyDrug= rs.getInt(DBConstants.STUDY_DRUG);
                            String thisTreatmentType= rs.getString(DBConstants.TREATMENT_TYPE);
                            String thisChemotherapy= rs.getString(DBConstants.CHEMOTHERAPY);
                            long thisDateAdded= rs.getInt(DBConstants.DATE_ADDED);
                            int thisActive= rs.getInt(DBConstants.ACTIVE);

                            Drug thisDrug = new Drug(thisDrugId,thisDisplayName,thisGenericName,thisBrandName, thisChemocat,
                                    thisChemoType, thisStudyDrug, thisTreatmentType, thisChemotherapy, thisDateAdded, thisActive);

                            drugList.add(thisDrug);
                        }
                    }
                }
                else {
                    throw new RuntimeException("Drug list is empty");
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting drug list ", results.resultException);
        }
        logger.info("Drug list has " + drugList.size() + " drugs ");

        return drugList;
    }
}
