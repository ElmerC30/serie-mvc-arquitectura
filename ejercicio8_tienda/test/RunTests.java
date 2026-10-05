import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

import java.io.PrintWriter;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/** Lanzador de JUnit 5 sin Maven ni Gradle: ejecuta las pruebas de modelo, persistencia y controlador. */
public class RunTests {
    public static void main(String[] args) {
        LauncherDiscoveryRequestBuilder solicitud = LauncherDiscoveryRequestBuilder.request();
        for (String paquete : new String[] {"modelo", "persistencia", "controlador"}) {
            solicitud.selectors(selectPackage(paquete));
        }
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener resumen = new SummaryGeneratingListener();
        TestExecutionListener detalle = new TestExecutionListener() {
            @Override
            public void executionFinished(TestIdentifier id, TestExecutionResult r) {
                if (id.isTest()) {
                    boolean ok = r.getStatus() == TestExecutionResult.Status.SUCCESSFUL;
                    System.out.println((ok ? "[OK]    " : "[FALLA] ") + id.getDisplayName());
                    r.getThrowable().ifPresent(t -> System.out.println("        " + t));
                }
            }
        };
        launcher.execute(solicitud.build(), resumen, detalle);
        TestExecutionSummary s = resumen.getSummary();
        System.out.printf("%nPruebas: %d | exitosas: %d | fallidas: %d%n",
                s.getTestsFoundCount(), s.getTestsSucceededCount(), s.getTestsFailedCount());
        s.printFailuresTo(new PrintWriter(System.out), 10);
        System.exit(s.getTotalFailureCount() == 0 ? 0 : 1);
    }
}
