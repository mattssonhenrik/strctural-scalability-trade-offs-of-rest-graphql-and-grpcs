package se.lnu.metrics;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * gRPC ServerInterceptor that resets CM2 counter before each call
 * and writes the final count as trailing metadata after the response.
 */
public class OrchestrationGrpcInterceptor implements ServerInterceptor {

    static final Metadata.Key<String> CM2_KEY =
            Metadata.Key.of("x-orchestration-count", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <Req, Resp> ServerCall.Listener<Req> interceptCall(
            ServerCall<Req, Resp> call, Metadata headers, ServerCallHandler<Req, Resp> next) {

        OrchestrationCounter.reset();

        ServerCall<Req, Resp> wrappedCall = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
            @Override
            public void sendMessage(Resp message) {
                Metadata trailers = new Metadata();
                trailers.put(CM2_KEY, String.valueOf(OrchestrationCounter.get()));
                super.sendMessage(message);
                // trailers sent in close()
            }

            @Override
            public void close(io.grpc.Status status, Metadata trailers) {
                trailers.put(CM2_KEY, String.valueOf(OrchestrationCounter.get()));
                super.close(status, trailers);
            }
        };

        return next.startCall(wrappedCall, headers);
    }
}
