package ru.ifmo.rain.korobkov.bank;

import java.io.Serializable;

public abstract class AbstractPerson implements Serializable {
    protected String firstName;
    protected String lastName;
    protected String passport;

    public AbstractPerson(final String firstName, final String lastName, final String passport) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.passport = passport;
    }

    private void get(final String field) {
        System.out.println(String.format("Getting %s for Person with passport = %s", field, passport));
    }

    public String getFirstName() {
        get("first name");
        return firstName;
    }

    public String getLastName() {
        get("last name");
        return lastName;
    }

    public String getPassport() {
        get("passport");
        return passport;
    }
}
