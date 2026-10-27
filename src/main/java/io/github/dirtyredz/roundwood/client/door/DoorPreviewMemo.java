package io.github.dirtyredz.roundwood.client.door;

import java.util.Objects;
import java.util.function.Supplier;

/** One current target, including misses. The caller supplies the complete live validation key. */
final class DoorPreviewMemo<K,V> {
    private K key;
    private V value;
    V get(K next,Supplier<V> evaluate) {
        if(value==null || !Objects.equals(key,next)){value=evaluate.get();key=next;}
        return value;
    }
    void clear(){key=null;value=null;}
}
