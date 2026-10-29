package io.github.dirtyredz.roundwood.block.attachment;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlockEntity;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.archway.trim.ArchwayTrimNbt;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageData;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorSwing;
import io.github.dirtyredz.roundwood.block.door.trim.DoorTrimNbt;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlockEntity;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPlacement;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.glass.WindowGlassNbt;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.trim.WindowSillNbt;
import io.github.dirtyredz.roundwood.block.window.trim.WindowTrimActions;
import io.github.dirtyredz.roundwood.block.window.trim.WindowTrimNbt;
import io.github.dirtyredz.roundwood.registry.ModBlockEntities;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.ShowcaseWindow;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.anchorEntity;
import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.write;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.BRICK;

/**
 * Characterizes how the four anchor block entities (door, archway, window, compatibility frame) save,
 * sync and load, so the lifecycle can move into a shared base without changing behaviour: the exact
 * saved and update-tag key sets (default and styled), the default/malformed/incompatible appearance
 * rules on load, custody of unknown-version trim payloads (kept byte for byte, edits refused, clients
 * sent an explicit empty current-version payload, the window's two slots independent), the door's
 * camouflage (refund on disk only) and untouched swing, and the window's null-packet reading.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AnchorPersistenceGameTests {
    private static final BlockPos DOOR_AT = new BlockPos(1, 1, 1);
    private static final BlockPos ARCHWAY_AT = new BlockPos(1, 1, 4);
    private static final BlockPos FRAME_AT = new BlockPos(1, 1, 7);
    private static final BlockPos WINDOW_AT = new BlockPos(6, 2, 4);
    private static final Appearance BLUE = Appearance.DEFAULT.painted(DyeColor.BLUE);
    private static final Appearance SPRUCE = Appearance.wood(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "spruce_planks"));

    private AnchorPersistenceGameTests() {
    }

    // ---- placement ----

    private static RoundDoorBlockEntity door(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly assembly = io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.assembly(helper, DOOR_AT, Direction.NORTH);
        check(DoorPlacement.placeAt(helper.getLevel(), ModBlocks.roundDoor(), assembly, false), "door not placed");
        return io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.anchorEntity(helper, assembly);
    }

    private static ArchwayBlockEntity archway(GameTestHelper helper) {
        floor(helper);
        ArchwayLayout.Assembly assembly = write(helper, ARCHWAY_AT, Direction.NORTH, ArchwayDesign.KEYHOLE, Appearance.DEFAULT);
        return anchorEntity(helper, assembly);
    }

    private static RoundTrimBlockEntity frame(GameTestHelper helper) {
        floor(helper);
        TrimLayout.Assembly assembly = io.github.dirtyredz.roundwood.fixture.TrimFixtures.trimAssembly(helper, FRAME_AT, Direction.NORTH);
        check(TrimPlacement.placeAt(helper.getLevel(), ModBlocks.roundTrim(), assembly, Appearance.DEFAULT), "frame not placed");
        if (helper.getLevel().getBlockEntity(assembly.anchor()) instanceof RoundTrimBlockEntity entity) {
            return entity;
        }
        throw new net.minecraft.gametest.framework.GameTestAssertException("no frame entity at " + assembly.anchor());
    }

    private static BlockState windowState() {
        return ShowcaseWindow.of(WindowDesign.plain(WindowProfile.ARCH_LEFT), WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER),
                0, 0, 0, Appearance.DEFAULT).state();
    }

    private static RoundWindowBlockEntity window(GameTestHelper helper) {
        helper.setBlock(WINDOW_AT, windowState());
        return (RoundWindowBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(WINDOW_AT));
    }

    private static void assertKeys(String what, CompoundTag tag, String... expected) {
        Set<String> actual = new TreeSet<>(tag.getAllKeys());
        Set<String> wanted = new TreeSet<>(List.of(expected));
        check(actual.equals(wanted), what + ": keys " + actual + ", expected " + wanted);
    }

    private static CompoundTag future(String key, int version) {
        CompoundTag tag = new CompoundTag();
        CompoundTag payload = new CompoundTag();
        payload.putInt("Version", version);
        payload.putString("Mystery", "kept");
        tag.put(key, payload);
        return tag;
    }

    private static FaceTrims front(Appearance look) {
        return FaceTrims.NONE.with(AttachmentFace.FRONT, look);
    }

    // ---- (a) saved versus update key sets ----

    @GameTest(template = TEMPLATE)
    public static void doorKeySetsDefaultAndStyled(GameTestHelper helper) {
        RoundDoorBlockEntity door = door(helper);
        assertKeys("untrimmed default door saved", door.saveWithoutMetadata());
        assertKeys("untrimmed default door update", door.getUpdateTag(), io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareStore.TAG, io.github.dirtyredz.roundwood.block.door.frame.DoorArchStore.TAG, CamouflageData.TAG, DoorTrimNbt.TAG);
        door.setAppearance(BLUE);
        assertKeys("painted door saved", door.saveWithoutMetadata(), AppearanceNbt.TAG);
        assertKeys("painted door update", door.getUpdateTag(), io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareStore.TAG, io.github.dirtyredz.roundwood.block.door.frame.DoorArchStore.TAG, AppearanceNbt.TAG, CamouflageData.TAG, DoorTrimNbt.TAG);
        check(door.setTrims(front(BRICK)), "trims refused");
        door.setCamouflage(CamouflageData.free(Blocks.STONE.defaultBlockState()));
        assertKeys("styled door saved", door.saveWithoutMetadata(), AppearanceNbt.TAG, CamouflageData.TAG, DoorTrimNbt.TAG);
        assertKeys("styled door update", door.getUpdateTag(), io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareStore.TAG, io.github.dirtyredz.roundwood.block.door.frame.DoorArchStore.TAG, AppearanceNbt.TAG, CamouflageData.TAG, DoorTrimNbt.TAG);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void archwayKeySetsDefaultAndStyled(GameTestHelper helper) {
        ArchwayBlockEntity archway = archway(helper);
        assertKeys("untrimmed default archway saved", archway.saveWithoutMetadata());
        assertKeys("untrimmed default archway update", archway.getUpdateTag(), ArchwayTrimNbt.TAG);
        archway.setAppearance(BRICK);
        assertKeys("styled archway saved", archway.saveWithoutMetadata(), AppearanceNbt.TAG);
        check(archway.setTrims(front(SPRUCE)), "trims refused");
        assertKeys("trimmed archway saved", archway.saveWithoutMetadata(), AppearanceNbt.TAG, ArchwayTrimNbt.TAG);
        assertKeys("trimmed archway update", archway.getUpdateTag(), AppearanceNbt.TAG, ArchwayTrimNbt.TAG);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void windowKeySetsDefaultAndStyled(GameTestHelper helper) {
        RoundWindowBlockEntity window = window(helper);
        BlockPos abs = helper.absolutePos(WINDOW_AT);
        assertKeys("default window saved", window.saveWithoutMetadata(), WindowTrimNbt.TAG);
        check(window.saveWithoutMetadata().getCompound(WindowTrimNbt.TAG).getInt("Version") == 2
                && !window.saveWithoutMetadata().getCompound(WindowTrimNbt.TAG).contains("Front"), "empty perimeter payload v2");
        assertKeys("default window update", window.getUpdateTag(), WindowTrimNbt.TAG, WindowSillNbt.TAG, "WindowCrossBeams");
        check(WindowTrimActions.install(helper.getLevel(), abs, TrimSlot.SILL, AttachmentFace.FRONT, BRICK), "sill");
        assertKeys("sill window saved", window.saveWithoutMetadata(), WindowTrimNbt.TAG, WindowSillNbt.TAG);
        window.setAppearance(BLUE);
        window.setGlass(Optional.of(DyeColor.RED));
        assertKeys("styled window saved", window.saveWithoutMetadata(), AppearanceNbt.TAG, WindowGlassNbt.TAG, WindowTrimNbt.TAG, WindowSillNbt.TAG);
        assertKeys("styled window update", window.getUpdateTag(), AppearanceNbt.TAG, WindowGlassNbt.TAG, WindowTrimNbt.TAG, WindowSillNbt.TAG, "WindowCrossBeams");
        helper.setBlock(WINDOW_AT, Blocks.AIR);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void frameKeySetsDefaultAndStyled(GameTestHelper helper) {
        RoundTrimBlockEntity frame = frame(helper);
        assertKeys("default frame saved", frame.saveWithoutMetadata());
        assertKeys("default frame update", frame.getUpdateTag());
        frame.setAppearance(BRICK);
        assertKeys("styled frame saved", frame.saveWithoutMetadata(), AppearanceNbt.TAG);
        assertKeys("styled frame update", frame.getUpdateTag(), AppearanceNbt.TAG);
        helper.succeed();
    }

    // ---- (b) appearance on load ----

    /** One anchor type: its placed entity, a fresh twin, and how to read and set its look. */
    private record Anchor(String name, boolean takesMasonry, BlockEntity placed, Supplier<BlockEntity> fresh,
                          Function<BlockEntity, Appearance> look, BiConsumer<BlockEntity, Appearance> set) {
    }

    private static List<Anchor> anchors(GameTestHelper helper) {
        RoundDoorBlockEntity door = door(helper);
        ArchwayBlockEntity archway = archway(helper);
        RoundTrimBlockEntity frame = frame(helper);
        RoundWindowBlockEntity window = window(helper);
        return List.of(
                new Anchor("door", false, door,
                        () -> new RoundDoorBlockEntity(ModBlockEntities.roundDoor(), door.getBlockPos(), door.getBlockState()),
                        e -> ((RoundDoorBlockEntity) e).appearance(), (e, a) -> ((RoundDoorBlockEntity) e).setAppearance(a)),
                new Anchor("archway", true, archway,
                        () -> new ArchwayBlockEntity(ModBlockEntities.archway(), archway.getBlockPos(), archway.getBlockState()),
                        e -> ((ArchwayBlockEntity) e).appearance(), (e, a) -> ((ArchwayBlockEntity) e).setAppearance(a)),
                new Anchor("frame", true, frame,
                        () -> new RoundTrimBlockEntity(ModBlockEntities.roundTrim(), frame.getBlockPos(), frame.getBlockState()),
                        e -> ((RoundTrimBlockEntity) e).appearance(), (e, a) -> ((RoundTrimBlockEntity) e).setAppearance(a)),
                new Anchor("window", false, window,
                        () -> new RoundWindowBlockEntity(ModBlockEntities.roundWindow(), window.getBlockPos(), window.getBlockState()),
                        e -> ((RoundWindowBlockEntity) e).appearance(), (e, a) -> ((RoundWindowBlockEntity) e).setAppearance(a)));
    }

    private static void load(BlockEntity entity, CompoundTag tag) {
        entity.load(tag);
    }

    @GameTest(template = TEMPLATE)
    public static void missingMalformedAndIncompatibleAppearancesLoadAsTheDefault(GameTestHelper helper) {
        for (Anchor anchor : anchors(helper)) {
            Appearance def = anchor.look().apply(anchor.placed());
            check(def.equals(Appearance.DEFAULT), anchor.name() + ": placed default " + def);

            BlockEntity fresh = anchor.fresh().get();
            load(fresh, new CompoundTag());
            check(anchor.look().apply(fresh).equals(def), anchor.name() + ": no appearance did not read as the default");

            CompoundTag styled = new CompoundTag();
            AppearanceNbt.write(styled, BLUE);
            load(fresh, styled);
            check(anchor.look().apply(fresh).equals(BLUE), anchor.name() + ": a painted look was not read");
            load(fresh, new CompoundTag());
            check(anchor.look().apply(fresh).equals(def), anchor.name() + ": loading nothing kept the old look");

            CompoundTag bogusKind = new CompoundTag();
            CompoundTag compound = new CompoundTag();
            compound.putInt("Version", 1);
            compound.putString("Kind", "plasma");
            compound.putString("Material", "minecraft:oak_planks");
            bogusKind.put(AppearanceNbt.TAG, compound);
            load(fresh, styled);
            load(fresh, bogusKind);
            check(anchor.look().apply(fresh).equals(def), anchor.name() + ": unknown kind did not read as the default");

            CompoundTag wrongType = new CompoundTag();
            wrongType.putString(AppearanceNbt.TAG, "oak_planks");
            load(fresh, styled);
            load(fresh, wrongType);
            check(anchor.look().apply(fresh).equals(def), anchor.name() + ": a non-compound did not read as the default");

            CompoundTag emptyCompound = new CompoundTag();
            emptyCompound.put(AppearanceNbt.TAG, new CompoundTag());
            load(fresh, styled);
            load(fresh, emptyCompound);
            check(anchor.look().apply(fresh).equals(def), anchor.name() + ": an empty compound did not read as the default");

            CompoundTag masonry = new CompoundTag();
            AppearanceNbt.write(masonry, BRICK);
            load(fresh, masonry);
            check(anchor.look().apply(fresh).equals(anchor.takesMasonry() ? BRICK : def),
                    anchor.name() + ": masonry read as " + anchor.look().apply(fresh));

            anchor.set().accept(anchor.placed(), BLUE);
            check(anchor.placed().saveWithoutMetadata().contains(AppearanceNbt.TAG), anchor.name() + ": a painted look is not saved");
            anchor.set().accept(anchor.placed(), def);
            check(!anchor.placed().saveWithoutMetadata().contains(AppearanceNbt.TAG), anchor.name() + ": the default look was stored");
            check(!anchor.placed().getUpdateTag().contains(AppearanceNbt.TAG), anchor.name() + ": the default look was sent");
        }
        helper.setBlock(WINDOW_AT, Blocks.AIR);
        helper.succeed();
    }

    // ---- (c) unknown payload custody ----

    @GameTest(template = TEMPLATE)
    public static void doorKeepsAnUnknownTrimPayloadAndRefusesEdits(GameTestHelper helper) {
        RoundDoorBlockEntity door = door(helper);
        CompoundTag tag = future(DoorTrimNbt.TAG, 99);
        CompoundTag payload = tag.getCompound(DoorTrimNbt.TAG).copy();
        RoundDoorBlockEntity unknown = new RoundDoorBlockEntity(ModBlockEntities.roundDoor(), door.getBlockPos(), door.getBlockState());
        unknown.load(tag);
        check(unknown.trims().equals(FaceTrims.NONE) && !unknown.trimsEditable(), "an unknown payload was read or is editable");
        check(!unknown.setTrims(front(BRICK)), "an unknown payload accepted an edit");
        check(unknown.saveWithoutMetadata().getCompound(DoorTrimNbt.TAG).equals(payload), "the unknown payload was not kept");
        CompoundTag update = unknown.getUpdateTag().getCompound(DoorTrimNbt.TAG);
        check(update.getInt("Version") == DoorTrimNbt.VERSION && !update.contains("Front") && !update.contains("Back") && !update.contains("Mystery"),
                "clients were sent the unknown payload: " + update);
        unknown.load(new CompoundTag());
        check(unknown.trimsEditable() && unknown.setTrims(front(BRICK)), "loading a clean tag did not release custody");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void windowPerimeterAndSillCustodyAreIndependent(GameTestHelper helper) {
        RoundWindowBlockEntity base = window(helper);
        CompoundTag perimeterTag = future(WindowTrimNbt.TAG, 99);
        CompoundTag perimeterPayload = perimeterTag.getCompound(WindowTrimNbt.TAG).copy();
        RoundWindowBlockEntity unknownPerimeter = new RoundWindowBlockEntity(ModBlockEntities.roundWindow(), base.getBlockPos(), base.getBlockState());
        unknownPerimeter.load(perimeterTag);
        check(!unknownPerimeter.trimsEditable() && unknownPerimeter.trimsEditable(TrimSlot.SILL), "perimeter custody flags");
        check(!unknownPerimeter.trims().any(), "an unknown perimeter was read");
        check(!unknownPerimeter.setTrims(front(BRICK)) && !unknownPerimeter.setTrims(TrimSlot.PERIMETER, front(BRICK)), "perimeter edit accepted");
        check(unknownPerimeter.setTrims(TrimSlot.SILL, front(BRICK)), "sill edit refused beside an unknown perimeter");
        CompoundTag saved = unknownPerimeter.saveWithoutMetadata();
        check(saved.getCompound(WindowTrimNbt.TAG).equals(perimeterPayload), "the unknown perimeter payload was not kept");
        check(saved.getCompound(WindowSillNbt.TAG).getInt("Version") == WindowSillNbt.VERSION && saved.getCompound(WindowSillNbt.TAG).contains("Front"),
                "the sill was not saved beside an unknown perimeter");
        CompoundTag update = unknownPerimeter.getUpdateTag();
        check(update.getCompound(WindowTrimNbt.TAG).getInt("Version") == WindowTrimNbt.VERSION
                && !update.getCompound(WindowTrimNbt.TAG).contains("Front") && !update.getCompound(WindowTrimNbt.TAG).contains("Mystery"),
                "clients were sent the unknown perimeter payload");
        check(update.getCompound(WindowSillNbt.TAG).contains("Front"), "clients were not sent the sill");

        CompoundTag sillTag = future(WindowSillNbt.TAG, 99);
        CompoundTag sillPayload = sillTag.getCompound(WindowSillNbt.TAG).copy();
        RoundWindowBlockEntity unknownSill = new RoundWindowBlockEntity(ModBlockEntities.roundWindow(), base.getBlockPos(), base.getBlockState());
        unknownSill.load(sillTag);
        check(unknownSill.trimsEditable() && !unknownSill.trimsEditable(TrimSlot.SILL), "sill custody flags");
        check(!unknownSill.setTrims(TrimSlot.SILL, front(BRICK)), "sill edit accepted over an unknown payload");
        check(unknownSill.setTrims(TrimSlot.PERIMETER, front(SPRUCE)), "perimeter edit refused beside an unknown sill");
        saved = unknownSill.saveWithoutMetadata();
        check(saved.getCompound(WindowSillNbt.TAG).equals(sillPayload), "the unknown sill payload was not kept");
        check(saved.getCompound(WindowTrimNbt.TAG).contains("Front"), "the perimeter was not saved beside an unknown sill");
        update = unknownSill.getUpdateTag();
        check(update.getCompound(WindowSillNbt.TAG).getInt("Version") == WindowSillNbt.VERSION
                && !update.getCompound(WindowSillNbt.TAG).contains("Front") && !update.getCompound(WindowSillNbt.TAG).contains("Mystery"),
                "clients were sent the unknown sill payload");
        helper.setBlock(WINDOW_AT, Blocks.AIR);
        helper.succeed();
    }

    // ---- (d) independent slots ----

    @GameTest(template = TEMPLATE)
    public static void windowPerimeterAndSillStoresLoadAndSaveIndependently(GameTestHelper helper) {
        RoundWindowBlockEntity base = window(helper);
        FaceTrims perimeter = front(BRICK);
        FaceTrims sill = FaceTrims.NONE.with(AttachmentFace.BACK, SPRUCE);
        RoundWindowBlockEntity source = new RoundWindowBlockEntity(ModBlockEntities.roundWindow(), base.getBlockPos(), base.getBlockState());
        check(source.setTrims(TrimSlot.PERIMETER, perimeter) && source.setTrims(TrimSlot.SILL, sill), "setup");
        CompoundTag saved = source.saveWithoutMetadata();
        RoundWindowBlockEntity copy = new RoundWindowBlockEntity(ModBlockEntities.roundWindow(), base.getBlockPos(), base.getBlockState());
        copy.load(saved);
        check(copy.trims().equals(perimeter) && copy.trims(TrimSlot.SILL).equals(sill), "both slots round trip");

        CompoundTag perimeterOnly = saved.copy();
        perimeterOnly.remove(WindowSillNbt.TAG);
        copy.load(perimeterOnly);
        check(copy.trims().equals(perimeter) && !copy.trims(TrimSlot.SILL).any(), "a missing sill payload kept the old sill or lost the perimeter");

        CompoundTag sillOnly = new CompoundTag();
        sillOnly.put(WindowSillNbt.TAG, saved.getCompound(WindowSillNbt.TAG).copy());
        copy.load(sillOnly);
        check(!copy.trims().any() && copy.trims(TrimSlot.SILL).equals(sill), "a missing perimeter payload (state not trimmed) kept the old perimeter");
        check(copy.saveWithoutMetadata().getCompound(WindowTrimNbt.TAG).getInt("Version") == WindowTrimNbt.VERSION, "empty perimeter not written");
        helper.succeed();
    }

    // ---- (e) door camouflage and swing ----

    @GameTest(template = TEMPLATE)
    public static void doorCamouflageKeepsItsRefundOnDiskOnlyAndTheSwingSurvivesLoads(GameTestHelper helper) {
        RoundDoorBlockEntity door = door(helper);
        BlockState stone = Blocks.STONE.defaultBlockState();
        door.setCamouflage(CamouflageData.paid(stone, new ItemStack(Items.STONE)));
        check(door.saveWithoutMetadata().getCompound(CamouflageData.TAG).contains("Refund"), "disk camouflage lacks the refund");
        CompoundTag update = door.getUpdateTag().getCompound(CamouflageData.TAG);
        check(update.contains("Material") && !update.contains("Refund"), "update camouflage: " + update);

        RoundDoorBlockEntity copy = new RoundDoorBlockEntity(ModBlockEntities.roundDoor(), door.getBlockPos(), door.getBlockState());
        copy.load(door.saveWithoutMetadata());
        check(copy.camouflage().map(CamouflageData::paid).orElse(false), "the refund did not survive a save");
        copy.load(door.getUpdateTag());
        check(copy.camouflage().isPresent() && !copy.camouflage().get().paid(), "a client copy kept a refund");

        Optional<CamouflageData> taken = door.takeCamouflage();
        check(taken.isPresent() && taken.get().paid() && door.camouflage().isEmpty(), "takeCamouflage did not return and clear");
        check(!door.saveWithoutMetadata().contains(CamouflageData.TAG), "a taken camouflage was still saved");
        check(door.getUpdateTag().getCompound(CamouflageData.TAG).isEmpty() && door.getUpdateTag().contains(CamouflageData.TAG),
                "a cleared camouflage is not an explicit empty compound");
        check(door.takeCamouflage().isEmpty(), "a second take returned something");
        copy.load(door.getUpdateTag());
        check(copy.camouflage().isEmpty(), "the clear did not reach the client copy");

        DoorSwing swing = copy.swing();
        swing.retarget(true, 100);
        check(swing.progress(1000) == 1, "setup swing");
        copy.load(door.saveWithoutMetadata());
        copy.load(door.getUpdateTag());
        copy.load(new CompoundTag());
        check(copy.swing() == swing && swing.progress(1000) == 1 && !swing.moving(1000), "a load replaced or disturbed the swing");
        helper.succeed();
    }

    // ---- (f) window null packet ----

    @GameTest(template = TEMPLATE)
    public static void windowNullPacketClearsTrimsButAMissingPayloadReadsTheLegacyState(GameTestHelper helper) {
        RoundWindowBlockEntity base = window(helper);
        BlockState trimmed = base.getBlockState().setValue(WindowPaneBlock.TRIMMED, true);

        RoundWindowBlockEntity viaPacket = new RoundWindowBlockEntity(ModBlockEntities.roundWindow(), base.getBlockPos(), trimmed);
        check(viaPacket.setTrims(front(BRICK)) && viaPacket.setTrims(TrimSlot.SILL, front(BRICK)), "setup");
        // An empty tag is how a packet without a tag is made: the packet stores it as null.
        ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(viaPacket, entity -> new CompoundTag());
        check(packet.getTag() == null, "setup: the packet carries a tag");
        viaPacket.onDataPacket(null, packet);
        check(viaPacket.trims().equals(FaceTrims.NONE) && viaPacket.trims(TrimSlot.SILL).equals(FaceTrims.NONE),
                "a null packet kept or invented trims: " + viaPacket.trims() + " / " + viaPacket.trims(TrimSlot.SILL));

        RoundWindowBlockEntity explicit = new RoundWindowBlockEntity(ModBlockEntities.roundWindow(), base.getBlockPos(), trimmed);
        CompoundTag emptyPayload = new CompoundTag();
        WindowTrimNbt.write(emptyPayload, FaceTrims.NONE);
        explicit.load(emptyPayload);
        check(explicit.trims().equals(FaceTrims.NONE), "an explicit empty perimeter read as trims on a trimmed state");

        RoundWindowBlockEntity legacy = new RoundWindowBlockEntity(ModBlockEntities.roundWindow(), base.getBlockPos(), trimmed);
        legacy.load(new CompoundTag());
        check(legacy.trims().equals(FaceTrims.both(Appearance.DEFAULT)), "the 0.7.0 reading of a trimmed state: " + legacy.trims());
        check(!legacy.trims(TrimSlot.SILL).any(), "the legacy reading invented a sill");

        RoundWindowBlockEntity untrimmedLegacy = new RoundWindowBlockEntity(ModBlockEntities.roundWindow(), base.getBlockPos(), base.getBlockState());
        untrimmedLegacy.load(new CompoundTag());
        check(untrimmedLegacy.trims().equals(FaceTrims.NONE), "a missing payload on an untrimmed state read as trims");
        helper.setBlock(WINDOW_AT, Blocks.AIR);
        helper.succeed();
    }

    // ---- (g) failed writes ----

    @GameTest(template = TEMPLATE)
    public static void refusedTrimWritesLeaveTheSavedPayloadUnchanged(GameTestHelper helper) {
        RoundDoorBlockEntity door = door(helper);
        ArchwayBlockEntity archway = archway(helper);
        RoundWindowBlockEntity window = window(helper);

        door.load(future(DoorTrimNbt.TAG, 99));
        CompoundTag doorBefore = door.saveWithoutMetadata();
        check(!door.setTrims(front(BRICK)) && !door.setTrims(FaceTrims.NONE), "door accepted a write");
        check(door.saveWithoutMetadata().equals(doorBefore) && door.trims().equals(FaceTrims.NONE), "a refused door write changed the entity");

        archway.load(future(ArchwayTrimNbt.TAG, 99));
        CompoundTag archwayBefore = archway.saveWithoutMetadata();
        check(!archway.setTrims(front(BRICK)), "archway accepted a write");
        check(archway.saveWithoutMetadata().equals(archwayBefore), "a refused archway write changed the entity");

        CompoundTag windowTag = future(WindowTrimNbt.TAG, 99);
        windowTag.put(WindowSillNbt.TAG, future(WindowSillNbt.TAG, 99).getCompound(WindowSillNbt.TAG));
        window.load(windowTag);
        CompoundTag windowBefore = window.saveWithoutMetadata();
        check(!window.setTrims(front(BRICK)) && !window.setTrims(TrimSlot.SILL, front(BRICK)), "window accepted a write");
        check(!WindowTrimActions.install(helper.getLevel(), helper.absolutePos(WINDOW_AT), TrimSlot.SILL, AttachmentFace.FRONT, BRICK),
                "the production install fitted over an unknown payload");
        check(window.saveWithoutMetadata().equals(windowBefore), "a refused window write changed the entity");
        helper.setBlock(WINDOW_AT, Blocks.AIR);
        helper.succeed();
    }
}
