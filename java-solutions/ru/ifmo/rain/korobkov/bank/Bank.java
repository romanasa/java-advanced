package ru.ifmo.rain.korobkov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

public interface Bank extends Remote {
    Person getLocalPerson(String passport) throws RemoteException;

    Person getRemotePerson(String passport) throws RemoteException;

    Person createPerson(String firstName, String lastName, String password) throws RemoteException;

    ConcurrentHashMap<String, Account> getAccounts(String passport) throws RemoteException;

    Account getAccount(String passport, String subId) throws RemoteException;

    Account createAccount(final String passport, final String subId) throws RemoteException;
}
