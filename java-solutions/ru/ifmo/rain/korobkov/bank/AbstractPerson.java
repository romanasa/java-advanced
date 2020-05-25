package ru.ifmo.rain.korobkov.bank;

import java.io.Serializable;
import java.rmi.RemoteException;

public abstract class AbstractPerson implements Person, Serializable {
    private final String firstName;
    private final String lastName;
    private final String passport;

    public AbstractPerson(final String firstName, final String lastName, final String passport) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.passport = passport;
    }

    @Override
    public String getFirstName() {
        System.out.println("Getting first name for person with passport = " + passport);
        return firstName;
    }

    @Override
    public String getLastName() {
        System.out.println("Getting last name for person with passport = " + passport);
        return lastName;
    }

    @Override
    public String getPassport() {
        return passport;
    }

}
