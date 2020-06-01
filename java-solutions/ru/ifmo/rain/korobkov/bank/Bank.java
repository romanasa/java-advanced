package ru.ifmo.rain.korobkov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {
    LocalPerson getLocalPerson(String passport) throws RemoteException;

    Person getRemotePerson(String passport) throws RemoteException;

    Person createPerson(String firstName, String lastName, String password) throws RemoteException;

    Account createAccount(final String passport, final String subId) throws RemoteException;
}
