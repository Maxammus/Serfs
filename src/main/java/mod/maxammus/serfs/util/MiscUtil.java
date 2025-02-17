package mod.maxammus.serfs.util;

public class MiscUtil {
    public static int min(int... vals)
    {
        int min = vals[0];
        for(int test : vals)
            if(test < min)
                min = test;
        return min;
    }
}
