package mod.maxammus.serfs.util;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;

import java.util.HashMap;
import java.util.logging.Logger;

public class ReflectionUtility {
    private static final HashMap<String, String> classNames = new HashMap<>();
    private static final Logger logger = Logger.getLogger(ReflectionUtility.class.getName());
    private static ClassPool classPool;

    public static void init() {
        classPool = HookManager.getInstance().getClassPool();
        ReflectionUtility.mapClassName("com.wurmonline.server.players.Player");
        ReflectionUtility.mapClassName("com.wurmonline.server.Players");
        ReflectionUtility.mapClassName("mod.maxammus.serfs.creatures.Serf");
        ReflectionUtility.mapClassName("mod.maxammus.serfs.Serfs");
        ReflectionUtility.mapClassName("mod.maxammus.serfs.tasks.TaskHandler");
        ReflectionUtility.mapClassName("com.wurmonline.server.creatures.Creature");
        ReflectionUtility.mapClassName("mod.maxammus.serfs.items.SerfContract");
        ReflectionUtility.mapClassName("com.wurmonline.server.creatures.SerfCommunicator");
        ReflectionUtility.mapClassName("com.wurmonline.server.WurmId");
        ReflectionUtility.mapClassName("com.wurmonline.server.skills.Skills");
    }

    public static void mapClassName(String fullName) {
        classNames.put(fullName.substring(fullName.lastIndexOf(".") + 1), fullName);
    }

    public static String convertToFullClassNames(String src) {
        //Split src by words, keeping the delimiter to add back later.
        String[] words = src.split("((?<=\\W)|(?=\\W))");
        for (int i = 0; i < words.length; ++i){
            //Find any simple class names and replace them with the full name
            String replace = classNames.get(words[i]);
            if(replace != null)
                words[i] = replace;
        }
        return String.join("", words);
    }

    public static void replaceMethodCall(String className, String methodName, String desc, String target, String replace) throws NotFoundException, CannotCompileException {
        CtMethod ctMethod = getMethod(className, methodName, desc);
        ExprEditor exprEditor = getMethodCallReplacer(target, replace);
        ctMethod.instrument(exprEditor);
    }

    public static void replaceNewCall(String className, String methodName, String desc, String target, String targetDesc, String replace) throws NotFoundException, CannotCompileException {
        CtMethod ctMethod = getMethod(className, methodName, desc);
        ExprEditor exprEditor = getNewCallReplacer(target, targetDesc, replace);
        ctMethod.instrument(exprEditor);
    }
    public static void replaceFieldAccess(String className, String methodName, String desc, String target, String replace) throws NotFoundException, CannotCompileException {
        CtMethod ctMethod = getMethod(className, methodName, desc);
        ExprEditor exprEditor = getFieldAccessReplacer(target, replace);
        ctMethod.instrument(exprEditor);
    }

    private static CtMethod getMethod(String className, String methodName, String desc) throws NotFoundException {
        CtMethod ctMethod;
        if(desc == null)
            ctMethod = classPool.getMethod(className, methodName);
        else
            ctMethod = classPool.getCtClass(className).getMethod(methodName, desc);
        return ctMethod;
    }

    public static ExprEditor getNewCallReplacer(String target, String desc, String replace) {
        return new ExprEditor() {
            @Override
            public void edit(NewExpr c) throws CannotCompileException {
                //manually get the simple name of c
                if (c.getClassName().substring(c.getClassName().lastIndexOf('.') + 1).equals(target)
                        && (desc == null || c.getSignature().equals(desc)))
                    c.replace(ReflectionUtility.convertToFullClassNames(replace));
            }
        };
    }

    public static ExprEditor getMethodCallReplacer(String target, String replace) {
        return new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals(target))
                    m.replace(ReflectionUtility.convertToFullClassNames(replace));
            }
        };
    }

    public static ExprEditor getFieldAccessReplacer(String target, String replace) {
        return new ExprEditor() {
            @Override
            public void edit(FieldAccess fieldAccess) {
                try {
                    if (fieldAccess.getFieldName().equals(target))
                        fieldAccess.replace(ReflectionUtility.convertToFullClassNames(replace));
                } catch (CannotCompileException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
