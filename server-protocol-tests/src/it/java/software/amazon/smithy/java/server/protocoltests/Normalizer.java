package software.amazon.smithy.java.server.protocoltests;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.ThreadLocalRandom;
import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;
import software.amazon.smithy.java.runtime.core.serde.DataStream;
import software.amazon.smithy.java.runtime.core.serde.document.Document;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

final class Normalizer {
    private Normalizer() {}

    private static final float FNAN_STANDIN = ThreadLocalRandom.current().nextFloat();
    private static final double DNAN_STANDIN = ThreadLocalRandom.current().nextDouble();

    static <T> T normalize(T obj) {
        if (obj instanceof Document d) {
            return (T) Document.createFromObject(d.asObject());
        }
        if (obj instanceof Node n) {
            return (T) scrubJSONNaNs(n);
        }
        if (!(obj instanceof SerializableStruct)) {
            return obj;
        }
        try {
            for (Field f : obj.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                if (f.get(obj) == null) {
                    continue;
                }
                if (f.getType() == double.class ||
                    f.getType() == Double.class) {
                    Double dVal = (Double) f.get(obj);
                    if (dVal != null && dVal.isNaN()) {
                        f.set(obj, DNAN_STANDIN);
                    }
                }
                if (f.getType() == float.class ||
                    f.getType() == Float.class) {
                    Float fVal = (Float) f.get(obj);
                    if (fVal != null && fVal.isNaN()) {
                        f.set(obj, FNAN_STANDIN);
                    }
                }
                if (f.getType() == Document.class) {
                    Document doc = (Document) f.get(obj);
                    f.set(obj, Document.createFromObject(doc.asObject()));
                }
                if (f.getType() == Map.class) {
                    Map map = (Map) f.get(obj);
                    for (Object e : map.entrySet()) {
                        Map.Entry entry = (Map.Entry) e;
                        entry.setValue(normalize(entry.getValue()));
                    }
                }
                if (List.class.isAssignableFrom(f.getType())) {
                    List c = (List) f.get(obj);
                    for (ListIterator<Object> iter = c.listIterator(); iter.hasNext();) {
                        iter.set(normalize(iter.next()));
                    }
                }
                if (DataStream.class.isAssignableFrom(f.getType())) {
                    DataStream ds = (DataStream) f.get(obj);
                    f.set(obj, new TestDataStream(ds));
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return obj;
    }

    private static Node scrubJSONNaNs(Node n) {
        if (n.isNumberNode() && n.expectNumberNode().isNaN()) {
            return new NumberNode(DNAN_STANDIN, n.getSourceLocation());
        }
        if (n.isObjectNode()) {
            var builder = ObjectNode.builder();
            for (Map.Entry<StringNode, Node> member : ((ObjectNode) n).getMembers().entrySet()) {
                if (member.getValue().isNumberNode()) {
                    if (member.getValue().expectNumberNode().isNaN()) {
                        builder.withMember(
                            member.getKey(),
                            new NumberNode(DNAN_STANDIN, member.getValue().getSourceLocation())
                        );
                    } else {
                        builder.withMember(member.getKey(), member.getValue());
                    }
                } else if (member.getValue().isObjectNode()) {
                    builder.withMember(member.getKey(), normalize(member.getValue()));
                } else {
                    builder.withMember(member.getKey(), member.getValue());
                }
            }
            return builder.build();
        }
        return n;
    }

    private static final class TestDataStream implements DataStream {
        private final byte[] bytes;
        private final Optional<String> contentType;

        TestDataStream(DataStream ds) {
            try {
                bytes = ds.asBytes().toCompletableFuture().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            contentType = ds.contentType();
        }

        @Override
        public long contentLength() {
            return bytes.length;
        }

        @Override
        public Optional<String> contentType() {
            return contentType;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
            subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                    subscriber.onNext(ByteBuffer.wrap(bytes));
                    subscriber.onComplete();
                }

                @Override
                public void cancel() {

                }
            });
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestDataStream that = (TestDataStream) o;
            return Objects.deepEquals(bytes, that.bytes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bytes);
        }
    }

}
