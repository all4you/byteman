package org.byteman.logger;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 通过System.out.println
 * 和System.err.println
 * 来打印日志
 * 主要是用来调试
 *
 * @author houyi.wh
 * @since 2021-08-04
 */
public class SoutLogger extends AbstractInternalLogger {

    private static SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private static String loggerName = SoutLogger.class.getName();

    private static ExecutorService executorService;

    public SoutLogger(String name) {
        super(name);
        executorService = new ThreadPoolExecutor(5, 5,
                5000L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(500),
                new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(1);
                    private final SecurityManager securityManager = System.getSecurityManager();
                    private final ThreadGroup threadGroup = (securityManager != null) ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();

                    @Override
                    public Thread newThread(Runnable runnable) {
                        Thread t = new Thread(threadGroup, runnable,
                                "SoutLogger-thread-" + threadNumber.getAndIncrement(),
                                0);
                        if (t.isDaemon())
                            t.setDaemon(false);
                        if (t.getPriority() != Thread.NORM_PRIORITY)
                            t.setPriority(Thread.NORM_PRIORITY);
                        return t;
                    }
                });
    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public void trace(String msg) {
        // do nothing
    }

    @Override
    public void trace(String format, Object arg) {
        // do nothing
    }

    @Override
    public void trace(String format, Object argA, Object argB) {
        // do nothing
    }

    @Override
    public void trace(String format, Object... arguments) {
        // do nothing
    }

    @Override
    public void trace(String msg, Throwable t) {
        // do nothing
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public void debug(String msg) {
        println(System.out, "DEBUG", msg);
    }

    @Override
    public void debug(String format, Object arg) {
        String msg = StrFormatter.format(format, arg);
        println(System.out, "DEBUG", msg);
    }

    @Override
    public void debug(String format, Object argA, Object argB) {
        String msg = StrFormatter.format(format, argA, argB);
        println(System.out, "DEBUG", msg);
    }

    @Override
    public void debug(String format, Object... arguments) {
        String msg = StrFormatter.format(format, arguments);
        println(System.out, "DEBUG", msg);
    }

    @Override
    public void debug(String msg, Throwable t) {
        println(System.out, "DEBUG", msg);
        if (t != null) {
            t.printStackTrace(System.out);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return false;
    }

    @Override
    public void info(String msg) {
        println(System.out, "INFO", msg);
    }

    @Override
    public void info(String format, Object arg) {
        String msg = StrFormatter.format(format, arg);
        println(System.out, "INFO", msg);
    }

    @Override
    public void info(String format, Object argA, Object argB) {
        String msg = StrFormatter.format(format, argA, argB);
        println(System.out, "INFO", msg);
    }

    @Override
    public void info(String format, Object... arguments) {
        String msg = StrFormatter.format(format, arguments);
        println(System.out, "INFO", msg);
    }

    @Override
    public void info(String msg, Throwable t) {
        println(System.out, "INFO", msg);
        if (t != null) {
            t.printStackTrace(System.out);
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return false;
    }

    @Override
    public void warn(String msg) {
        println(System.out, "WARN", msg);
    }

    @Override
    public void warn(String format, Object arg) {
        String msg = StrFormatter.format(format, arg);
        println(System.out, "WARN", msg);
    }

    @Override
    public void warn(String format, Object argA, Object argB) {
        String msg = StrFormatter.format(format, argA, argB);
        println(System.out, "WARN", msg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        String msg = StrFormatter.format(format, arguments);
        println(System.out, "WARN", msg);
    }

    @Override
    public void warn(String msg, Throwable t) {
        println(System.out, "WARN", msg);
        if (t != null) {
            t.printStackTrace(System.out);
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return false;
    }

    @Override
    public void error(String msg) {
        println(System.err, "ERROR", msg);
    }

    @Override
    public void error(String format, Object arg) {
        String msg = StrFormatter.format(format, arg);
        println(System.err, "ERROR", msg);
    }

    @Override
    public void error(String format, Object argA, Object argB) {
        String msg = StrFormatter.format(format, argA, argB);
        println(System.err, "ERROR", msg);
    }

    @Override
    public void error(String format, Object... arguments) {
        String msg = StrFormatter.format(format, arguments);
        println(System.err, "ERROR", msg);
    }

    @Override
    public void error(String msg, Throwable t) {
        println(System.err, "ERROR", msg);
        if (t != null) {
            t.printStackTrace(System.err);
        }
    }

    private void println(final PrintStream out, final String logLevel, final String msg) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                String time = dtf.format(new Date());
                out.println(time + "|" + logLevel + "|" + Thread.currentThread().getName() + "|" + loggerName + "|" + msg);
            }
        });
    }

}
