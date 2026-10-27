package io.github.dirtyredz.roundwood.testsupport;

import java.net.URLClassLoader;
import java.nio.file.Path;

/** Optional local-jar API/class-load check; never starts a client or initializes a renderer. */
public final class EmbeddiumClassLoad {
    public static void main(String[] args) throws Exception {
        try (var loader = new URLClassLoader(new java.net.URL[]{Path.of(args[0]).toUri().toURL()}, EmbeddiumClassLoad.class.getClassLoader())) {
            var renderer = Class.forName("me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", false, loader);
            var model = Class.forName("io.github.dirtyredz.roundwood.client.render.StaticWorldModel", false, loader);
            Class.forName("net.minecraftforge.client.model.data.ModelData", false, loader);
            System.out.println("Class-load OK (initialization disabled): " + renderer.getName() + "; " + model.getName());
        }
    }
}
