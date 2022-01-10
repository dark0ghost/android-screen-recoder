package org.dark0ghost.android_screen_recorder.utils;

import org.dark0ghost.android_screen_recorder.base.BaseGetHashWithObjectAndSalt;
import org.dark0ghost.android_screen_recorder.interfaces.Prng;

public class ObjectRandom extends BaseGetHashWithObjectAndSalt implements Prng {

    @Override
    public  int randomNumber(int begin, int end){
        int hash = getAddressPlusTime();
        int difference = end - begin;
        return begin + (hash % difference);
    }
}
