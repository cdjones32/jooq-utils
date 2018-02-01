package co.cdjones.jooq;

import org.jooq.util.DefaultGeneratorStrategy;
import org.jooq.util.Definition;

/**
 * Custom name generator that puts a 'Rec' suffix on the generated Pojos. Useful because we sometimes want both the table
 * definition and the Pojo Imported, and this prevents the Namespace clash.
 *
 * @author chrisjones
 * @date 14/09/2017
 */
public class PojoRecSuffixRenamingGeneratorStrategy extends DefaultGeneratorStrategy {

    public static final String SUFFIX = "Rec";

    @Override
    public String getJavaClassName( Definition definition, Mode mode ) {
        // If the generated item is a Pojo representation, we want to append the suffix. Otherwise use the DefaultGeneratorStrategy implementation.
        return mode == Mode.POJO ?
            super.getJavaClassName(definition, mode) + SUFFIX :
            super.getJavaClassName(definition, mode);
    }
}