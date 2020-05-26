package ru.ifmo.rain.korobkov.bank;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalPerson extends AbstractPerson {
    private final Map<String, Account> accounts = new HashMap<>();

    public LocalPerson(final Person person) throws RemoteException {
        super(person.getFirstName(), person.getLastName(), person.getPassport());
        for (final Map.Entry<String, Account> entry : person.getAccounts().entrySet()) {
            this.accounts.put(entry.getKey(), new RemoteAccount(entry.getValue()));
        }
    }

    // :NOTE: Убрать
    public ConcurrentHashMap<String, Account> getAccounts() {
        return new ConcurrentHashMap<>(accounts);
    }

    public Account getAccount(final String subId)   {
        return accounts.get(subId);
    }

}
