/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.byteman.logger;

/**
 * Creates an {@link InternalLogger} or changes the default factory
 * implementation.  This factory allows you to choose what logging framework
 * Netty should use.  The default factory is {@link Slf4JLoggerFactory}.  If SLF4J
 * is not available, {@link Log4JLoggerFactory} is used.  If Log4J is not available,
 * {@link JdkLoggerFactory} is used.  You can change it to your preferred
 * logging framework before other Netty classes are loaded:
 * <pre>
 * {@link InternalLoggerFactory}.setDefaultFactory({@link Log4JLoggerFactory}.INSTANCE);
 * </pre>
 * Please note that the new default factory is effective only for the classes
 * which were loaded after the default factory is changed.  Therefore,
 * {@link #setDefaultFactory(InternalLoggerFactory)} should be called as early
 * as possible and shouldn't be called more than once.
 *
 * 该工厂类是从Netty中拷贝过来的，主要是为了在SDK中使用logger，
 * 会根据当前项目的classpath中的具体的日志框架来创建LoggerFactory
 *
 * 首先会先尝试创建 {@link Slf4JLoggerFactory}
 * 如果项目中没有依赖 Slf4J，则会尝试创建 {@link Log4JLoggerFactory}
 * 如果项目中没有依赖 Log4J，则会尝试创建 {@link JdkLoggerFactory}
 *
 * 可以通过 setDefaultFactory 方法提前设置你想要使用的日志框架，比如：
 * <pre>
 * {@link InternalLoggerFactory}.setDefaultFactory({@link Log4JLoggerFactory}.INSTANCE);
 * </pre>
 * 这样就可以将 LoggerFactory 设置为 {@link Log4JLoggerFactory}
 * 需要注意的是 setDefaultFactory 方法最好是在最开始的时候就设置，
 * 因为新设置的 LoggerFactory 只会对修改后的那些类生效，
 * 之前已经创建了的Logger不会发生变化
 *
 */
public abstract class InternalLoggerFactory {

    private static volatile InternalLoggerFactory defaultFactory;

    private static volatile InternalLoggerFactory debuggerLoggerFactory;

    @SuppressWarnings("UnusedCatchParameter")
    private static InternalLoggerFactory newDefaultFactory(String name) {
        InternalLoggerFactory loggerFactory;
        InternalLogger logger;
        try {
            // 默认使用Slf4JLoggerFactory
            loggerFactory = new Slf4JLoggerFactory(true);
            // 通过Slf4JLoggerFactory创建一个Logger，如果成功则说明项目中有使用Slf4J
            logger = loggerFactory.newInstance(name);
            System.out.println("[byteman][logger_factory] Have matched SLF4J as logging framework");
            logger.info("[byteman][logger_factory] Using SLF4J as the default logging framework");
        } catch (Throwable t1) {
            // 未匹配到SLF4J
            System.err.println("[byteman][logger_factory] Haven't match SLF4J logging framework will check Log4J");
            try {
                // 如果项目中没有使用Slf4J，则尝试使用Log4JLoggerFactory
                loggerFactory = Log4JLoggerFactory.INSTANCE;
                // 通过Log4JLoggerFactory创建一个Logger，如果成功则说明项目中有使用Log4J
                logger = loggerFactory.newInstance(name);
                System.out.println("[byteman][logger_factory] Have matched Log4J as logging framework");
                logger.info("[byteman][logger_factory] Using Log4J as the default logging framework");
            } catch (Throwable t2) {
                // 未匹配到Log4J
                System.err.println("[byteman][logger_factory] Haven't match Log4J logging framework will check Log4J2");
                try {
                    // 如果项目中没有使用Log4J，则尝试使用Log4J2LoggerFactory
                    loggerFactory = Log4J2LoggerFactory.INSTANCE;
                    // 通过Log4J2LoggerFactory创建一个Logger，如果成功则说明项目中有使用Log4J
                    logger = loggerFactory.newInstance(name);
                    System.out.println("[byteman][logger_factory] Have matched Log4J2 as logging framework");
                    logger.info("[byteman][logger_factory] Using Log4J2 as the default logging framework");
                } catch (Throwable t3) {
                    // 未匹配到Log4J2
                    System.err.println("[byteman][logger_factory] Haven't match Log4J2 logging framework will check JDKLogger");
                    loggerFactory = JdkLoggerFactory.INSTANCE;
                    logger = loggerFactory.newInstance(name);
                    System.out.println("[byteman][logger_factory] Have matched JDKLogger as logging framework");
                    logger.info("[byteman][logger_factory] Using JDKLogger as the default logging framework");
                }
            }
        }
        return loggerFactory;
    }

    /**
     * Returns the default factory.  The initial default factory is
     * {@link JdkLoggerFactory}.
     */
    private static InternalLoggerFactory getDefaultFactory(boolean useDebugLogger) {
        // 关门用来调试的LoggerFactory
        if (useDebugLogger) {
            if (debuggerLoggerFactory == null ) {
                System.out.println("[track_trace][logger_factory] Have matched SoutLogger as debug logging framework");
                debuggerLoggerFactory = SoutLoggerFactory.INSTANCE;
            }
            return debuggerLoggerFactory;
        }
        if (defaultFactory == null) {
            defaultFactory = newDefaultFactory(InternalLoggerFactory.class.getName());
        }
        return defaultFactory;
    }

    /**
     * Changes the default factory.
     */
    public static void setDefaultFactory(InternalLoggerFactory defaultFactory) {
        if (defaultFactory == null) {
            throw new NullPointerException("defaultFactory");
        }
        InternalLoggerFactory.defaultFactory = defaultFactory;
    }

    /**
     * Creates a new logger instance with the name of the specified class.
     */
    public static InternalLogger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * Creates a new logger instance with the specified name.
     */
    public static InternalLogger getLogger(String name) {
        return getDefaultFactory(false).newInstance(name);
    }

    /**
     * Creates a new logger instance with the name of the specified class.
     */
    public static InternalLogger getLogger(Class<?> clazz, boolean useDebugLogger) {
        return getLogger(clazz.getName(), useDebugLogger);
    }

    /**
     * Creates a new logger instance with the specified name.
     */
    public static InternalLogger getLogger(String name, boolean useDebugLogger) {
        return getDefaultFactory(useDebugLogger).newInstance(name);
    }
    /**
     * Creates a new logger instance with the specified name.
     */
    protected abstract InternalLogger newInstance(String name);

}
