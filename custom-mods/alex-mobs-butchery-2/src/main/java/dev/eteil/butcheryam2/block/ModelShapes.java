package dev.eteil.butcheryam2.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Outline boxes of the converted block models, from {@code butcheryam2/shapes.json} (written by
 * tools/gen.py): per model, a few boxes in pixels facing north; per block, the model shown for
 * each value of its {@code blockstate} property.
 */
public final class ModelShapes {

    private static final Map<String, double[][]> MODELS = new HashMap<>();
    private static final Map<String, String[]> BLOCKS = new HashMap<>();

    static {
        try (InputStream in = ModelShapes.class.getResourceAsStream("/butcheryam2/shapes.json")) {
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("models").entrySet()) {
                JsonArray boxes = e.getValue().getAsJsonArray();
                double[][] out = new double[boxes.size()][];
                for (int i = 0; i < boxes.size(); i++) {
                    JsonArray b = boxes.get(i).getAsJsonArray();
                    out[i] = new double[b.size()];
                    for (int j = 0; j < b.size(); j++) {
                        out[i][j] = b.get(j).getAsDouble();
                    }
                }
                MODELS.put(e.getKey(), out);
            }
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("blocks").entrySet()) {
                JsonArray models = e.getValue().getAsJsonArray();
                String[] out = new String[models.size()];
                for (int i = 0; i < models.size(); i++) {
                    out[i] = models.get(i).getAsString();
                }
                BLOCKS.put(e.getKey(), out);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("could not read butcheryam2/shapes.json", ex);
        }
    }

    /** The outline boxes for a block at a given stage, or null if the block has none. */
    @Nullable
    public static double[][] boxes(String block, int stage) {
        String[] stages = BLOCKS.get(block);
        if (stages == null) {
            return null;
        }
        return MODELS.get(stages[Math.min(Math.max(stage, 0), stages.length - 1)]);
    }

    private ModelShapes() {}
}
