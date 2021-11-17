package org.broadinstitute.dsm.model.elastic;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.broadinstitute.dsm.db.OncHistoryDetail;
import org.broadinstitute.dsm.db.Participant;
import org.broadinstitute.dsm.db.Tissue;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDataDto;
import org.junit.Test;

public class UtilTest {

    @Test
    public void underscoresToCamelCase() {
        String fieldName = "column_name";
        String fieldName2 = "COLUMN_NAME";
        String fieldName3 = "column";
        String fieldName4 = "COLUMN";
        String fieldName5 = "columnName";
        String transformed = Util.underscoresToCamelCase(fieldName);
        String transformed2 = Util.underscoresToCamelCase(fieldName2);
        String transformed3 = Util.underscoresToCamelCase(fieldName3);
        String transformed4 = Util.underscoresToCamelCase(fieldName4);
        String transformed5 = Util.underscoresToCamelCase(fieldName5);
        assertEquals("columnName", transformed);
        assertEquals("columnName", transformed2);
        assertEquals("column", transformed3);
        assertEquals("column", transformed4);
        assertEquals("columnName", transformed5);
    }


    @Test
    public void transformObjectToMap() {
        Participant participant = new Participant(
                "1", "QWERTY", "assigneeMr",
                "assigneeTissue", "instance", "2020-10-28",
                "2020-10-28", "2020-10-28", "2020-10-28",
                "ptNotes", true, true,
                "additionalValuesJson", 1934283746283L);
        Map<String, Object> transformedObject = Util.transformObjectToMap(participant);
        assertEquals("1", transformedObject.get("participantId"));
        assertEquals("QWERTY", transformedObject.get("ddpParticipantId"));
        assertEquals("2020-10-28", transformedObject.get("created"));
        assertEquals(true, transformedObject.get("minimalMr"));
        assertEquals(1934283746283L, transformedObject.get("exitDate"));
    }

    @Test
    public void transformJsonToMap() {
        String json = "{\"DDP_INSTANCE\": \"TEST\", \"DDP_VALUE\": \"VALUE\"}";

        ParticipantDataDto participantDataDto = new ParticipantDataDto.Builder()
                .withParticipantDataId(10)
                .withDdpParticipantId("123")
                .withDdpInstanceId(55)
                .withFieldTypeId("f")
                .withData(json)
                .build();

        Map<String, Object> result = Util.transformObjectToMap(participantDataDto);
        assertEquals("TEST", result.get("ddpInstance"));
        assertEquals("VALUE", result.get("ddpValue"));
    }

    @Test
    public void convertToMap() {
        String fieldName = "tissue";
        List<Object> fieldValue = List.of(new Tissue("11", "22",
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null));

        Map<String, Object> stringObjectMap = Util.convertToMap(fieldName, fieldValue);

        System.out.println();
        System.out.println();
    }

    @Test
    public void getParameterizedType() throws NoSuchFieldException {
        class MockClass {
            List<Object> listField;
        }
        Field listField = MockClass.class.getDeclaredField("listField");
        Class<?> clazz = Util.getParameterizedType(listField.getGenericType());
        assertEquals(Object.class, clazz);
    }
}