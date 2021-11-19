package org.dark0ghost.android_screen_recorder.utils;

import org.dark0ghost.android_screen_recorder.interfaces.Prng;
import java.util.Objects;

public class ObjectRandom implements Prng {

    private static int getAddress(){
        return (int) (Objects.hash(new Object()) + System.currentTimeMillis());
    }

    @Override
    public  int randomNumber(int begin, int end){
        int hash = getAddress();
        int difference = end - begin;
        return begin + (hash % difference);
    }
}
