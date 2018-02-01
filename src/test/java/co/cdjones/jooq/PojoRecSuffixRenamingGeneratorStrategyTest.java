package co.cdjones.jooq;

import org.jooq.util.DefaultGeneratorStrategy;
import org.jooq.util.Definition;
import org.jooq.util.GeneratorStrategy;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PojoRecSuffixRenamingGeneratorStrategyTest {

    public static final String JAVA_CLASS_NAME = "Test";
    public static final String EXPECTED_POJO_NAME = "TestRec";

    Definition definitionMock = mock(Definition.class);

    @Before
    public void before() {
        definitionMock = mock(Definition.class);
        when(definitionMock.getOutputName()).thenReturn(JAVA_CLASS_NAME);
    }

    @Test
    public void getJavaClassName() {

        PojoRecSuffixRenamingGeneratorStrategy customStrategy = new PojoRecSuffixRenamingGeneratorStrategy();
        DefaultGeneratorStrategy defaultGeneratorStrategy = new DefaultGeneratorStrategy();

        // Test that all cases match the expected output (i.e. they are equivalent except for the POJO mode)
        for (GeneratorStrategy.Mode mode : GeneratorStrategy.Mode.values()) {
            switch (mode) {
                case POJO:
                    assertEquals(
                        "TestRec",
                        customStrategy.getJavaClassName(definitionMock, mode)
                    );
                    break;
                default:
                    assertEquals(
                        defaultGeneratorStrategy.getJavaClassName(definitionMock, mode),
                        customStrategy.getJavaClassName(definitionMock, mode)
                    );
            }
        }
    }
}