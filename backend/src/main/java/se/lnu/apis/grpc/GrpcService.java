package se.lnu.apis.grpc;

import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.lnu.data.DataStore;

/**
 * gRPC service implementation — returns the full tree in one response.
 *
 * Builds the Node tree recursively from DataStore and returns it as a
 * single TreeResponse. CM2 is tracked via OrchestrationGrpcInterceptor.
 */
@Component
public class GrpcService extends TreeServiceGrpc.TreeServiceImplBase {

    @Autowired
    private DataStore dataStore;

    @Override
    public void getTree(TreeRequest request, StreamObserver<TreeResponse> responseObserver) {
        se.lnu.data.Node root = dataStore.getRoot();
        TreeResponse response = TreeResponse.newBuilder()
                .setRoot(buildNode(root, request.getTargetDepth(), 0))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private Node buildNode(se.lnu.data.Node node, int targetDepth, int currentDepth) {
        Node.Builder builder = Node.newBuilder();
        builder.setId(node.getId());
        node.getFields().forEach((key, value) -> setField(builder, key, value));
        if (currentDepth < targetDepth) {
            for (se.lnu.data.Node child : dataStore.getChildren(node.getId())) {
                builder.addChildren(buildNode(child, targetDepth, currentDepth + 1));
            }
        }
        return builder.build();
    }

    private void setField(Node.Builder builder, String key, String value) {
        switch (key) {
            case "k00" -> builder.setK00(value);
            case "k01" -> builder.setK01(value);
            case "k02" -> builder.setK02(value);
            case "k03" -> builder.setK03(value);
            case "k04" -> builder.setK04(value);
            case "k05" -> builder.setK05(value);
            case "k06" -> builder.setK06(value);
            case "k07" -> builder.setK07(value);
            case "k08" -> builder.setK08(value);
            case "k09" -> builder.setK09(value);
            case "k10" -> builder.setK10(value);
            case "k11" -> builder.setK11(value);
            case "k12" -> builder.setK12(value);
            case "k13" -> builder.setK13(value);
            case "k14" -> builder.setK14(value);
            case "k15" -> builder.setK15(value);
            case "k16" -> builder.setK16(value);
            case "k17" -> builder.setK17(value);
            case "k18" -> builder.setK18(value);
            case "k19" -> builder.setK19(value);
            case "k20" -> builder.setK20(value);
            case "k21" -> builder.setK21(value);
            case "k22" -> builder.setK22(value);
            case "k23" -> builder.setK23(value);
            case "k24" -> builder.setK24(value);
            case "k25" -> builder.setK25(value);
            case "k26" -> builder.setK26(value);
            case "k27" -> builder.setK27(value);
            case "k28" -> builder.setK28(value);
            case "k29" -> builder.setK29(value);
            case "k30" -> builder.setK30(value);
            case "k31" -> builder.setK31(value);
            case "k32" -> builder.setK32(value);
            case "k33" -> builder.setK33(value);
            case "k34" -> builder.setK34(value);
            case "k35" -> builder.setK35(value);
            case "k36" -> builder.setK36(value);
            case "k37" -> builder.setK37(value);
            case "k38" -> builder.setK38(value);
            case "k39" -> builder.setK39(value);
            case "k40" -> builder.setK40(value);
            case "k41" -> builder.setK41(value);
            case "k42" -> builder.setK42(value);
            case "k43" -> builder.setK43(value);
            case "k44" -> builder.setK44(value);
            case "k45" -> builder.setK45(value);
            case "k46" -> builder.setK46(value);
            case "k47" -> builder.setK47(value);
            case "k48" -> builder.setK48(value);
            case "k49" -> builder.setK49(value);
            case "k50" -> builder.setK50(value);
            case "k51" -> builder.setK51(value);
            case "k52" -> builder.setK52(value);
            case "k53" -> builder.setK53(value);
            case "k54" -> builder.setK54(value);
            case "k55" -> builder.setK55(value);
            case "k56" -> builder.setK56(value);
            case "k57" -> builder.setK57(value);
            case "k58" -> builder.setK58(value);
            case "k59" -> builder.setK59(value);
            case "k60" -> builder.setK60(value);
            case "k61" -> builder.setK61(value);
            case "k62" -> builder.setK62(value);
            case "k63" -> builder.setK63(value);
            case "k64" -> builder.setK64(value);
            case "k65" -> builder.setK65(value);
            case "k66" -> builder.setK66(value);
            case "k67" -> builder.setK67(value);
            case "k68" -> builder.setK68(value);
            case "k69" -> builder.setK69(value);
            case "k70" -> builder.setK70(value);
            case "k71" -> builder.setK71(value);
            case "k72" -> builder.setK72(value);
            case "k73" -> builder.setK73(value);
            case "k74" -> builder.setK74(value);
            case "k75" -> builder.setK75(value);
            case "k76" -> builder.setK76(value);
            case "k77" -> builder.setK77(value);
            case "k78" -> builder.setK78(value);
            case "k79" -> builder.setK79(value);
            case "k80" -> builder.setK80(value);
            case "k81" -> builder.setK81(value);
            case "k82" -> builder.setK82(value);
            case "k83" -> builder.setK83(value);
            case "k84" -> builder.setK84(value);
            case "k85" -> builder.setK85(value);
            case "k86" -> builder.setK86(value);
            case "k87" -> builder.setK87(value);
            case "k88" -> builder.setK88(value);
            case "k89" -> builder.setK89(value);
            case "k90" -> builder.setK90(value);
            case "k91" -> builder.setK91(value);
            case "k92" -> builder.setK92(value);
            case "k93" -> builder.setK93(value);
            case "k94" -> builder.setK94(value);
            case "k95" -> builder.setK95(value);
            case "k96" -> builder.setK96(value);
            case "k97" -> builder.setK97(value);
            case "k98" -> builder.setK98(value);
            case "k99" -> builder.setK99(value);
        }
    }
}
