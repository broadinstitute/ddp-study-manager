package org.broadinstitute.dsm.db;

import lombok.Data;
import lombok.NonNull;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.ddp.db.TransactionWrapper;
import org.broadinstitute.dsm.statics.ApplicationConfigConstants;
import org.broadinstitute.dsm.statics.DBConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;
import java.util.ArrayList;
import java.util.List;

// Initially used for retrieving display name values only, then expanded to pull/view/edit full database entries

@Data
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

    // Display names only (original method to show in MBC followup survey)
    public static List<String> getDrugList() {
        List<String> drugList = new ArrayList<>();

        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();

            try (PreparedStatement stmt = conn.prepareStatement(TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.GET_DRUG_LIST))) {
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
    public static List<Drug> getDrugListings() {
        List<Drug> drugList = new ArrayList<>();

        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.GET_DRUG_DATA))) {
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
            throw new RuntimeException("Error getting druglist entries ", results.resultException);
        }
        logger.info("Drug list has " + drugList.size() + " drugs ");

        return drugList;
    }

    //


    public static void updateDrugListing(@NonNull Drug[] updatedDrugValues) {

        // We'll attempt to update each changed value one at a time
        for (Drug updatedDrug : updatedDrugValues) {
            SimpleResult results = inTransaction((conn) -> {
                SimpleResult dbVals = new SimpleResult();
                try (PreparedStatement stmt = conn.prepareStatement(TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.UPDATE_DRUGLIST_ENTRY))) {
                    stmt.setInt(1, updatedDrug.getDrugId());
                    stmt.setString(2, updatedDrug.getDisplayName());
                    stmt.setString(3, updatedDrug.getGenericName());
                    stmt.setString(4, updatedDrug.getBrandName());
                    stmt.setString(5, updatedDrug.getChemocat());
                    stmt.setString(6, updatedDrug.getChemoType());
                    stmt.setInt(7, updatedDrug.getStudyDrug());
                    stmt.setString(8, updatedDrug.getTreatmentType());
                    stmt.setString(9, updatedDrug.getChemotherapy());
                    stmt.setLong(10, updatedDrug.getDateAdded());
                    stmt.setLong(11, updatedDrug.getActive());
                    int result = stmt.executeUpdate();
                    if (result == 1) {
                        logger.info("Updated drug entry w/ id " + updatedDrug.getDrugId());
                    } else {
                        throw new RuntimeException("Error updating drug entry w/ id " + updatedDrug.getDrugId() + " it was updating " + result + " rows");
                    }
                } catch (SQLException ex) {
                    dbVals.resultException = ex;
                }
                return dbVals;
            });

            if (results.resultException != null) {
                throw new RuntimeException("Error saving labelSetting w/ id " + updatedDrug.getDrugId(), results.resultException);
            }
        }



    }

    // @TODO: will add this once we get to csv upload
    // May want one method to loop through and keep track, and one to do the individual saving for each valid row
    public static void addDrugListing(@NonNull Drug[] importedDrugs) {
//        for (Drug importedDrug : importedDrugs) {
////            if (StringUtils.isNotBlank(importedDrug.drugId)) {
//            if (StringUtils.isNotBlank(importedDrug.drugId)) {
//                int drugEntryId = importedDrug.drugId();
//                if (StringUtils.isNotBlank(drugEntryId)) {
//                    updateDruglistEntry(drugEntryId, importedDrug);
//                }
//                else {
//                    // check for duplicate display name first, then
//                    //addDruglistEntry(importedDrug);
//                }
//            }
//        }
    }
}
