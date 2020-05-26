package ru.ifmo.rain.korobkov.bank;
import org.junit.jupiter.api.*;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Bank Test")
public class BankTests {

    private static Registry registry;
    private static final int port = 8888;
    private static final String name = String.format("//localhost:%d/bank", port);

    private static final String[][] names = {
            {"Ivan", "Ivanov", "123456"},
            {"Petr", "Petrov", "7514123456"},
            {"Petr", "Ivanov", "654321"},
    };

    private static final Map<String, List<String>> accounts = Map.of(
            "123456", Arrays.asList("1", "2", "3"),
            "7514123456", Arrays.asList("first", "second", "main"),
            "654321", Arrays.asList("A", "B", "C")
    );

    @BeforeAll
    public static void beforeAll() throws RemoteException {
        System.out.println("Starting registry");
        try {
            LocateRegistry.createRegistry(port);
        } catch (final ExportException ignored) {
            System.out.println("Registry has already started");
        }
        registry = LocateRegistry.getRegistry(port);
    }

    @BeforeEach
    public void beforeEach() throws RemoteException {
        final Bank bank = new RemoteBank(port);
        UnicastRemoteObject.exportObject(bank, port);
        registry.rebind(name, bank);
        System.out.println("Bank rebinded");
    }

    @AfterEach
    public void afterEach() throws RemoteException {
        try {
            registry.unbind(name);
            System.out.println("Bank unbinded");
        } catch (final NotBoundException e) {
            System.out.println("Failed to unbind: " + name);
            e.printStackTrace();
        }
    }

    private Bank getBank() throws RemoteException {
        final Bank bank;
        try {
            bank = (Bank) registry.lookup(name);
        } catch (final NotBoundException e) {
            throw new RemoteException("Bank is not found");
        }
        return bank;
    }

    private void checkPerson(final Person person, final String firstName, final String lastName,
                            final String passport) throws RemoteException {
        assertNotNull(person);
        assertEquals(firstName, person.getFirstName());
        assertEquals(lastName, person.getLastName());
        assertEquals(passport, person.getPassport());
    }

    private void checkLocalPerson(final Bank bank, final String firstName, final String lastName,
                                 final String passport) throws RemoteException {
        final Person person = bank.getLocalPerson(passport);
        checkPerson(person, firstName, lastName, passport);
    }

    private void checkRemotePerson(final Bank bank, final String firstName, final String lastName,
                                 final String passport) throws RemoteException {
        final Person person = bank.getRemotePerson(passport);
        checkPerson(person, firstName, lastName, passport);
    }

    private void createPersons(final Bank bank) throws RemoteException {
        for (final String[] data : names) {
            final Person person = bank.createPerson(data[0], data[1], data[2]);
            checkPerson(person, data[0], data[1], data[2]);
        }
    }

    @Test
    @DisplayName("createPerson")
    public void createPerson() throws RemoteException {
        final Bank bank = getBank();
        createPersons(bank);
    }

    @Test
    @DisplayName("getRemotePerson")
    public void getRemotePerson() throws RemoteException {
        final Bank bank = getBank();
        createPersons(bank);
        for (final String[] data: names) {
            checkRemotePerson(bank, data[0], data[1], data[2]);
        }
    }

    @Test
    @DisplayName("getLocalPerson")
    public void getLocalPerson() throws RemoteException {
        final Bank bank = getBank();
        createPersons(bank);
        for (final String[] data: names) {
            checkLocalPerson(bank, data[0], data[1], data[2]);
        }
    }

    private void createAccounts(final Bank bank) throws RemoteException {
        for (final Map.Entry<String, List<String>> person : accounts.entrySet()) {
            for (final String accountName : person.getValue()) {
                assertNotNull(bank.createAccount(person.getKey(), accountName));
            }
        }
    }

    @Test
    @DisplayName("getAccounts")
    public void getAccounts() throws RemoteException {
        final Bank bank = getBank();
        createPersons(bank);
        createAccounts(bank);

        for (final Map.Entry<String, List<String>> data : accounts.entrySet()) {
            final Person person = bank.getRemotePerson(data.getKey());
            for (final String accountName : data.getValue()) {
                assertNotNull(person.getAccount(accountName));
            }
        }
    }

    private void checkAmount(final Bank bank, final String passport, final String subId, final int expected) throws RemoteException {
        final Person person = bank.getRemotePerson(passport);
        assertNotNull(person);
        final Account account = person.getAccount(subId);
        assertNotNull(account);
        assertEquals(expected, account.getAmount());
    }

    private void createAmounts(final Bank bank) throws RemoteException {
        int s = 0;
        for (final Map.Entry<String, List<String>> data : accounts.entrySet()) {
            final Person person = bank.getRemotePerson(data.getKey());
            int i = 0;
            for (final String accountName : data.getValue()) {
                person.getAccount(accountName).setAmount(s * i);
                i++;
            }
            s++;
        }
    }

    @Test
    @DisplayName("setAmount")
    public void setAmount() throws RemoteException {
        final Bank bank = getBank();

        createPersons(bank);
        createAccounts(bank);
        createAmounts(bank);

        int s = 0;
        for (final Map.Entry<String, List<String>> data : accounts.entrySet()) {
            final Person person = bank.getRemotePerson(data.getKey());
            int i = 0;
            for (final String accountName : data.getValue()) {
                checkAmount(bank, data.getKey(), accountName, s * i);
                i++;
            }
            s++;
        }
    }


    @Test
    @DisplayName("remoteChanges")
    public void remoteChanges() throws RemoteException {
        final Bank bank = getBank();
        createPersons(bank);
        createAccounts(bank);

        for (final String[] data: names) {
            final Person person = bank.getRemotePerson(data[2]);
            final Map<String, Account> accounts = person.getAccounts();
            for (final Map.Entry<String, Account> item : accounts.entrySet()) {
                item.getValue().setAmount(200);
            }
        }

        for (final String[] data: names) {
            final Person person = bank.getRemotePerson(data[2]);
            final Map<String, Account> accounts = person.getAccounts();
            for (final Map.Entry<String, Account> item : accounts.entrySet()) {
                assertEquals(200, person.getAccount(item.getKey()).getAmount());
            }
        }
    }

    @Test
    @DisplayName("localChanges")
    public void localChanges() throws RemoteException {
        final Bank bank = getBank();
        createPersons(bank);
        createAccounts(bank);

        for (final String[] data: names) {
            final Person person = bank.getRemotePerson(data[2]);
            final Map<String, Account> accounts = person.getAccounts();
            for (final Map.Entry<String, Account> item : accounts.entrySet()) {
                item.getValue().setAmount(100);
            }
        }

        final List<Person> locals = new ArrayList<>();
        for (final String[] data: names) {
            final Person local = bank.getLocalPerson(data[2]);
            final Map<String, Account> accounts = local.getAccounts();
            for (final Map.Entry<String, Account> item : accounts.entrySet()) {
                item.getValue().setAmount(200);
            }
            locals.add(local);
        }

        for (final Person local : locals) {
            final Map<String, Account> accounts = local.getAccounts();
            final Person remote = bank.getRemotePerson(local.getPassport());
            for (final Map.Entry<String, Account> item : accounts.entrySet()) {
                assertEquals(200, local.getAccount(item.getKey()).getAmount());
                assertEquals(100, remote.getAccount(item.getKey()).getAmount());
            }
        }

        for (final String[] data: names) {
            final Map<String, Account> accounts = bank.getRemotePerson(data[2]).getAccounts();
            for (final Map.Entry<String, Account> item : accounts.entrySet()) {
                item.getValue().setAmount(300);
            }
        }

        for (final Person local : locals) {
            final Person remote = bank.getRemotePerson(local.getPassport());
            final Map<String, Account> accounts = local.getAccounts();
            for (final Map.Entry<String, Account> item : accounts.entrySet()) {
                assertEquals(200, item.getValue().getAmount());
                assertEquals(300, remote.getAccount(item.getKey()).getAmount());
            }
        }
    }


    @Test
    @DisplayName("multiplePersons")
    public void multiplePersons() throws RemoteException {
        final Bank bank = getBank();
        createPersons(bank);

        for (final String[] data : names) {
            final Person old = bank.getRemotePerson(data[2]);
            final Person person = bank.createPerson(data[0], data[1], data[2]);
            assertEquals(old, person);
        }
    }


    @Test
    @DisplayName("multipleAccounts")
    public void multipleAccounts() throws RemoteException {
        final Bank bank = getBank();
        createPersons(bank);
        createAccounts(bank);

        for (final Map.Entry<String, List<String>> person : accounts.entrySet()) {
            for (final String accountName : person.getValue()) {
                final Account old = bank.getRemotePerson(person.getKey()).getAccount(accountName);
                final Account account = bank.createAccount(person.getKey(), accountName);
                assertEquals(old, account);
            }
        }
    }
}