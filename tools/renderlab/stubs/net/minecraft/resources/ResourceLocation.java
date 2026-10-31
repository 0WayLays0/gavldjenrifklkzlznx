package net.minecraft.resources;

/** Render lab stand-in for Minecraft's ResourceLocation. */
public final class ResourceLocation {
    private final String namespace;
    private final String path;

    private ResourceLocation(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    public static ResourceLocation fromNamespaceAndPath(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    public static ResourceLocation withDefaultNamespace(String path) {
        return new ResourceLocation("minecraft", path);
    }

    public static ResourceLocation parse(String id) {
        int i = id.indexOf(':');
        return i < 0 ? withDefaultNamespace(id) : new ResourceLocation(id.substring(0, i), id.substring(i + 1));
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
