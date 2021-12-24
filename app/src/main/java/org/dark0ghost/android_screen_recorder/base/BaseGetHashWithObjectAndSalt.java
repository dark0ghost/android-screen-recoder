package org.dark0ghost.android_screen_recorder.base;

import java.util.Objects;

public abstract class BaseGetHashWithObjectAndSalt {
    protected static int getAddressPlusTime() {
        return (int) (Objects.hash(new Object()) + System.currentTimeMillis());
    }
}
