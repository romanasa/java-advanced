package ru.ifmo.rain.korobkov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

public class RemotePerson extends AbstractPerson {
    private final Bank bank;

    public RemotePerson(final String firstName, final String lastName, final String passport,
                        final Bank bank) {
        super(firstName, lastName, passport);
        this.bank = bank;
    }

    public ConcurrentHashMap<String, Account> getAccounts() throws RemoteException {
        return bank.getAccounts(getPassport());
    }

    public Account getAccount(final String subId) throws RemoteException {
        return bank.getAccount(getPassport(), subId);
    }
}
