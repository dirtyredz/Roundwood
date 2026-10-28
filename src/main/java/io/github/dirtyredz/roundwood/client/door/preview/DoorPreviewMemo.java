package io.github.dirtyredz.roundwood.client.door.preview;

import java.util.Objects;
import java.util.function.Supplier;

/** One current target, including misses. The caller supplies the complete live validation key. */
public final class DoorPreviewMemo<K,V> {
    private K key;
    private V value;
    public V get(K next,Supplier<V> evaluate) {
        if(value==null || !Objects.equals(key,next)){value=evaluate.get();key=next;}
        return value;
    }
    public void clear(){key=null;value=null;}
}
