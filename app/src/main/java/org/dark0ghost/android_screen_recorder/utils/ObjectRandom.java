package org.dark0ghost.android_screen_recorder.utils;

import org.dark0ghost.android_screen_recorder.interfaces.PRNG;

import java.util.Objects;

public class ObjectRandom implements PRNG {

    private int getAddress(){
        return Objects.hash(new Object());
    }

    @Override
    public int randomNumber(int begin, int end){
        int hash = getAddress();
        int difference = end - begin;
        return begin + (hash % difference);
    }
}
