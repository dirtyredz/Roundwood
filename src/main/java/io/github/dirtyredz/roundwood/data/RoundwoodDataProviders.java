package io.github.dirtyredz.roundwood.data;

import io.github.dirtyredz.roundwood.data.client.DoorItemModelProvider;
import io.github.dirtyredz.roundwood.data.client.DoorModelProvider;
import io.github.dirtyredz.roundwood.data.client.RoundwoodLanguageProvider;
import io.github.dirtyredz.roundwood.data.client.TrimItemModelProvider;
import io.github.dirtyredz.roundwood.data.client.TrimModelProvider;
import io.github.dirtyredz.roundwood.data.client.WindowItemModelProvider;
import io.github.dirtyredz.roundwood.data.client.WindowModelProvider;
import io.github.dirtyredz.roundwood.data.server.RoundwoodBlockTagsProvider;
import io.github.dirtyredz.roundwood.data.server.RoundwoodItemTagsProvider;
import io.github.dirtyredz.roundwood.data.server.RoundwoodLootProvider;
import io.github.dirtyredz.roundwood.data.server.RoundwoodRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

/** Registers the data providers for {@code runData}. Nothing here runs in a normal game. */
public final class RoundwoodDataProviders {
    private RoundwoodDataProviders() {
    }

    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFiles = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new DoorModelProvider(output, existingFiles));
        generator.addProvider(event.includeClient(), new WindowModelProvider(output, existingFiles));
        generator.addProvider(event.includeClient(), new TrimModelProvider(output, existingFiles));
        generator.addProvider(event.includeClient(), new DoorItemModelProvider(output, existingFiles));
        generator.addProvider(event.includeClient(), new WindowItemModelProvider(output, existingFiles));
        generator.addProvider(event.includeClient(), new TrimItemModelProvider(output, existingFiles));
        generator.addProvider(event.includeClient(), new RoundwoodLanguageProvider(output));

        generator.addProvider(event.includeServer(), new RoundwoodRecipeProvider(output));
        generator.addProvider(event.includeServer(), RoundwoodLootProvider.create(output));
        RoundwoodBlockTagsProvider blockTags = generator.addProvider(event.includeServer(),
                new RoundwoodBlockTagsProvider(output, lookup, existingFiles));
        generator.addProvider(event.includeServer(),
                new RoundwoodItemTagsProvider(output, lookup, blockTags.contentsGetter(), existingFiles));
    }
}
