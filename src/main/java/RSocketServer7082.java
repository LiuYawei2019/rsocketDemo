import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class RSocketServer7082 {

    public static void main(String[] args) {
        try {
            RSocketServer.create(
                    SocketAcceptor.forRequestResponse(
                            p -> {
                                System.out.println("Server 3 got fnf " + p.getDataUtf8());
                                return Mono.just(DefaultPayload.create("Server 3 response"))
                                        .delayElement(Duration.ofMillis(100));
                            }))
                    .bindNow(TcpServerTransport.create("127.0.0.1", 7082));
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
