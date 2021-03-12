package org.broadinstitute.dsm.db.dao;

public interface Dao<T> {

    int create(T t);

    int delete(int id);
}
