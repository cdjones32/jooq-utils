package co.cdjones.jooq.listeners;

import org.jooq.ExecuteContext;
import org.jooq.tools.StopWatch;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JooqSlowQueryListenerTest {

    ExecuteContext context = mock(ExecuteContext.class);
    StopWatch watch;

    @Before
    public void before() {
        // Return the argument (Watch)
        when(context.data(anyString(), any(StopWatch.class))).thenAnswer(it -> watch = (StopWatch)it.getArguments()[1]);
        when(context.data(anyString())).then(it -> watch);
    }

    @Test
    public void onSlowQuery() throws InterruptedException {
        CountDownLatch latch = simulateQuery(60, 50);

        assertThat(latch.getCount()).describedAs("Latch should have counted down").isZero();
    }

    @Test
    public void onFastQuery() throws InterruptedException {

        CountDownLatch latch = simulateQuery(10, 50);

        assertThat(latch.getCount()).describedAs("Latch should NOT have counted down").isNotZero();
    }

    private CountDownLatch simulateQuery( int sleepTimeMills, int slowQueryThresholdMills ) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        JooqSlowQueryListener slowQueryTest = getListenerImpl(slowQueryThresholdMills, latch);

        // Call the start method (normally handled by Jooq)
        slowQueryTest.start(context);

        // Wait for not enough time to pass
        Thread.sleep(sleepTimeMills);

        // Call the end
        slowQueryTest.end(context);
        return latch;
    }

    private JooqSlowQueryListener getListenerImpl( int waitTimeMillis, CountDownLatch latch ) {
        return new JooqSlowQueryListener(waitTimeMillis) {
            @Override
            public void onSlowQuery( ExecuteContext ctx, long executionTimeMills ) {
                latch.countDown();
            }
        };
    }
}