package org.broadinstitute.dsm.db.dao.kit;

import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.db.dto.kit.ClinicalKitDto;
import org.broadinstitute.dsm.statics.DBConstants;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

public class ClinicalKitDao {
    private final static String SQL_GET_CLINICAL_KIT_BASED_ON_SM_ID_VALUE = "SELECT p.ddp_participant_id, oD.accession_number, ddp.instance_name, bsp_organism, bsp_collection," +
                                                                            " kit_type_name, bsp_material_type, bsp_receptable_type, accession_number, " +
                                                                            "from sm_id sm " +
                                                                            "left join ddp_tissue t on (t.tissue_id  = sm.tissue_id) " +
                                                                            "left join ddp_onc_history_detail oD on (oD.onc_history_detail_id = t.onc_history_detail_id) " +
                                                                            "left join ddp_medical_record mr on (mr.medical_record_id = oD.medical_record_id) " +
                                                                            "left join ddp_institution inst on  (mr.institution_id = inst.institution_id AND NOT mr.deleted <=> 1) " +
                                                                            "LEFT JOIN ddp_participant as p on (p.participant_id = inst.participant_id) " +
                                                                            "LEFT JOIN ddp_instance as ddp on (ddp.ddp_instance_id = p.ddp_instance_id)  " +
                                                                            "left join sm_id_type sit on (sit.sm_id_type_id = sm.sm_id_type_id) " +
                                                                            "left join kit_type ktype on ( sit.kit_type_id = ktype.kit_type_id) " +
                                                                            "where sm.sm_id_value = ? ";

    public Optional<ClinicalKitDto> getClinicalKitFromSMId(String smIdValue){
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_GET_CLINICAL_KIT_BASED_ON_SM_ID_VALUE)) {
                stmt.setString(1, smIdValue);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        ClinicalKitDto clinicalKitDto = new ClinicalKitDto(
                                null,
                                rs.getString(DBConstants.BSP_COLLECTION),
                                rs.getString(DBConstants.BSP_ORGANISM),
                                rs.getString(DBConstants.BSP_MATERIAL_TYPE),
                                rs.getString(DBConstants.BSP_RECEPTABLE_TYPE),
                                null,
                                null,
                                null,
                                null,
                                null,
                                rs.getString(DBConstants.ACCESSION_NUMBER),
                                null
                                );
                        clinicalKitDto.setSampleType(rs.getString(DBConstants.KIT_TYPE_NAME));
                        clinicalKitDto.setDdpInstanceId(Integer.parseInt(rs.getString(DBConstants.DDP_INSTANCE_ID)));
                        clinicalKitDto.setDdpParticipantId(rs.getString(DBConstants.DDP_PARTICIPANT_ID));
                        dbVals.resultValue = clinicalKitDto;

                    }
                }
                catch (Exception e) {
                    throw new RuntimeException("Error getting id of new sm id ", e);
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }

            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting clinicalKit based on smId " + smIdValue, results.resultException);
        }
        else {
            return Optional.ofNullable((ClinicalKitDto) results.resultValue);
        }
    }
}
