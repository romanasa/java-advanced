package ru.ifmo.rain.korobkov.bank;

import java.rmi.RemoteException;

public class RemoteAccount implements Account {
    private final String id;
    private volatile int amount;

    public RemoteAccount(final String id) {
        this.id = id;
        amount = 0;
    }

    public RemoteAccount(final Account other) throws RemoteException {
        this.id = other.getId();
        this.amount = other.getAmount();
    }

    public String getId() {
        return id;
    }

    public synchronized int getAmount() {
        System.out.println("Getting amount of money for account " + id);
        return amount;
    }

    public synchronized void setAmount(final int amount) {
        System.out.println("Setting amount of money for account " + id);
        this.amount = amount;
    }
}
