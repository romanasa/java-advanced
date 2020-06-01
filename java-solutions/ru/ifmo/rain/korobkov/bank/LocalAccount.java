package ru.ifmo.rain.korobkov.bank;

import java.io.Serializable;
import java.rmi.RemoteException;

public class LocalAccount extends AbstractAccount implements Serializable {
    public LocalAccount(final String id) {
        super(id);
    }

    public LocalAccount(final Account account) throws RemoteException {
        super(account.getId());
    }
}
