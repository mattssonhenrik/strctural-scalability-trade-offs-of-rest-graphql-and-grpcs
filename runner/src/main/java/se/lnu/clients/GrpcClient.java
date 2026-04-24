package se.lnu.clients;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import se.lnu.apis.grpc.TreeRequest;
import se.lnu.apis.grpc.TreeResponse;
import se.lnu.apis.grpc.TreeServiceGrpc;
import se.lnu.runner.TestConfig;

import java.util.concurrent.atomic.AtomicReference;

/**
 * gRPC client — fetches the full tree in one RPC call.
 * Returns the raw TreeResponse and trailing metadata for CM2 and CM3 measurement.
 */
public class GrpcClient {

    static final Metadata.Key<String> CM2_KEY =
            Metadata.Key.of("x-orchestration-count", Metadata.ASCII_STRING_MARSHALLER);

    private final ManagedChannel channel;
    private final TreeServiceGrpc.TreeServiceBlockingStub stub;

    public GrpcClient() {
        channel = ManagedChannelBuilder.forAddress("localhost", TestConfig.GRPC_PORT)
                .usePlaintext()
                .maxInboundMessageSize(1000 * 1024 * 1024)  // TODO: 1000 MB — default 4 MB är för litet för stora träd -1????
                .build();
        stub = TreeServiceGrpc.newBlockingStub(channel);
    }

    public GrpcResult fetch(int targetDepth) {
        AtomicReference<Metadata> trailingMetadata = new AtomicReference<>();

        TreeServiceGrpc.TreeServiceBlockingStub interceptedStub = stub.withInterceptors(
                MetadataUtils.newCaptureMetadataInterceptor(new AtomicReference<>(), trailingMetadata));

        TreeResponse response = interceptedStub.getTree(
                TreeRequest.newBuilder().setRootId("root").setTargetDepth(targetDepth).build());

        int cm2 = 0;
        Metadata trailing = trailingMetadata.get();
        if (trailing != null) {
            String val = trailing.get(CM2_KEY);
            if (val != null) cm2 = Integer.parseInt(val);
        }

        int cm3 = response.toByteArray().length;
        return new GrpcResult(response, cm2, cm3);
    }

    public void shutdown() {
        channel.shutdown();
    }

    /** Carries the response + pre-extracted CM2 and CM3 back to TestRunner. */
    public record GrpcResult(TreeResponse response, int cm2, int cm3) {}
}
