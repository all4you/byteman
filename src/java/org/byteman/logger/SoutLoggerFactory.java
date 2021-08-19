package org.byteman.logger;

/**
 * @author houyi.wh
 * @since 2021-08-04
 */
public class SoutLoggerFactory extends InternalLoggerFactory {

    public static SoutLoggerFactory INSTANCE = new SoutLoggerFactory();

    private SoutLoggerFactory() {

    }

    @Override
    protected InternalLogger newInstance(String name) {
        return new SoutLogger(name);
    }

}
