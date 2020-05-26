package ru.ifmo.rain.korobkov.bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentMap<String, Person> remotePersons = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<Person, ConcurrentHashMap<String, Account>> accountMaps = new ConcurrentHashMap<>();

    public RemoteBank(final int port) {
        this.port = port;
    }

    private<T> T noSuch(final String object, final String field, final String value) {
        System.out.println(String.format("No %s with %s = %s", object, field, value));
        return null;
    }

    private<T> T noSuchPerson(final String passport) {
        return noSuch("Person", "passport", passport);
    }

    private void get(final String object, final String field, final String value) {
        System.out.println(String.format("Getting %s by %s = %s", object, field, value));
    }

    private void create(final String object, final String field, final String value) {
        System.out.println(String.format("Creating %s by %s = %s", object, field, value));
    }

    public Person getLocalPerson(final String passport) throws RemoteException {
        get("LocalPerson", "passport", passport);
        final Person person = remotePersons.get(passport);
        return person != null ? new LocalPerson(person) : noSuchPerson(passport);
    }

    public Person getRemotePerson(final String passport) {
        get("RemotePerson", "passport", passport);
        final Person person = remotePersons.get(passport);
        return person != null ? person : noSuchPerson(passport);
    }

    private<T> T checkResult(final T result, final RemoteException exception) throws RemoteException {
        if (result == null) {
            throw exception;
        }
        return result;
    }

    public Person createPerson(final String firstName, final String lastName,
                               final String passport) throws RemoteException {
        final RemoteException exception = new RemoteException("Can't export person with " +
                                                            "passport = " + passport);
        final Person result = remotePersons.computeIfAbsent(passport, ignored -> {
            create("Person", "passport", passport);
            final RemotePerson person = new RemotePerson(firstName, lastName, passport, this);
            try {
                UnicastRemoteObject.exportObject(person, port);
            } catch (final RemoteException e) {
                exception.addSuppressed(e);
                return null;
            }
            accountMaps.put(person, new ConcurrentHashMap<>());
            return person;
        });
        return checkResult(result, exception);
    }

    private String getId(final String passport, final String subId) {
        return passport + ":" + subId;
    }

    public Account createAccount(final String passport, final String subId) throws RemoteException {
        final String id = getId(passport, subId);
        final Person person = getRemotePerson(passport);
        if (person == null) {
            System.out.print("Can't create account: ");
            return noSuchPerson(passport);
        }

        final RemoteException exception = new RemoteException("Can't export account with " +
                "id = " + id);
        final Account result = accounts.computeIfAbsent(id, ignored -> {
            create("Account", "id", id);
            final Account account = new RemoteAccount(id);
            try {
                UnicastRemoteObject.exportObject(account, port);
            } catch (final RemoteException e) {
                exception.addSuppressed(e);
                return null;
            }
            accountMaps.get(person).put(subId, account);
            return account;
        });
        return checkResult(result, exception);
    }

    public ConcurrentHashMap<String, Account> getAccounts(final String passport) {
        get("Accounts", "passport", passport);
        final Person person = remotePersons.get(passport);
        return person != null ? accountMaps.get(person) : noSuchPerson(passport);
    }

    public Account getAccount(final String passport, final String subId) {
        return getAccount(getId(passport, subId));
    }

    public Account getAccount(final String id) {
        get("Account", "id", id);
        final Account account = accounts.get(id);
        return account != null ? account : noSuch("Account", "id", id);
    }
}
