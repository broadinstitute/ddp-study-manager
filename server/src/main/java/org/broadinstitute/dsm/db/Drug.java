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
    private String brandName = null;
    private String chemocat;
    private String chemoType;
    private int studyDrug;
    private String treatmentType;
    private String chemotherapy;
    private long dateCreated; // note: Postgres bigint (in database) is the same as java long
    private int active;
    private Long dateUpdated = null; // regular long can't be null


    public Drug(int drugId, String displayName, String genericName, String brandName, String chemocat, String chemoType,
                int studyDrug, String treatmentType, String chemotherapy, long dateCreated, int active, Long dateUpdated) {
        this.drugId = drugId;
        this.displayName = displayName;
        this.genericName = genericName;
        this.brandName = brandName;
        this.chemocat = chemocat;
        this.chemoType = chemoType;
        this.studyDrug = studyDrug;
        this.treatmentType = treatmentType;
        this.chemotherapy = chemotherapy;
        this.dateCreated = dateCreated;
        this.active = active;
        this.dateUpdated = dateUpdated;
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
                            long thisDateCreated= (rs.getInt(DBConstants.DATE_CREATED));
                            int thisActive= rs.getInt(DBConstants.ACTIVE);
                            long thisDateUpdated= (rs.getInt(DBConstants.DATE_UPDATED));

                            Drug thisDrug = new Drug(thisDrugId,thisDisplayName,thisGenericName,thisBrandName, thisChemocat,
                                    thisChemoType, thisStudyDrug, thisTreatmentType, thisChemotherapy, thisDateCreated, thisActive, thisDateUpdated);

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

    // loop through a bunch of changed drugs and call updateDrugListing for each one
    public static void updateDrugListings(@NonNull Drug[] updatedDrugs) {

        // We'll attempt to update each changed value one at a time
        // @TODO: placeholder for now, will need to come back and add validation, error handling, etc
        for (Drug updatedDrug : updatedDrugs) {
            SimpleResult dbVals = new SimpleResult();
            updateDrugListing(updatedDrug);
        }
    }

    // For one updated drug listing, update its values if valid
    // @TODO: placeholder for now, will need to come back and add validation, error handling, etc
    public static void updateDrugListing(@NonNull Drug updatedDrugValues) {
            Long nowValue = System.currentTimeMillis()/1000; //(divide by 1000 to match epoch)

            SimpleResult results = inTransaction((conn) -> {
                SimpleResult dbVals = new SimpleResult();
                try (PreparedStatement stmt = conn.prepareStatement(TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.UPDATE_DRUG_LISTING))) {
                    stmt.setString(1, updatedDrugValues.getDisplayName());
                    stmt.setString(2, updatedDrugValues.getGenericName());
                    stmt.setString(3, updatedDrugValues.getBrandName());
                    stmt.setString(4, updatedDrugValues.getChemocat());
                    stmt.setString(5, updatedDrugValues.getChemoType());
                    stmt.setInt(6, updatedDrugValues.getStudyDrug());
                    stmt.setString(7, updatedDrugValues.getTreatmentType());
                    stmt.setString(8, updatedDrugValues.getChemotherapy());
                    stmt.setLong(9, updatedDrugValues.getDateCreated());
                    stmt.setLong(10, updatedDrugValues.getActive());
                    stmt.setLong(11, nowValue);
                    stmt.setInt(12, updatedDrugValues.getDrugId());

                    int result = stmt.executeUpdate();
                    if (result == 1) {
                        logger.info("Updated drug entry w/ id " + updatedDrugValues.getDrugId());
                    } else {
                        throw new RuntimeException("Error updating drug entry w/ id " + updatedDrugValues.getDrugId() + " it was updating " + result + " rows");
                    }
                } catch (SQLException ex) {
                    dbVals.resultException = ex;
                }
                return dbVals;
            });

            if (results.resultException != null) {
                throw new RuntimeException("Error saving drug Listing w/ id " + updatedDrugValues.getDrugId(), results.resultException);
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
