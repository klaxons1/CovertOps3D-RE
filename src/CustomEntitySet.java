import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * External C3D entity-placement sidecar.
 *
 * C3B contains static geometry only when its external-entities header flag is
 * set.  This UTF-8 INI stores editable x/z/angle/type/param/sprite records without
 * forcing every spawn or gameplay tweak to rebuild BSP geometry.  Parsing is
 * load-time only and uses primitive arrays afterwards, so there is no cost in
 * the frame or collision hot paths.
 */
public final class CustomEntitySet {

    private static final String FORMAT = "C3D-ENTITIES-1";
    private static final int FIELD_X = 1;
    private static final int FIELD_Z = 2;
    private static final int FIELD_TYPE = 4;
    private static final int REQUIRED_FIELDS = FIELD_X | FIELD_Z | FIELD_TYPE;

    private final short[] xs;
    private final short[] zs;
    private final short[] angles;
    private final short[] types;
    private final short[] parameters;
    // Optional manifest sprite slot. Slot N is installed in texture id -N.
    private final short[] spriteSlots;
    // frame1..frame6; spriteSlots is frame0.
    private final short[] animationSlots;
    // death1..death4 fill the middle of the Doom I->N death sequence.
    private final short[] deathAnimationSlots;
    private final int count;

    private CustomEntitySet(int count) {
        this.count = count;
        this.xs = new short[count];
        this.zs = new short[count];
        this.angles = new short[count];
        this.types = new short[count];
        this.parameters = new short[count];
        this.spriteSlots = new short[count];
        this.animationSlots = new short[count * 6];
        this.deathAnimationSlots = new short[count * 4];
    }

    /** Loads and validates a C3D entity INI resource. */
    public static CustomEntitySet load(String path) throws IOException {
        if (path == null) throw new NullPointerException();
        String text = readResourceText(path);
        int count = countEntitySections(text, path);
        if (count == 0) throw new IOException("Entity file has no entities: " + path);

        CustomEntitySet entities = new CustomEntitySet(count);
        parse(text, path, entities);
        return entities;
    }

    public int getEntityCount() {
        return count;
    }

    /**
     * Turns the sidecar records into the existing game-world objects.
     * Player spawn types (1..4) remain markers rather than static objects,
     * exactly as in the legacy map loader.
     */
    public void install(GameWorld world, boolean shouldLoadObjects) throws IOException {
        if (world == null) throw new NullPointerException();
        if (LevelLoader.levelVariant == 0) LevelLoader.levelVariant = 1;

        int staticCount = 0;
        int levelVariant = LevelLoader.levelVariant;
        for (int i = 0; i < count; ++i) {
            int type = types[i];
            if (type >= 1 && type <= 4 && levelVariant == type) {
                world.worldOrigin = createTransform(i);
            } else if (shouldLoadObjects && (type < 1 || type > 4)) {
                ++staticCount;
            }
        }
        if (world.worldOrigin == null) {
            throw new IOException("Entity file has no player spawn for variant " + levelVariant);
        }

        GameObject[] objects = shouldLoadObjects ? new GameObject[staticCount] : new GameObject[0];
        int objectIndex = 0;
        if (shouldLoadObjects) {
            for (int i = 0; i < count; ++i) {
                int type = types[i];
                if (type < 1 || type > 4) {
                    short rawAngle = angles[i];
                    GameObject object = new GameObject(createTransform(i), rawAngle,
                            type, parameters[i]);
                    addExternalSpriteFrames(object, spriteSlots[i], animationSlots, i * 6,
                            deathAnimationSlots, i * 4);
                    if (DoomGameMode.isSolidDoomProp(type)) {
                        // Generic props otherwise start with aiState=-1 and
                        // bypass the existing collision loop entirely.
                        object.aiState = 0;
                    }
                    objects[objectIndex++] = object;
                }
            }
        }
        world.staticObjects = objects;
    }

    private Transform3D createTransform(int index) {
        short angle = angles[index];
        return new Transform3D(xs[index] << 16, 0, zs[index] << 16,
                -angle * 1144 + 102943);
    }

    /**
     * C3D actor art is a single floor-centered billboard. It deliberately
     * bypasses the old upper/lower-body frame convention: Doom monsters are
     * not CovertOps legs, and AI frame indexes must not change their anchor.
     */
    private static void addExternalSpriteFrames(GameObject object, short spriteSlot,
                                                short[] animationSlots, int offset,
                                                short[] deathSlots, int deathOffset) {
        if (spriteSlot <= 0) return;
        byte[] frames = new byte[7];
        frames[0] = (byte)-spriteSlot;
        for (int frame = 1; frame < frames.length; ++frame) {
            short slot = animationSlots[offset + frame - 1];
            frames[frame] = slot > 0 ? (byte)-slot : frames[0];
        }
        object.setExternalBillboardFrames(frames);

        // Doom's first and final death poses are state frame5/frame6. The
        // sidecar carries only the four in-between poses, keeping normal C3D
        // entities compact while giving actors the complete I..N death arc.
        boolean hasDeathFrames = false;
        for (int frame = 0; frame < 4; ++frame) {
            if (deathSlots[deathOffset + frame] > 0) {
                hasDeathFrames = true;
                break;
            }
        }
        if (hasDeathFrames) {
            byte[] deathFrames = new byte[6];
            deathFrames[0] = frames[5];
            for (int frame = 0; frame < 4; ++frame) {
                short slot = deathSlots[deathOffset + frame];
                deathFrames[frame + 1] = slot > 0 ? (byte)-slot : deathFrames[frame];
            }
            deathFrames[5] = frames[6];
            object.setExternalBillboardDeathFrames(deathFrames);
        }
    }

    private static int countEntitySections(String text, String path) throws IOException {
        int count = 0;
        int lineStart = 0;
        while (lineStart <= text.length()) {
            int lineEnd = text.indexOf('\n', lineStart);
            if (lineEnd < 0) lineEnd = text.length();
            String line = text.substring(lineStart, lineEnd).trim();
            if (isEntityHeader(line)) ++count;
            if (lineEnd >= text.length()) break;
            lineStart = lineEnd + 1;
        }
        return count;
    }

    private static void parse(String text, String path, CustomEntitySet entities) throws IOException {
        int currentEntity = -1;
        int parsedEntities = 0;
        int entityFields = 0;
        int[] entityIds = new int[entities.count];
        boolean entitiesSectionSeen = false;
        boolean formatSeen = false;
        boolean inEntitiesSection = false;
        int lineNumber = 0;
        int lineStart = 0;

        while (lineStart <= text.length()) {
            int lineEnd = text.indexOf('\n', lineStart);
            if (lineEnd < 0) lineEnd = text.length();
            String line = text.substring(lineStart, lineEnd).trim();
            ++lineNumber;

            if (line.length() != 0 && line.charAt(0) != '#' && line.charAt(0) != ';') {
                if (isHeader(line)) {
                    if (currentEntity >= 0) {
                        validateEntityFields(path, lineNumber, currentEntity, entityFields);
                    }
                    currentEntity = -1;
                    entityFields = 0;
                    String section = line.substring(1, line.length() - 1).trim();

                    if (section.equals("entities")) {
                        if (entitiesSectionSeen) {
                            throw new IOException(path + ":" + lineNumber
                                    + " duplicate [entities] section");
                        }
                        entitiesSectionSeen = true;
                        inEntitiesSection = true;
                    } else if (startsWith(section, "entity.")) {
                        if (!entitiesSectionSeen || !formatSeen) {
                            throw new IOException(path + ":" + lineNumber
                                    + " [entities] format must come first");
                        }
                        int id = parseEntityId(section.substring(7), path, lineNumber);
                        for (int i = 0; i < parsedEntities; ++i) {
                            if (entityIds[i] == id) {
                                throw new IOException(path + ":" + lineNumber
                                        + " duplicate entity id");
                            }
                        }
                        if (parsedEntities >= entities.count) {
                            throw new IOException(path + ": too many entity sections");
                        }
                        currentEntity = parsedEntities++;
                        entityIds[currentEntity] = id;
                        inEntitiesSection = false;
                    } else {
                        throw new IOException(path + ":" + lineNumber
                                + " unknown section [" + section + "]");
                    }
                } else {
                    int equals = line.indexOf('=');
                    if (equals <= 0) {
                        throw new IOException(path + ":" + lineNumber + " expected key=value");
                    }
                    String key = line.substring(0, equals).trim();
                    String value = line.substring(equals + 1).trim();

                    if (currentEntity < 0) {
                        if (!inEntitiesSection || !key.equals("format") || formatSeen) {
                            throw new IOException(path + ":" + lineNumber
                                    + " property outside an entity section");
                        }
                        if (!value.equals(FORMAT)) {
                            throw new IOException(path + ":" + lineNumber
                                    + " unsupported entity format: " + value);
                        }
                        formatSeen = true;
                    } else {
                        int field = parseEntityField(key, value, path, lineNumber, entities, currentEntity);
                        if ((entityFields & field) != 0) {
                            throw new IOException(path + ":" + lineNumber
                                    + " duplicate entity key: " + key);
                        }
                        entityFields |= field;
                    }
                }
            }

            if (lineEnd >= text.length()) break;
            lineStart = lineEnd + 1;
        }

        if (currentEntity >= 0) {
            validateEntityFields(path, lineNumber, currentEntity, entityFields);
        }
        if (!formatSeen) {
            throw new IOException(path + " missing [entities] format=" + FORMAT);
        }
        if (parsedEntities != entities.count) {
            throw new IOException(path + " entity section count changed while parsing");
        }

        boolean hasSpawn = false;
        for (int i = 0; i < entities.count; ++i) {
            int type = entities.types[i];
            if (type >= 1 && type <= 4) {
                hasSpawn = true;
                break;
            }
        }
        if (!hasSpawn) throw new IOException(path + " has no player spawn (type 1..4)");
    }

    private static int parseEntityField(String key, String value, String path, int lineNumber,
                                        CustomEntitySet entities, int index) throws IOException {
        short number = parseShort(value, path, lineNumber, key);
        if (key.equals("x")) {
            entities.xs[index] = number;
            return FIELD_X;
        }
        if (key.equals("z")) {
            entities.zs[index] = number;
            return FIELD_Z;
        }
        if (key.equals("angle")) {
            entities.angles[index] = number;
            return 8;
        }
        if (key.equals("type")) {
            entities.types[index] = number;
            return FIELD_TYPE;
        }
        if (key.equals("param")) {
            entities.parameters[index] = number;
            return 16;
        }
        if (key.equals("sprite")) {
            if (number < 0 || number > 127) {
                throw new IOException(path + ":" + lineNumber
                        + " sprite must be a material slot 0..127");
            }
            entities.spriteSlots[index] = number;
            return 32;
        }
        if (startsWith(key, "frame") && key.length() == 6) {
            int frame = key.charAt(5) - '0';
            if (frame >= 1 && frame <= 6) {
                if (number < 0 || number > 127) {
                    throw new IOException(path + ":" + lineNumber
                            + " frame sprite must be a material slot 0..127");
                }
                entities.animationSlots[index * 6 + frame - 1] = number;
                return 32 << frame;
            }
        }
        if (startsWith(key, "death") && key.length() == 6) {
            int frame = key.charAt(5) - '0';
            if (frame >= 1 && frame <= 4) {
                if (number < 0 || number > 127) {
                    throw new IOException(path + ":" + lineNumber
                            + " death sprite must be a material slot 0..127");
                }
                entities.deathAnimationSlots[index * 4 + frame - 1] = number;
                return 8192 << frame;
            }
        }
        throw new IOException(path + ":" + lineNumber + " unknown entity key: " + key);
    }

    private static void validateEntityFields(String path, int lineNumber, int entityIndex,
                                             int fields) throws IOException {
        if ((fields & REQUIRED_FIELDS) != REQUIRED_FIELDS) {
            throw new IOException(path + ":" + lineNumber + " entity." + entityIndex
                    + " needs x, z and type");
        }
    }

    private static int parseEntityId(String value, String path, int lineNumber) throws IOException {
        try {
            int id = Integer.parseInt(value);
            if (id < 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException error) {
            throw new IOException(path + ":" + lineNumber + " entity id must be a non-negative integer");
        }
    }

    private static short parseShort(String value, String path, int lineNumber, String key)
            throws IOException {
        try {
            int number = Integer.parseInt(value);
            if (number < -32768 || number > 32767) throw new NumberFormatException();
            return (short)number;
        } catch (NumberFormatException error) {
            throw new IOException(path + ":" + lineNumber + " " + key
                    + " must be a signed 16-bit integer");
        }
    }

    private static boolean isHeader(String line) {
        return line.length() >= 2 && line.charAt(0) == '['
                && line.charAt(line.length() - 1) == ']';
    }

    private static boolean isEntityHeader(String line) {
        if (!isHeader(line)) return false;
        String section = line.substring(1, line.length() - 1).trim();
        return startsWith(section, "entity.");
    }

    private static boolean startsWith(String text, String prefix) {
        return text.length() >= prefix.length()
                && text.substring(0, prefix.length()).equals(prefix);
    }

    private static String readResourceText(String path) throws IOException {
        InputStream input = CustomEntitySet.class.getResourceAsStream(path);
        if (input == null) throw new IOException("Entity file not found: " + path);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        try {
            int bytes;
            while ((bytes = input.read(buffer)) != -1) {
                if (bytes > 0) output.write(buffer, 0, bytes);
            }
            return new String(output.toByteArray(), "UTF-8");
        } finally {
            try {
                input.close();
            } catch (IOException ignored) {
            }
        }
    }
}
