package ru.ifmo.rain.korobkov.bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentMap<String, RemotePerson> remotePersons = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<Person, ConcurrentHashMap<String, Account>> accountMaps = new ConcurrentHashMap<>();

    public RemoteBank(final int port) {
        this.port = port;
    }

    public Person getLocalPerson(final String passport) throws RemoteException {
        final Person person = remotePersons.get(passport);
        if (person != null) {
            return new LocalPerson(person);
        }
        System.out.println("No user with passport: " + passport);
        return null;
    }

    public Person getRemotePerson(final String passport) {
        return remotePersons.get(passport);
    }

    public Person createPerson(final String firstName, final String lastName,
                               final String passport) throws RemoteException {
        final RemoteException exception = new RemoteException("Can't export person with " +
                                                            "passport = " + passport);
        final RemotePerson result = remotePersons.computeIfAbsent(passport, ignored -> {
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
        if (result == null) {
            throw exception;
        }
        return result;
    }

    private String getId(final String passport, final String subId) {
        return passport + ":" + subId;
    }

    public Account createAccount(final String passport, final String subId) throws RemoteException {
        final String id = getId(passport, subId);
        final Person person = getRemotePerson(passport);
        if (person == null) {
            System.out.println("Can't create account. No person with passport = " + passport);
            return null;
        }

        final RemoteException exception = new RemoteException("Can't export account with " +
                "id = " + id);
        final Account result = accounts.computeIfAbsent(id, ignored -> {
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
        if (result == null) {
            throw exception;
        }
        return result;
    }

    public ConcurrentHashMap<String, Account> getAccounts(final String passport) {
        final RemotePerson person = remotePersons.get(passport);
        if (person != null) {
            return accountMaps.get(person);
        }
        System.out.println("No person with passport = " + passport);
        return null;
    }

    public Account getAccount(final String passport, final String subId) {
        final Person person = remotePersons.get(passport);
        if (person == null) {
            System.out.println("No person with passport = " + passport);
            return null;
        }
        final Account account = accountMaps.get(person).get(subId);
        if (account == null) {
            System.out.println(String.format("Person with passport = %s " +
                    "has no account with subId = %s", passport, subId));
            return null;
        }
        return account;
    }
}
