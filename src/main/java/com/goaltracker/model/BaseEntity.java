package com.goaltracker.model;

/** Abstract parent of all entities (abstraction + inheritance + polymorphism via describe()). */
public abstract class BaseEntity {
    protected int id;
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public abstract String describe();
}
