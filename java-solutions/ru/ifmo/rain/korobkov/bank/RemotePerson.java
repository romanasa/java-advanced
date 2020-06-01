package ru.ifmo.rain.korobkov.bank;

import java.util.Map;

public class RemotePerson extends AbstractPerson implements Person {
    private final RemoteBank bank;

    public RemotePerson(final String firstName, final String lastName, final String passport,
                        final RemoteBank bank) {
        super(firstName, lastName, passport);
        this.bank = bank;
    }

    public Map<String, Account> getAccounts() {
        return bank.getAccounts(getPassport());
    }

    public Account getAccount(final String subId) {
        return bank.getAccount(getPassport(), subId);
    }
}
