package soporte;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Reloj que se puede adelantar para simular el paso de los dias en una demostracion
 * (por ejemplo, para ver una multa por retraso sin esperar una semana).
 */
public class RelojAjustable extends Clock {
    private final Clock base;
    private volatile Duration desfase = Duration.ZERO;

    public RelojAjustable(Clock base) {
        this.base = base;
    }

    public void avanzarDias(long dias) {
        desfase = desfase.plusDays(dias);
    }

    @Override public ZoneId getZone()             { return base.getZone(); }
    @Override public Clock withZone(ZoneId zone)  { return new RelojAjustable(base.withZone(zone)); }
    @Override public Instant instant()            { return base.instant().plus(desfase); }
}
