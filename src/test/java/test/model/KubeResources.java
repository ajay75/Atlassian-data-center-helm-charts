package test.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.vavr.collection.Array;
import io.vavr.collection.Map;
import io.vavr.collection.Traversable;
import io.vavr.jackson.datatype.VavrModule;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Represents a collection of {@link KubeResources}, e.g. the YAML document generated by a Helm chart.
 */
public final class KubeResources {
    private final Map<Kind, Array<KubeResource>> map;

    private KubeResources(Map<Kind, Array<KubeResource>> map) {
        this.map = map;
    }

    public Traversable<StatefulSet> getStatefulSets() {
        return getAll(Kind.StatefulSet).map(StatefulSet.class::cast);
    }

    public StatefulSet getStatefulSet(final String name) {
        return getStatefulSets().find(ss -> name.equals(ss.getName()))
                .getOrElseThrow(() -> new AssertionError("No StatefulSet found with name " + name));
    }

    public KubeResource get(Kind kind) {
        return getAll(kind)
                .singleOption()
                .getOrElseThrow(() -> new AssertionError("No single " + kind + " found"));
    }

    public Traversable<KubeResource> getAll(Kind kind) {
        return map.get(kind)
                .getOrElse(Array::empty);
    }

    public static KubeResources parse(Path outputFile) throws IOException {
        final var yamlParser = new YAMLFactory().createParser(outputFile.toFile());
        final var objectMapper = new ObjectMapper().registerModule(new VavrModule());

        var map = Array.ofAll(objectMapper
                .readValues(yamlParser, new TypeReference<ObjectNode>() {
                })
                .readAll())
                .map(KubeResource::wrap)
                .groupBy(KubeResource::getKind);

        return new KubeResources(map);
    }
}
