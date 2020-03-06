package org.broadinstitute.dsm.db;


import lombok.Data;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.statics.DBConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

@Data
public class Drug {

    private static final Logger logger = LoggerFactory.getLogger(Drug.class);

    private static final String SQL_SELECT_DRUGS = "SELECT display_name FROM drug_list ORDER BY display_name asc";
    private static final String SQL_SELECT_DRUGS_ALL_INFO = "SELECT drug_id, display_name, generic_name, brand_name, chemocat2, chemo_type, study_drug, " +
            "treatment_type, chemotherapy, active FROM drug_list ORDER BY display_name asc";
    private static final String SQL_UPDATE_DRUG = "UPDATE drug_list SET display_name = ?, generic_name = ? brand_name = ?, chemocat2 = ?, chemo_type = ?, " +
            "study_drug = ?, treatment_type = ?, chemotherapy = ?, active = ?, date_updated = ?, changed_by = ? WHERE drug_id = ?";
    private static final String SQL_INSERT_DRUG = "INSERT INTO drug_list SET display_name = ?, generic_name = ?, brand_name = ?, chemocat2 = ?, " +
            "chemo_type = ?, study_drug = ?, treatment_type = ?, chemotherapy = ?, date_created = ?, active = ?, changed_by = ?";

    private final int drugId;
    private String displayName;
    private String genericName;
    private String brandName;
    private String chemocat;
    private String chemoType;
    private boolean studyDrug;
    private String treatmentType;
    private String chemotherapy;
    private boolean active;


    public Drug(int drugId, String displayName, String genericName, String brandName, String chemocat, String chemoType,
                boolean studyDrug, String treatmentType, String chemotherapy, boolean active) {
        this.drugId = drugId;
        this.displayName = displayName;
        this.genericName = genericName;
        this.brandName = brandName;
        this.chemocat = chemocat;
        this.chemoType = chemoType;
        this.studyDrug = studyDrug;
        this.treatmentType = treatmentType;
        this.chemotherapy = chemotherapy;
        this.active = active;
    }

    // Display names only (original method to show in MBC followup survey)
    public static List<String> getDrugList() {
        List<String> drugList = new ArrayList<>();

        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();

            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_DRUGS)) {
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
        logger.info("Drug list has " + drugList.size() + " drugs");

        return drugList;
    }

    public static List<Drug> getDrugListALL() {
        List<Drug> drugList = new ArrayList<>();

        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_DRUGS_ALL_INFO)) {
                if (stmt != null) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            Drug drug = new Drug(rs.getInt(DBConstants.DRUG_ID),
                                    rs.getString(DBConstants.DISPLAY_NAME),
                                    rs.getString(DBConstants.GENERIC_NAME),
                                    rs.getString(DBConstants.BRAND_NAME),
                                    rs.getString(DBConstants.CHEMOCAT),
                                    rs.getString(DBConstants.CHEMO_TYPE),
                                    rs.getBoolean(DBConstants.STUDY_DRUG),
                                    rs.getString(DBConstants.TREATMENT_TYPE),
                                    rs.getString(DBConstants.CHEMOTHERAPY),
                                    rs.getBoolean(DBConstants.ACTIVE));
                            drugList.add(drug);
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
            throw new RuntimeException("Error getting drug list entries ", results.resultException);
        }
        logger.info("Drug list has " + drugList.size() + " drugs");

        return drugList;
    }

    public static void saveDrugListings(@NonNull String user, @NonNull Drug[] drugListings) {
        for (Drug drugListing : drugListings) {
            if (StringUtils.isNotBlank(drugListing.getDisplayName())) {
                if (drugListing.drugId > 0) {
                    updateDrugListing(user, drugListing);
                }
                else {
                    addDrugListing(user, drugListing);
                }
            }
        }
    }

    public static void updateDrugListing(@NonNull String user, @NonNull Drug drug) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_DRUG)) {
                stmt.setString(1, drug.getDisplayName());
                stmt.setString(2, drug.getGenericName());
                stmt.setString(3, drug.getBrandName());
                stmt.setString(4, drug.getChemocat());
                stmt.setString(5, drug.getChemoType());
                stmt.setBoolean(6, drug.isStudyDrug());
                stmt.setString(7, drug.getTreatmentType());
                stmt.setString(8, drug.getChemotherapy());
                stmt.setBoolean(9, drug.isActive());
                stmt.setLong(10, System.currentTimeMillis() / 1000);//druglist has date as epoch
                stmt.setInt(11, drug.getDrugId());
                stmt.setString(12, user);

                int result = stmt.executeUpdate();
                if (result == 1) {
                    logger.info("Updated drug entry w/ id " + drug.getDrugId());
                }
                else {
                    throw new RuntimeException("Error updating drug entry w/ id " + drug.getDrugId() + " it was updating " + result + " rows");
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error saving drug w/ id " + drug.getDrugId(), results.resultException);
        }
    }

    private static void addDrugListing(@NonNull String user, @NonNull Drug newDrugEntry) {
        Long now = System.currentTimeMillis() / 1000; //druglist has date as epoch

        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_DRUG)) {
                stmt.setString(1, newDrugEntry.getDisplayName());
                stmt.setString(2, newDrugEntry.getGenericName());
                stmt.setString(3, newDrugEntry.getBrandName());
                stmt.setString(4, newDrugEntry.getChemocat());
                stmt.setString(5, newDrugEntry.getChemoType());
                stmt.setBoolean(6, newDrugEntry.isStudyDrug());
                stmt.setString(7, newDrugEntry.getTreatmentType());
                stmt.setString(8, newDrugEntry.getChemotherapy());
                stmt.setLong(9, now);
                stmt.setBoolean(10, newDrugEntry.isActive());
                stmt.setString(11, user);
                int result = stmt.executeUpdate();
                if (result == 1) {
                    logger.info("Added new drug ");
                }
                else {
                    throw new RuntimeException("Error adding new drug, it was updating " + result + " rows");
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });
        if (results.resultException != null) {
            throw new RuntimeException("Error saving drug: " + newDrugEntry.getDisplayName(), results.resultException);
        }
    }
}
