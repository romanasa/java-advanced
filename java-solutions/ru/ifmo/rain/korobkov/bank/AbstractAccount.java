package ru.ifmo.rain.korobkov.bank;

import java.io.Serializable;

public abstract class AbstractAccount implements Serializable {
    private final String id;
    private volatile int amount;

    public AbstractAccount(final String id) {
        this.id = id;
        amount = 0;
    }

    private void log(final String action, final String field) {
        System.out.println(String.format("%s %s for RemoteAccount with id = %s",
                action, field, id));
    }

    public String getId() {
        log("Getting", "id");
        return id;
    }

    public int getAmount() {
        log("Getting", "amount of money");
        return amount;
    }

    public void setAmount(final int amount) {
        log("Setting", "amount of money");
        this.amount = amount;
    }
}
