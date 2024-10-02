package controllers;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.routing.RoundRobinRouter;
import akka.util.Duration;

public class Pi {

    static class Calculate {
    }

    static class Work {
        private final int start;
        private final int nrOfElements;

        public Work(int start, int nrOfElements) {
            this.start = start;
            this.nrOfElements = nrOfElements;
        }

        public int getStart() {
            return start;
        }

        public int getNrOfElements() {
            return nrOfElements;
        }
    }

    static class Result {
        private final double value;

        public Result(double value) {
            this.value = value;
        }

        public double getValue() {
            return value;
        }
    }

    static class PiApproximation {
        private final double pi;
        
        public PiApproximation(double pi) {
            this.pi = pi;
            
        }

        public double getPi() {
            return pi;
        }

    }

    public static class Worker extends UntypedActor {

        private double calculatePiFor(int start, int nrOfElements) {
            double acc = 0.0;
            for (int i = start * nrOfElements; i <= ((start + 1) * nrOfElements - 1); i++) {
                acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1);
            }
            
            return acc;
        }

        public void onReceive(Object message) {
            if (message instanceof Work) {
                Work work = (Work) message;
                double result = calculatePiFor(work.getStart(), work.getNrOfElements());
                getSender().tell(new Result(result), getSelf());
            } else {
                unhandled(message);
            }
        }
    }

    public static class Master extends UntypedActor {
        private final int nrOfMessages;
        private final int nrOfElements;

        private double pi;
        private int nrOfResults;
        private final long start = System.currentTimeMillis();

        private final ActorRef listener;
        private final ActorRef workerRouter;

        public Master(final int nrOfWorkers, int nrOfMessages, int nrOfElements, ActorRef listener) {
            this.nrOfMessages = nrOfMessages;
            this.nrOfElements = nrOfElements;
            this.listener = listener;

            workerRouter = this.getContext().actorOf(new Props(Worker.class).withRouter(new RoundRobinRouter(nrOfWorkers)), "workerRouter");
        }

        public void onReceive(Object message) {
            if (message instanceof Calculate) {
                for (int start = 0; start < nrOfMessages; start++) {
                    workerRouter.tell(new Work(start, nrOfElements), getSelf());
                }
            } else if (message instanceof Result) {
                Result result = (Result) message;
                pi += result.getValue();
                nrOfResults += 1;
                if (nrOfResults == nrOfMessages) {
                    // Send the result to the listener
                    listener.tell(new PiApproximation(pi), getSelf());
                    // Stops this actor and all its supervised children
                    getContext().stop(getSelf());
                }
            } else {
                unhandled(message);
            }
        }
    }

    public static class Listener extends UntypedActor {
        private ResultContainer<Double> resultContainer;

        public Listener(ResultContainer<Double> resultContainer) {
            this.resultContainer = resultContainer;
        }

        public void onReceive(Object message) {
            if (message instanceof PiApproximation) {
                PiApproximation approximation = (PiApproximation) message;
                System.out.println(String.format("\n\tPi approximation: \t\t%s\n", approximation.getPi()));
                resultContainer.setResult(approximation.getPi());
                synchronized (resultContainer) {
                    resultContainer.notifyAll();
                }
                getContext().system().shutdown();
            } else {
                unhandled(message);
            }
        }
    }

    public static double calculate(final int nrOfWorkers, final int nrOfElements, final int nrOfMessages) throws InterruptedException, ExecutionException {
        // Create an Akka system
        ActorSystem system = ActorSystem.create("PiSystem");

        final ResultContainer<Double> resultContainer = new ResultContainer<Double>();
        // create the result listener, which will print the result and shutdown the system
        final ActorRef listener = system.actorOf(new Props(new UntypedActorFactory() {
            @Override
            public Actor create() {
                return new Listener(resultContainer);
            }
        }), "listener");

        // create the master
        ActorRef master = system.actorOf(new Props(new UntypedActorFactory() {
            
            @Override
            public Actor create() {
                return new Master(nrOfWorkers, nrOfMessages, nrOfElements, listener);
            }
        }), "master");

        // start the calculation
        master.tell(new Calculate());

        synchronized (resultContainer) {
            resultContainer.wait();
        }
        
        return resultContainer.getResult();
    }
    
    private static class ResultContainer<T> {
        private T result;

        public T getResult() {
            return result;
        }

        public void setResult(T result) {
            this.result = result;
        }
    }
}