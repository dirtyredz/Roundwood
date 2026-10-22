package io.github.dirtyredz.roundwood.showcase.section.door;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageData;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseSection;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseSamples.vanillaMasonry;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseSamples.vanillaWood;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The 1 x 2 arched door (0.11.0), five doors per row with a sign in front of each, rows running
 * away from the builder: oak closed and open on both hinges, plus a closed door beside a lever
 * (redstone); other woods and a paint; and the Universal Trim's jamb-and-crown casings (front only,
 * different front and back, on an open door) and the upper-corner camouflage. The section lists
 * every door ({@link #doors}) so building and the GameTest check read the same list. Occupies
 * {@code r 40..49, f 0..7}.
 */
public final class ArchedDoorsSection implements ShowcaseSection {
    private static final int R0 = 40;
    private static final int PITCH = 2;
    private static final int[] ROW_F = {1, 4, 7};
    /** The redstone door is the last of the first row; its lever stands on its right. */
    public static final int LEVER_R = R0 + 4 * PITCH + 1;

    /**
     * One exhibited door: its anchor at ({@code r}, 0, {@code f}), hinge, open state, leaf look, face
     * trims, camouflage material (or none) and sign text.
     */
    public record Exhibit(int r, int f, DoorHingeSide hinge, boolean open, Appearance look, FaceTrims trims,
                          Optional<Block> camouflage, String label) {
    }

    @Override
    public String id() {
        return "arched doors";
    }

    /** Every door of the section, in building order. */
    public static List<Exhibit> doors() {
        Appearance oak = Appearance.DEFAULT;
        Appearance spruce = vanillaWood("spruce_planks");
        Appearance darkOak = vanillaWood("dark_oak_planks");
        Appearance cherry = vanillaWood("cherry_planks");
        Appearance birch = vanillaWood("birch_planks");
        Appearance blue = oak.painted(DyeColor.BLUE);
        Appearance stoneBrick = vanillaMasonry("stone_bricks");
        Appearance deepslate = vanillaMasonry("deepslate_bricks");
        Appearance mudBrick = vanillaMasonry("mud_bricks");
        List<Exhibit> out = new ArrayList<>();
        out.add(exhibit(0, 0, DoorHingeSide.LEFT, false, oak, FaceTrims.NONE, null, "hinge left"));
        out.add(exhibit(0, 1, DoorHingeSide.RIGHT, false, oak, FaceTrims.NONE, null, "hinge right"));
        out.add(exhibit(0, 2, DoorHingeSide.LEFT, true, oak, FaceTrims.NONE, null, "open, left"));
        out.add(exhibit(0, 3, DoorHingeSide.RIGHT, true, oak, FaceTrims.NONE, null, "open, right"));
        out.add(exhibit(0, 4, DoorHingeSide.LEFT, false, oak, FaceTrims.NONE, null, "flip the lever"));
        out.add(exhibit(1, 0, DoorHingeSide.LEFT, false, spruce, FaceTrims.NONE, null, "spruce"));
        out.add(exhibit(1, 1, DoorHingeSide.LEFT, false, darkOak, FaceTrims.NONE, null, "dark oak"));
        out.add(exhibit(1, 2, DoorHingeSide.LEFT, false, cherry, FaceTrims.NONE, null, "cherry"));
        out.add(exhibit(1, 3, DoorHingeSide.LEFT, false, birch, FaceTrims.NONE, null, "birch"));
        out.add(exhibit(1, 4, DoorHingeSide.LEFT, false, blue, FaceTrims.NONE, null, "blue paint"));
        out.add(exhibit(2, 0, DoorHingeSide.LEFT, false, oak, front(oak, Optional.empty()), null, "oak trim"));
        out.add(exhibit(2, 1, DoorHingeSide.RIGHT, false, spruce, front(stoneBrick, Optional.of(deepslate)), null,
                "stone/deepslate"));
        out.add(exhibit(2, 2, DoorHingeSide.LEFT, true, darkOak, front(mudBrick, Optional.of(blue)), null, "open, trimmed"));
        out.add(exhibit(2, 3, DoorHingeSide.LEFT, false, oak, FaceTrims.NONE, Blocks.STONE_BRICKS, "camouflage"));
        out.add(exhibit(2, 4, DoorHingeSide.RIGHT, false, spruce, front(stoneBrick, Optional.empty()), Blocks.STONE_BRICKS,
                "camouflage, trim"));
        return List.copyOf(out);
    }

    @Override
    public void build(ShowcaseCanvas canvas) {
        for (Exhibit door : doors()) {
            canvas.sign(door.r(), door.f() - 1, "Arched door", door.label());
            if (!canvas.door(ModBlocks.archedDoor(), door.look(), door.r(), door.f(), door.hinge(), door.open())) {
                continue;
            }
            for (AttachmentFace face : AttachmentFace.values()) {
                door.trims().get(face).ifPresent(look -> canvas.doorTrim(look, door.r(), door.f(), face));
            }
            door.camouflage().ifPresent(material -> {
                if (canvas.level().getBlockEntity(canvas.pos(door.r(), 0, door.f())) instanceof RoundDoorBlockEntity entity) {
                    entity.setCamouflage(CamouflageData.free(material.defaultBlockState()));
                } else {
                    canvas.problem("arched door camouflage not applied at " + canvas.pos(door.r(), 0, door.f()).toShortString());
                }
            });
        }
        canvas.set(LEVER_R, 0, ROW_F[0], Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.FACING, canvas.frame().forward()));
    }

    /** The redstone door: the first row's last. */
    public static Exhibit redstoneDoor() {
        return doors().get(4);
    }

    private static Exhibit exhibit(int row, int index, DoorHingeSide hinge, boolean open, Appearance look, FaceTrims trims,
                                   Block camouflage, String label) {
        return new Exhibit(R0 + PITCH * index, ROW_F[row], hinge, open, look, trims, Optional.ofNullable(camouflage), label);
    }

    private static FaceTrims front(Appearance front, Optional<Appearance> back) {
        return new FaceTrims(Optional.of(front), back);
    }
}
