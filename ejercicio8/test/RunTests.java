import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

import java.io.PrintWriter;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/** Lanzador minimo de JUnit 5 (no hay Maven/Gradle): ejecuta todas las pruebas del paquete modelo. */
public class RunTests {
    public static void main(String[] args) {
        String[] paquetes = args.length > 0 ? args : new String[] {"modelo"};
        LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request();
        for (String paquete : paquetes) {
            builder.selectors(selectPackage(paquete));
        }
        LauncherDiscoveryRequest request = builder.build();
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener resumen = new SummaryGeneratingListener();
        TestExecutionListener detalle = new TestExecutionListener() {
            @Override
            public void executionFinished(TestIdentifier id, TestExecutionResult r) {
                if (id.isTest()) {
                    String marca = r.getStatus() == TestExecutionResult.Status.SUCCESSFUL ? "[VERDE] " : "[ROJO]  ";
                    System.out.println(marca + id.getDisplayName());
                    r.getThrowable().ifPresent(t -> System.out.println("        " + t));
                }
            }
        };
        launcher.execute(request, resumen, detalle);
        TestExecutionSummary s = resumen.getSummary();
        System.out.println();
        System.out.printf("Pruebas encontradas: %d | exitosas: %d | fallidas: %d%n",
                s.getTestsFoundCount(), s.getTestsSucceededCount(), s.getTestsFailedCount());
        s.printFailuresTo(new PrintWriter(System.out), 10);
        System.out.println(s.getTotalFailureCount() == 0 ? "RESULTADO: TODO EN VERDE" : "RESULTADO: HAY FALLAS");
        System.exit(s.getTotalFailureCount() == 0 ? 0 : 1);
    }
}
