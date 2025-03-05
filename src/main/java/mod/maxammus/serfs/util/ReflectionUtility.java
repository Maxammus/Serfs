package mod.maxammus.serfs.util;

import javassist.*;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Opcode;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

public class ReflectionUtility {
    private static HashMap<String, String> classNames = new HashMap<>();
    private static final Logger logger = Logger.getLogger(ReflectionUtility.class.getName());
    private static ClassPool classPool;

    public static void init() {
        classPool = HookManager.getInstance().getClassPool();
        ReflectionUtility.mapClassName("Player", "com.wurmonline.server.players.Player");
        ReflectionUtility.mapClassName("Players", "com.wurmonline.server.Players");
        ReflectionUtility.mapClassName("Serf", "mod.maxammus.serfs.creatures.Serf");
        ReflectionUtility.mapClassName("Serfs", "mod.maxammus.serfs.Serfs");
        ReflectionUtility.mapClassName("TaskHandler", "mod.maxammus.serfs.tasks.TaskHandler");
        ReflectionUtility.mapClassName("Creature", "com.wurmonline.server.creatures.Creature");
        ReflectionUtility.mapClassName("SerfContract", "mod.maxammus.serfs.items.SerfContract");
        ReflectionUtility.mapClassName("SerfCommunicator", "com.wurmonline.server.creatures.SerfCommunicator");
        ReflectionUtility.mapClassName("WurmId", "com.wurmonline.server.WurmId");
    }

    public static void mapClassName(String simpleName, String fullName) {
        classNames.put(simpleName, fullName);
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

    public static void replacePlayerWithCreatureInMethod(CtClass targetClass, String method) throws CannotCompileException {
        logger.info("Replacing Player types with Creature in the bytecode of: " + method);
        ConstPool constPool = targetClass.getClassFile().getConstPool();
        try {
            CtClass playerClass = classPool.getCtClass("com.wurmonline.server.players.Player");
            CtClass creatureClass = classPool.getCtClass("com.wurmonline.server.creatures.Creature");
            CtMethod ctMethodmethod = targetClass.getDeclaredMethod(method);
            CodeIterator codeIterator = ctMethodmethod.getMethodInfo().getCodeAttribute().iterator();
            while (codeIterator.hasNext()) {
                int index = codeIterator.next();
                int opcode = codeIterator.byteAt(index);
                switch (opcode) {
                    case Opcode.INVOKEVIRTUAL:
                    case Opcode.INVOKESPECIAL:
                    case Opcode.INVOKESTATIC:
                        //Replace calls to Player methods with calls to Creature methods.
                        int methodRefIndex = codeIterator.u16bitAt(index + 1);
                        String className = constPool.getMethodrefClassName(methodRefIndex);
                        if (className.equals(playerClass.getName())) {
                            int newMethodRefIndex = constPool.addMethodrefInfo(
                                    constPool.addClassInfo(creatureClass),
                                    constPool.getMethodrefName(methodRefIndex),
                                    constPool.getMethodrefType(methodRefIndex));
                            codeIterator.write16bit(newMethodRefIndex, index + 1);
                        }
                        break;
                    case Opcode.GETFIELD:
                    case Opcode.GETSTATIC:
                    case Opcode.PUTSTATIC:
                    case Opcode.PUTFIELD:
                        int fieldRef = codeIterator.u16bitAt(index + 1);
                        //EDIT: chose to go with replacing accesses to Communicator.player with SerfCommunicator.serf
                        //EDIT: as long as types are changed in bytecode, instead of this and creating/initializing a new field
                        //Replace Player type fields with a Creature type field
//                        if (constPool.getFieldrefType(fieldRef).equals(playerTypeDesc)) {
//                            codeIterator.write16bit(fieldIndex, index + 1);
//                        }
                        //Replace access to fields from Player with fields from Creature
                        if(constPool.getFieldrefClassName(fieldRef).equals(playerClass.getName())) {

                            int newFieldRef = constPool.addFieldrefInfo(
                                    constPool.addClassInfo(creatureClass),
                                    constPool.getFieldrefName(fieldRef),
                                    constPool.getFieldrefType(fieldRef));
                            codeIterator.write16bit(newFieldRef, index + 1);
                        }
                        break;
                }
            }
            ctMethodmethod.getMethodInfo().rebuildStackMap(classPool);
        } catch (NotFoundException | BadBytecode e) {
            throw new RuntimeException(e);
        }
    }
}
