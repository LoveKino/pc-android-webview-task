package com.ddchen.bridge.pc;

import com.ddchen.bridge.pc.Promise.Callable;
import com.ddchen.bridge.pc.Promise.Finish;
import com.ddchen.bridge.pc.Promise.State;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

/**
 * Created by yuer on 11/2/16.
 */
public class PromiseTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void then_imm() throws Exception {
        Promise p = new Promise(new State() {
            @Override
            public void appoint(Finish finish) {
                finish.resolve(10);
            }
        });

        p.then(new Callable() {
            @Override
            public Object call(Object prev) {
                assertEquals(prev, 10);
                return null;
            }
        });
    }

    @Test
    public void then_sleep() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        Promise p = new Promise(new State() {
            @Override
            public void appoint(final Finish finish) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                            finish.resolve(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        p.then(new Callable() {
            @Override
            public Object call(Object prev) {
                System.out.println(prev);
                signal.countDown();
                return null;
            }
        });

        signal.await();
    }

    @Test
    public void then_then_test() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        Promise p = new Promise(new State() {
            @Override
            public void appoint(final Finish finish) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                            finish.resolve(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        p.then(new Callable() {
            @Override
            public Object call(Object prev) {
                System.out.println(prev);
                return 3;
            }
        }).then(new Callable() {
            @Override
            public Object call(Object prev) {
                System.out.println(prev);
                signal.countDown();
                return null;
            }
        });

        signal.await();
    }

    @Test
    public void doCatch_test() throws Exception {
        Promise p = new Promise(new State() {
            @Override
            public void appoint(final Finish finish) {
                finish.reject(new Exception("on purpose"));
            }
        });

        p.doCatch(new Callable() {
            @Override
            public Object call(Object prev) {
                assertEquals(prev.toString().trim(), "java.lang.Exception: on purpose");
                return null;
            }
        });
    }

    @Test
    public void doCatch_test2() throws Exception {
        Promise p = new Promise(new State() {
            @Override
            public void appoint(final Finish finish) {
                finish.resolve("haha");
            }
        });

        p.then(new Callable() {
            @Override
            public Object call(Object prev) {
                return (Double) prev / 10;
            }
        }).doCatch(new Callable() {
            @Override
            public Object call(Object prev) {
                assertEquals(prev.toString().trim(), "java.lang.ClassCastException: java.lang.String cannot be cast to java.lang.Double");
                return null;
            }
        });
    }

    @Test
    public void doCatch_test3() throws Exception {
        Promise p = new Promise(new State() {
            @Override
            public void appoint(final Finish finish) {
                finish.reject("haha");
            }
        });

        p.then(new Callable() {
            @Override
            public Object call(Object prev) {
                return null;
            }
        }).doCatch(new Callable() {
            @Override
            public Object call(Object prev) {
                System.out.println(prev);
                assertEquals(prev.toString().trim(), "haha");
                return null;
            }
        });
    }

    @Test
    public void doCatch_test4() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        Promise p = new Promise(new State() {
            @Override
            public void appoint(final Finish finish) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                            finish.reject("haha");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        p.then(new Callable() {
            @Override
            public Object call(Object prev) {
                return null;
            }
        }).doCatch(new Callable() {
            @Override
            public Object call(Object prev) {
                System.out.println(prev);
                assertEquals(prev.toString().trim(), "haha");
                signal.countDown();
                return null;
            }
        });

        signal.await();
    }
}