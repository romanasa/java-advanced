package ru.ifmo.rain.korobkov.bank;
import org.junit.jupiter.api.*;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Bank Test")
public class ServerTest {

    private static Registry registry;
    private static final int port = 8888;
    private static final String name = String.format("//localhost:%d/bank", port);

    private static final String[][] NAMES = {
            {"Ivan", "Ivanov", "123456"},
            {"Petr", "Petrov", "7514123456"},
            {"Petr", "Ivanov", "654321"},
    };

    private static final Map<String, List<String>> ACCOUNTS = Map.of(
            "123456", Arrays.asList("1", "2", "3", "4", "5"),
            "7514123456", Arrays.asList("first", "second", "main"),
            "654321", Arrays.asList("A", "B", "C", "D")
    );

    public static final List<BiFunction<Integer, Integer, Integer>> FUNCTIONS = Arrays.asList(
            Integer::sum,
            Integer::min,
            Integer::max,
            (a, b) -> (a + 1) * (b + 1),
            (a, b) -> (a + b + 1) * (a + b + 1),
            (a, b) -> 100
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

    private String createId(final String passport, final String subId) {
        return passport + ":" + subId;
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

    private Account getAccount(final Bank bank, final String passport, final String subId) throws RemoteException {
        final Person person = bank.getRemotePerson(passport);
        assertNotNull(person);
        final Account account = person.getAccount(subId);
        assertNotNull(account);
        return account;
    }

    private void checkAccount(final Account account, final String subId) throws RemoteException {
        assertEquals(subId, account.getId());
        assertEquals(0, account.getAmount());
    }

    private void checkLocalPerson(final Bank bank, final String firstName, final String lastName,
                                 final String passport) throws RemoteException {
        final LocalPerson person = bank.getLocalPerson(passport);
        assertNotNull(person);
        assertEquals(firstName, person.getFirstName());
        assertEquals(lastName, person.getLastName());
        assertEquals(passport, person.getPassport());
    }

    private void checkRemotePerson(final Bank bank, final String firstName, final String lastName,
                                 final String passport) throws RemoteException {
        final Person person = bank.getRemotePerson(passport);
        checkPerson(person, firstName, lastName, passport);
    }

    private void createPersons(final Bank bank) throws RemoteException {
        for (final String[] data : NAMES) {
            final Person person = bank.createPerson(data[0], data[1], data[2]);
            checkPerson(person, data[0], data[1], data[2]);
        }
    }

    @Test
    public void createPerson() throws RemoteException {
        final Bank bank = getBank();
        createPersons(bank);
    }

    @Test
    public void getRemotePerson() throws RemoteException {
        final Bank bank = getBank();
        createPersons(bank);
        for (final String[] data: NAMES) {
            checkRemotePerson(bank, data[0], data[1], data[2]);
        }
    }

    @Test
    public void getLocalPerson() throws RemoteException {
        final Bank bank = getBank();
        createPersons(bank);
        for (final String[] data: NAMES) {
            checkLocalPerson(bank, data[0], data[1], data[2]);
        }
    }

    private void createAccounts(final Bank bank) throws RemoteException {
        for (final Map.Entry<String, List<String>> data : ACCOUNTS.entrySet()) {
            for (final String accountName : data.getValue()) {
                final Account account = bank.createAccount(data.getKey(), accountName);
                checkAccount(account, createId(data.getKey(), accountName));
            }
        }
    }

    @Test
    public void createAccounts() throws RemoteException {
        final Bank bank = getBank();
        createPersons(bank);
        createAccounts(bank);
    }

    @Test
    public void getAccounts() throws RemoteException {
        final Bank bank = getBank();
        createPersons(bank);
        createAccounts(bank);

        for (final Map.Entry<String, List<String>> data : ACCOUNTS.entrySet()) {
            final Person person = bank.getRemotePerson(data.getKey());
            for (final String accountName : data.getValue()) {
                checkAccount(person.getAccount(accountName), createId(person.getPassport(), accountName));
            }
        }
    }

    private void checkAmount(final Account account, final int expected) throws RemoteException {
        assertEquals(expected, account.getAmount());
    }

    private void setAmount(final Account account, final int amount) throws RemoteException {
        account.setAmount(amount);
    }

    private void iterateAmounts(final Bank bank, final BiFunction<Integer, Integer, Integer> f,
                                final String type) throws RemoteException {
        int s = 1;
        for (final Map.Entry<String, List<String>> data : ACCOUNTS.entrySet()) {
            for (int i = 0; i < data.getValue().size(); i++) {
                final Account account = getAccount(bank, data.getKey(), data.getValue().get(i));
                final int value = f.apply(s, i);
                if ("set".equals(type)) {
                    setAmount(account, value);
                } else {
                    checkAmount(account, value);
                }
            }
            s++;
        }
    }

    private void setAmounts(final Bank bank, final BiFunction<Integer, Integer, Integer> f) throws RemoteException {
        iterateAmounts(bank, f, "set");
    }

    private void checkAmounts(final Bank bank, final BiFunction<Integer, Integer, Integer> f) throws RemoteException {
        iterateAmounts(bank, f, "get");
    }

    @Test
    public void setAmount() throws RemoteException {
        final Bank bank = getBank();

        createPersons(bank);
        createAccounts(bank);

        for (final BiFunction<Integer, Integer, Integer> f : FUNCTIONS) {
            setAmounts(bank, f);
            checkAmounts(bank, f);
        }
    }

    private List<Person> getRemotePersons(final Bank bank) throws RemoteException {
        final List<Person> persons = new ArrayList<>();
        for (final String[] data: NAMES) {
            persons.add(bank.getRemotePerson(data[2]));
        }
        return persons;
    }

    private List<LocalPerson> getLocalPersons(final Bank bank) throws RemoteException {
        final List<LocalPerson> persons = new ArrayList<>();
        for (final String[] data: NAMES) {
            persons.add(bank.getLocalPerson(data[2]));
        }
        return persons;
    }

    private void setRemoteAccounts(final Person person, final int amount) throws RemoteException {
        assertNotNull(person);
        final Map<String, Account> accounts = person.getAccounts();
        for (final Account account : accounts.values()) {
            account.setAmount(amount);
        }
    }

    private void setLocalAccounts(final LocalPerson person, final int amount) {
        assertNotNull(person);
        final Map<String, LocalAccount> accounts = person.getAccounts();
        for (final LocalAccount account : accounts.values()) {
            account.setAmount(amount);
        }
    }

    @Test
    public void remoteChanges() throws RemoteException {
        final Bank bank = getBank();
        createPersons(bank);
        createAccounts(bank);

        setAmounts(bank, Integer::sum);

        final List<Person> remotes = getRemotePersons(bank);
        for (final Person remote : remotes) {
            setRemoteAccounts(remote, 300);
        }
        checkAmounts(bank, (a, b) -> 300);
    }

    @Test
    public void localChanges() throws RemoteException {
        final Bank bank = getBank();
        createPersons(bank);
        createAccounts(bank);

        setAmounts(bank, Integer::sum);

        final List<LocalPerson> locals = getLocalPersons(bank);
        for (final LocalPerson local : locals) {
            setLocalAccounts(local, 300);
        }
        checkAmounts(bank, Integer::sum);
    }


    @Test
    public void multiplePersons() throws RemoteException {
        final Bank bank = getBank();
        createPersons(bank);

        for (final String[] data : NAMES) {
            final Person old = bank.getRemotePerson(data[2]);
            final Person person = bank.createPerson(data[0], data[1], data[2]);
            assertEquals(old, person);
        }
    }


    @Test
    public void multipleAccounts() throws RemoteException {
        final Bank bank = getBank();
        createPersons(bank);
        createAccounts(bank);

        for (final Map.Entry<String, List<String>> person : ACCOUNTS.entrySet()) {
            for (final String accountName : person.getValue()) {
                final Account old = bank.getRemotePerson(person.getKey()).getAccount(accountName);
                final Account account = bank.createAccount(person.getKey(), accountName);
                assertEquals(old, account);
            }
        }
    }
}