package com.ddchen.bridge.pc;

import java.util.ArrayList;

/**
 * Created by yuer on 11/2/16.
 */

public class Promise {
    public interface Finish {
        void resolve(Object obj);

        void reject(Object obj);
    }

    public interface State {
        void appoint(Finish finish);
    }

    public interface Callable {
        Object call(Object prev);
    }

    private class PromiseResult {
        public boolean isException = false;
        public Object value = null;

        public PromiseResult(boolean isException, Object value) {
            this.isException = isException;
            this.value = value;
        }
    }

    private class WaitingPromise {
        public Promise p;
        public Finish finish;
        public Callable callable;

        public WaitingPromise(Callable callable) {
            this.p = new Promise(new State() {
                @Override
                public void appoint(Finish finish) {
                    WaitingPromise.this.finish = finish;
                }
            });
            this.callable = callable;
        }
    }

    private PromiseResult result = null;

    private ArrayList<WaitingPromise> waitingPromises = new ArrayList<>();
    private ArrayList<WaitingPromise> errorWaitingPromises = new ArrayList<>();

    private Finish finish = new Finish() {
        @Override
        public void resolve(final Object obj) {
            if (result == null) {
                result = new PromiseResult(false, obj);
                consumeThenWaits(obj);
            }
        }

        @Override
        public void reject(Object obj) {
            if (result == null) {
                result = new PromiseResult(true, obj);
                consumeErrorWaits(obj);
            }
        }
    };

    private void consumeThenWaits(Object obj) {
        // consume list
        while (waitingPromises.size() > 0) {
            final WaitingPromise first = waitingPromises.remove(0);
            consumePromise(first.callable, obj).then(new Callable() {
                @Override
                public Object call(Object prev) {
                    first.finish.resolve(prev);
                    return null;
                }
            });
        }
    }

    private void consumeErrorWaits(Object obj) {
        // catching list
        while (errorWaitingPromises.size() > 0) {
            final WaitingPromise first = errorWaitingPromises.remove(0);
            consumePromise(first.callable, obj).then(new Callable() {
                @Override
                public Object call(Object prev) {
                    first.finish.reject(prev);
                    return null;
                }
            });
        }
        // then list
        while (waitingPromises.size() > 0) {
            final WaitingPromise first = waitingPromises.remove(0);
            first.finish.reject(obj);
        }
    }

    public Promise(State state) {
        state.appoint(finish);
    }

    public static Promise resolve(final Object obj) {
        if (obj instanceof Promise) {
            Promise p = (Promise) obj;
            return p.then(new Callable() {
                @Override
                public Object call(Object prev) {
                    return prev;
                }
            });
        } else {
            return new Promise(new State() {
                @Override
                public void appoint(Finish finish) {
                    finish.resolve(obj);
                }
            });
        }
    }

    public Promise then(Callable callable) {
        if (result == null) {
            WaitingPromise wait = new WaitingPromise(callable);
            waitingPromises.add(wait);
            return wait.p;
            //
        } else {
            if (!result.isException) {
                return consumePromise(callable, result.value);
            } else {
                // just passing current callable
                return this;
            }
        }
    }

    public Promise doCatch(Callable callable) {
        if (result == null) {
            WaitingPromise wait = new WaitingPromise(callable);
            errorWaitingPromises.add(wait);
            return wait.p;
        } else {
            if (result.isException) {
                return consumePromise(callable, result.value);
            } else {
                // just passing current callable
                return this;
            }
        }
    }

    private static Promise consumePromise(Callable callable, Object value) {
        try {
            Object ret = callable.call(value);
            return Promise.resolve(ret);
        } catch (final Exception exception) {
            return new Promise(new State() {
                @Override
                public void appoint(Finish finish) {
                    finish.reject(exception);
                }
            });
        }
    }
}
