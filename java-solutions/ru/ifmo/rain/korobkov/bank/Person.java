package ru.ifmo.rain.korobkov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

public interface Person extends Remote {
    String getFirstName() throws RemoteException;

    String getLastName() throws RemoteException;

    String getPassport() throws RemoteException;

    ConcurrentHashMap<String, Account> getAccounts() throws RemoteException;

    Account getAccount(String subId) throws RemoteException;
}
