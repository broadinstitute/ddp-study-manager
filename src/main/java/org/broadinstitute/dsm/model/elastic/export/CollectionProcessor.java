package org.broadinstitute.dsm.model.elastic.export;

import org.broadinstitute.dsm.model.elastic.ESDsm;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;

public class CollectionProcessor implements Processor {

    private ESDsm esDsm;
    private String propertyName;
    private GeneratorPayload generatorPayload;

    private final Predicate<Field> isFieldMatchProperty = field -> propertyName.equals(field.getName());

    public CollectionProcessor(ESDsm esDsm, String propertyName, GeneratorPayload generatorPayload) {
        this.esDsm = Objects.requireNonNull(esDsm);
        this.propertyName = Objects.requireNonNull(propertyName);
        this.generatorPayload = Objects.requireNonNull(generatorPayload);
    }

    @Override
    public List<Map<String, Object>> process() {
        // medicalRecords:
        // {
        //  id: 1
        //  name: a
        // }
        // {
        //  id: 2
        //  name: a (SSADASD)
        // }
        // {
        //  id: 3
        //  name: b
        // }
        Field[] declaredFields = esDsm.getClass().getDeclaredFields();
        Field field = Arrays.stream(declaredFields).filter(isFieldMatchProperty)
                .findFirst()
                .orElseThrow();
        List<Map<String, Object>> data = Collections.emptyList();
        try {
            data = (List<Map<String, Object>>) field.get(esDsm);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        long recordId = generatorPayload.getRecordId();


        for (Map<String, Object> datum : data) {
            //damekarge premier? me mesmis sheni me ki kai wamo aq gavtishot xma da
            if ((long) datum.get(BaseGenerator.ID) == recordId) {
                datum.put(propertyName, generatorPayload.getNameValue().getValue());
            }
        }

        return null;
    }
}
