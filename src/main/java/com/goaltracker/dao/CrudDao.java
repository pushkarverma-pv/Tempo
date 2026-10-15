package com.goaltracker.dao;

import com.goaltracker.exception.AppException;
import java.util.List;

/** Contract implemented by every DAO (interface + generics). */
public interface CrudDao<T> {
    int create(T item) throws AppException;
    T findById(int id) throws AppException;
    List<T> findAll() throws AppException;
    boolean delete(int id) throws AppException;
}
