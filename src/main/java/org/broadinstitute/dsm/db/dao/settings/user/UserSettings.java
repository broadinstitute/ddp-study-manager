package org.broadinstitute.dsm.db.dao.settings.user;

import org.broadinstitute.dsm.db.dao.Dao;
import org.broadinstitute.dsm.db.dto.settings.UserSettingsDto;

public interface UserSettings extends Dao<UserSettingsDto> {

    int getRowsOnPageById(int userId);

}
