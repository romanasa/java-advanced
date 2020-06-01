package ru.ifmo.rain.korobkov.bank;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RemotePerson extends AbstractPerson implements Person {
    private final RemoteBank bank;

    public RemotePerson(final String firstName, final String lastName, final String passport,
                        final RemoteBank bank) {
        super(firstName, lastName, passport);
        this.bank = bank;
    }

    public Map<String, Account> getAccounts() {
        final ConcurrentHashMap<String, Account> accounts = bank.getAccounts(getPassport());
        return accounts == null ? null : Map.copyOf(accounts);
    }

    public Account getAccount(final String subId) {
        return bank.getAccount(getPassport(), subId);
    }
}
