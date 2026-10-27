package io.github.dirtyredz.roundwood.block.door.hardware;

/** Assembly-wide groups. Tint markers preserve identity through baking despite the shared brass sprite. */
public enum DoorHardwareSlot {
    HINGES("hinges",1), KNOBS("knobs",2);
    private final String mesh;
    private final int marker;
    DoorHardwareSlot(String mesh,int marker){this.mesh=mesh;this.marker=marker;}
    public String mesh(){return mesh;}
    public int marker(){return marker;}
    public static int marker(String mesh){for(var slot:values()) if(slot.mesh.equals(mesh)) return slot.marker;return -1;}
}
