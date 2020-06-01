package ru.ifmo.rain.korobkov.bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class Client {
    public static void main(final String[] args) throws RemoteException {
        final Bank bank;
        try {
            bank = (Bank) Naming.lookup("//localhost/bank");
        } catch (final NotBoundException e) {
            System.out.println("Bank is not bound");
            return;
        } catch (final MalformedURLException e) {
            System.out.println("Bank URL is invalid");
            return;
        }

        if (args.length != 5) {
            System.out.println("Usage: java Client <first name> <last name> <Passport> <subId> <amount change>");
            return;
        }

        final Person person = bank.createPerson(args[0], args[1], args[2]);
        final Account account = bank.createAccount(args[2], args[3]);

        System.out.println("Passport number: " + person.getPassport());
        System.out.println("Person: " + person.getFirstName() + " " + person.getLastName());
        System.out.println("Account id: " + account.getId());
        System.out.println("Money: " + account.getAmount());
        System.out.println("Adding money");
        account.setAmount(account.getAmount() + Integer.parseInt(args[4]));
        System.out.println("Money: " + account.getAmount());
    }
}
