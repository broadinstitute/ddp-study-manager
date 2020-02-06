package org.broadinstitute.dsm.db;

import com.google.gson.Gson;
import lombok.Data;
import lombok.NonNull;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.model.Value;
import org.broadinstitute.dsm.statics.DBConstants;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

@Data
public class InstanceSettings {

    private static final String SQL_SELECT_INSTANCE_SETTINGS = "SELECT mr_cover_pdf, kit_behavior_change FROM instance_settings settings, ddp_instance realm " +
            "WHERE realm.ddp_instance_id = settings.ddp_instance_id AND realm.instance_name = ?";

    private List<Value> mrCoverPdf;
    private Object kitBehaviorChange;

    public InstanceSettings(List<Value> mrCoverPdf, Object kitBehaviorChange) {
        this.mrCoverPdf = mrCoverPdf;
        this.kitBehaviorChange = kitBehaviorChange;
    }

    public static InstanceSettings getInstanceSettings(@NonNull String realm) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_INSTANCE_SETTINGS)) {
                stmt.setString(1, realm);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        List<Value> mrCoverPdfSettings = Arrays.asList(new Gson().fromJson(rs.getString(DBConstants.MR_COVER_PDF), Value[].class));
                        Object kitBehaviorChange = new Gson().fromJson(rs.getString(DBConstants.KIT_BEHAVIOR_CHANGE), Object.class);
                        dbVals.resultValue = new InstanceSettings(mrCoverPdfSettings, kitBehaviorChange);
                    }
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting list of realms ", results.resultException);
        }
        return (InstanceSettings) results.resultValue;
    }
}
