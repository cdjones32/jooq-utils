package co.cdjones.jooq.listeners;

import org.jooq.ExecuteContext;
import org.jooq.impl.DefaultExecuteListener;
import org.jooq.tools.StopWatch;

/**
 *
 */
public abstract class JooqSlowQueryListener extends DefaultExecuteListener {

    public static final String WATCH_DATA_VARIABLE = "__watch_execute_listener__";

    protected int longQueryThresholdMillis = 1000;

    public JooqSlowQueryListener(int longQueryThresholdMillis) {
        this.longQueryThresholdMillis = longQueryThresholdMillis;
    }

    @Override
    public void start(ExecuteContext ctx) {
        super.start(ctx);
        ctx.data(WATCH_DATA_VARIABLE, new StopWatch());
    }

    @Override
    public void end(ExecuteContext ctx) {
        super.end(ctx);

        StopWatch startWatch = (StopWatch) ctx.data(WATCH_DATA_VARIABLE);

        // Convert nanoseconds to milliseconds
        long executionTimeMills = (long) (startWatch.split() / 1.0e6);

        if (executionTimeMills >= longQueryThresholdMillis) {
            onSlowQuery(ctx, executionTimeMills);
        }
    }

    /**
     * Method called when a slow query is detected
     *
     * @param ctx
     * @param executionTimeMills
     */
    abstract public void onSlowQuery( ExecuteContext ctx, long executionTimeMills );
}
