import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Materializes deterministic comparison crops from the single approved RedactGuard target raster.
 * Uses only JDK APIs so Visual Evidence can reproduce the regions without another image dependency.
 */
public final class MaterializeVisualTargetCrops {
    private static final Pattern SHA_PATTERN =
            Pattern.compile("\\\"sha256\\\"\\s*:\\s*\\\"([0-9a-f]{64})\\\"");
    private static final Pattern WIDTH_PATTERN = Pattern.compile("\\\"width_px\\\"\\s*:\\s*(\\d+)");
    private static final Pattern HEIGHT_PATTERN = Pattern.compile("\\\"height_px\\\"\\s*:\\s*(\\d+)");
    private static final Pattern REGION_PATTERN =
            Pattern.compile(
                    "\\{\\s*\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*"
                            + "\\\"left\\\"\\s*:\\s*(\\d+)\\s*,\\s*"
                            + "\\\"top\\\"\\s*:\\s*(\\d+)\\s*,\\s*"
                            + "\\\"right\\\"\\s*:\\s*(\\d+)\\s*,\\s*"
                            + "\\\"bottom\\\"\\s*:\\s*(\\d+)\\s*\\}");

    private MaterializeVisualTargetCrops() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: java scripts/MaterializeVisualTargetCrops.java <target> <provenance.json> <output-dir>");
        }

        Path target = Path.of(args[0]);
        Path provenance = Path.of(args[1]);
        Path outputDir = Path.of(args[2]);
        String manifest = Files.readString(provenance, StandardCharsets.UTF_8);

        verifySha256(target, requiredGroup(SHA_PATTERN, manifest, "sha256"));
        BufferedImage image = requireImage(target);
        int expectedWidth = Integer.parseInt(requiredGroup(WIDTH_PATTERN, manifest, "width_px"));
        int expectedHeight = Integer.parseInt(requiredGroup(HEIGHT_PATTERN, manifest, "height_px"));
        if (image.getWidth() != expectedWidth || image.getHeight() != expectedHeight) {
            throw new IllegalStateException(
                    "Target dimensions mismatch: expected "
                            + expectedWidth + "x" + expectedHeight
                            + ", got " + image.getWidth() + "x" + image.getHeight());
        }

        Files.createDirectories(outputDir);
        Matcher regions = REGION_PATTERN.matcher(manifest);
        int count = 0;
        while (regions.find()) {
            String id = regions.group(1);
            int left = Integer.parseInt(regions.group(2));
            int top = Integer.parseInt(regions.group(3));
            int right = Integer.parseInt(regions.group(4));
            int bottom = Integer.parseInt(regions.group(5));
            validateBounds(id, left, top, right, bottom, image);

            BufferedImage crop = image.getSubimage(left, top, right - left, bottom - top);
            Path output = outputDir.resolve(id.replace('_', '-') + ".png");
            if (!ImageIO.write(crop, "png", output.toFile())) {
                throw new IllegalStateException("No PNG writer available for " + output);
            }
            System.out.println(output + " " + crop.getWidth() + "x" + crop.getHeight());
            count++;
        }
        if (count == 0) {
            throw new IllegalStateException("No target regions found in " + provenance);
        }
    }

    private static void verifySha256(Path path, String expected) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] actualBytes = digest.digest(Files.readAllBytes(path));
        String actual = HexFormat.of().formatHex(actualBytes);
        if (!actual.equals(expected)) {
            throw new IllegalStateException(
                    "Target SHA-256 mismatch: expected " + expected + ", got " + actual);
        }
    }

    private static BufferedImage requireImage(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) {
            throw new IllegalStateException("Unsupported target image: " + path);
        }
        return image;
    }

    private static String requiredGroup(Pattern pattern, String source, String label) {
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            throw new IllegalStateException("Missing " + label + " in target provenance");
        }
        return matcher.group(1);
    }

    private static void validateBounds(
            String id,
            int left,
            int top,
            int right,
            int bottom,
            BufferedImage image) {
        if (left < 0 || top < 0 || right <= left || bottom <= top
                || right > image.getWidth() || bottom > image.getHeight()) {
            throw new IllegalArgumentException(
                    "Invalid crop bounds for " + id + ": "
                            + left + "," + top + "," + right + "," + bottom);
        }
    }
}
