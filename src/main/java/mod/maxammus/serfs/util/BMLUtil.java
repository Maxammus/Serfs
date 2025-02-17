package mod.maxammus.serfs.util;

import com.wurmonline.server.utils.BMLBuilder;

public class BMLUtil {
    public static BMLBuilder openBracket(BMLBuilder bmlBuilder, final String type) {
        bmlBuilder.openBracket(type);
        return bmlBuilder;
    }
}
