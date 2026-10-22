package io.github.dirtyredz.roundwood.data.client;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.client.door.DoorLeafModels;
import io.github.dirtyredz.roundwood.client.trim.TrimModelNames;
import io.github.dirtyredz.roundwood.data.client.window.WindowModelProvider;
import io.github.dirtyredz.roundwood.migration.LegacyContent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * Compatibility resources for the 49 hidden 0.4.0 aliases, kept apart from the canonical providers:
 * each alias blockstate points at its family's canonical models (empty door and trim world models,
 * the plain round window's multipart: every position and axis, with one part per arm combination, via
 * {@code WindowModelProvider.panes}; trims are not in the models) and each alias item model is the
 * family's canonical item model. Their historical looks come from the block's and item's default appearance at runtime, so
 * no per-finish model exists any more.
 */
public class LegacyModelProvider extends BlockStateProvider {
    public LegacyModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, Roundwood.MOD_ID, existingFiles);
    }

    @Override
    public String getName() {
        return "Roundwood 0.4.0 alias blockstates and item models";
    }

    @Override
    protected void registerStatesAndModels() {
        ModelFile door = existing("block/" + DoorLeafModels.DOOR_ID);
        for (Block block : LegacyContent.doors()) {
            getVariantBuilder(block).partialState().setModels(new ConfiguredModel(door));
            item(block, DoorLeafModels.DOOR_ID);
        }
        WindowDesign round = WindowDesign.plain(WindowProfile.ROUND);
        for (Block block : LegacyContent.windows()) {
            WindowModelProvider.panes(this, block, round);
            item(block, round.id());
        }
        ModelFile trim = existing("block/" + TrimModelNames.TRIM_ID);
        for (Block block : LegacyContent.trims()) {
            getVariantBuilder(block).partialState().setModels(new ConfiguredModel(trim));
            item(block, TrimModelNames.TRIM_ID);
        }
    }

    private void item(Block block, String canonicalItem) {
        itemModels().getBuilder(BuiltInRegistries.BLOCK.getKey(block).getPath()).parent(existing("item/" + canonicalItem));
    }

    private ModelFile existing(String path) {
        return new ModelFile.UncheckedModelFile(modLoc(path));
    }
}
