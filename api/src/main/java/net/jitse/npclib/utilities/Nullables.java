package net.jitse.npclib.utilities;

import java.util.function.Consumer;

public final class Nullables {

    public static <T> void doForEveryPresent(Consumer<T> cons, T... objects) {
        for (T obj : objects) {
            if(obj != null) {
                cons.accept(obj);
            }
        }
    }

}
