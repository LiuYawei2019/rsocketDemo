import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class RSocketServer7081 {

    public static void main(String[] args) {
        try {
            RSocketServer.create(
                            SocketAcceptor.forRequestResponse(
                                    p -> {
                                        System.out.println("Server 2 got fnf " + p.getDataUtf8());
                                        return Mono.just(DefaultPayload.create("Server 2 response"))
                                                .delayElement(Duration.ofMillis(100));
                                    }))
                    .bindNow(TcpServerTransport.create(7081));
            while(true) {
                try {
                    TimeUnit.MILLISECONDS.sleep(1000000);
                } catch (Exception ignore) {

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
