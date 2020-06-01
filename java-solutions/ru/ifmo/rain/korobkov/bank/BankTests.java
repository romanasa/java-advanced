package ru.ifmo.rain.korobkov.bank;


import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class BankTests {
    SummaryGeneratingListener listener = new SummaryGeneratingListener();

    public void runOne() {
        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(ru.ifmo.rain.korobkov.bank.ServerTest.class))
                .build();
        final Launcher launcher = LauncherFactory.create();
        final TestPlan testPlan = launcher.discover(request);
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);
    }

    public static void main(final String[] args) {
        final BankTests runner = new BankTests();
        runner.runOne();
        final TestExecutionSummary summary = runner.listener.getSummary();
        summary.printTo(new PrintWriter(System.out));

        System.exit(summary.getTestsFailedCount() > 0 ? 1 : 0);
    }
}
