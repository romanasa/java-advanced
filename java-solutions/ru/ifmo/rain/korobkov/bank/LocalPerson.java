package ru.ifmo.rain.korobkov.bank;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

public class LocalPerson extends AbstractPerson implements Serializable {
    private final Map<String, LocalAccount> accounts = new HashMap<>();

    public LocalPerson(final Person person) throws RemoteException {
        super(person.getFirstName(), person.getLastName(), person.getPassport());
        for (final Map.Entry<String, Account> entry : person.getAccounts().entrySet()) {
            this.accounts.put(entry.getKey(), new LocalAccount(entry.getValue()));
        }
    }

    public Map<String, LocalAccount> getAccounts() {
        return Map.copyOf(accounts);
    }

    public LocalAccount getAccount(final String subId) {
        return accounts.get(subId);
    }

}
