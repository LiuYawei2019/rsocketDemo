import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.loadbalance.LoadbalanceRSocketClient;
import io.rsocket.loadbalance.LoadbalanceStrategy;
import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class LoadBalancerClientTest {

    public static void main(String[] args) {
        List<LoadbalanceTarget> targets = new ArrayList<>();
        targets.add(LoadbalanceTarget.from("7080", TcpClientTransport.create("127.0.0.1",7080)));
        targets.add(LoadbalanceTarget.from("7081", TcpClientTransport.create("127.0.0.1",7081)));
        targets.add(LoadbalanceTarget.from("7082", TcpClientTransport.create("127.0.0.1",7082)));

        Sinks.Many<List<LoadbalanceTarget>> targetsSink = Sinks.many().replay().latest();
        targetsSink.tryEmitNext(targets);

        RSocketClient rSocketClient = LoadbalanceRSocketClient.builder(targetsSink.asFlux()).connector(
                RSocketConnector.create().reconnect(Retry.fixedDelay(3, Duration.ofMillis(500)))
        ).loadbalanceStrategy(new SxfRoundRobinLoadbalanceStrategy(targetsSink, targets)).build();
        for (int i = 0; i < 5000000; i++) {
            try {
                long start = System.currentTimeMillis();
                Payload resp = Mono.just(DefaultPayload.create("test" + i))
                        .flatMap(payload -> rSocketClient.requestResponse(Mono.just(payload)))
                        .doOnError(t -> t.printStackTrace())
                        .timeout(Duration.ofSeconds(1))
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                        .block();
                System.out.println("==>>" + resp.getDataUtf8() + " cost:" + (System.currentTimeMillis() - start));
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    static class SxfRoundRobinLoadbalanceStrategy implements LoadbalanceStrategy {

        private final Sinks.Many<List<LoadbalanceTarget>> targetsSink;
        private final List<LoadbalanceTarget> targets;

        public SxfRoundRobinLoadbalanceStrategy(Sinks.Many<List<LoadbalanceTarget>> targetsSink, List<LoadbalanceTarget> targets) {
            this.targetsSink = targetsSink;
            this.targets = targets;
        }

        volatile int nextIndex;

        private static final AtomicIntegerFieldUpdater<SxfRoundRobinLoadbalanceStrategy> NEXT_INDEX =
                AtomicIntegerFieldUpdater.newUpdater(SxfRoundRobinLoadbalanceStrategy.class, "nextIndex");

        @Override
        public RSocket select(List<RSocket> sockets) {
            int length = sockets.size();
            int indexToUse = Math.abs(NEXT_INDEX.getAndIncrement(this) % length);
            return sockets.get(indexToUse);
        }
    }
}
