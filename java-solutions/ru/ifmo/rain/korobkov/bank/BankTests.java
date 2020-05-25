package ru.ifmo.rain.korobkov.bank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Bank Test")
public class BankTests {

    private static Registry registry;
    private static final int port = 8888;
    private static final String name = String.format("//localhost:%d/bank", port);

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
        System.out.println("Server started");
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

    @Test
    @DisplayName("createRemotePerson")
    public void createRemotePerson() throws RemoteException {
        final Bank bank = getBank();
        final Person person = bank.createPerson("Ivan",
                "Ivanov", "123456");
        assertNotNull(person);
        assertEquals("Ivan", person.getFirstName());
        assertEquals("Ivanov", person.getLastName());
        assertEquals("123456", person.getPassport());
    }

    private void checkAmount(final Bank bank, final String passport, final String subId, final int expected) throws RemoteException {
        final Person person = bank.getRemotePerson(passport);
        assertNotNull(person);

        final Account account = person.getAccount(subId);
        assertNotNull(account);

        assertEquals(expected, account.getAmount());
    }

    @Test
    @DisplayName("setAmount")
    public void setAmount() throws RemoteException {
        final Bank bank = getBank();

        assertNotNull(bank.createPerson("Petr", "Petrov", "7514123456"));

        final Account account = bank.createAccount("7514123456", "1");
        account.setAmount(135);

        assertEquals(135, account.getAmount());
        assertEquals(135, bank.getLocalPerson("7514123456").getAccount("1").getAmount());
        assertEquals(135, bank.getRemotePerson("7514123456").getAccount("1").getAmount());
    }

    @Test
    @DisplayName("localChanges")
    public void localChanges() throws RemoteException {
        final Bank bank = getBank();

        assertNotNull(bank.createPerson("Ivan", "Ivanov", "123456"));
        assertNotNull(bank.createAccount("123456", "firstAccount"));

        final Person local = bank.getLocalPerson("123456");
        local.getAccount("firstAccount").setAmount(200);

        checkAmount(bank, "123456", "firstAccount", 0);
    }
}