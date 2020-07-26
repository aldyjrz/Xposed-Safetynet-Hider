package com.TMD.xposed.hider;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import top.fols.box.io.FilterXpInputStream;

import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * helpful link: https://github.com/w568w/XposedChecker
 */

public class XposedHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    String[] act, cmd1, key1, lib1, pkg1;
    private HashSet<String> commandSet;
    private Set<String> keywordSet;

    private HashSet appSet, activity, libnameSet;

    private String mSdcard;

    public XC_MethodHook opHook, finishOpHook;
    private Activity currentActivity;
    private Context systemContext;

    public void initZygote(IXposedHookZygoteInit.StartupParam paramStartupParam)
            throws Throwable {
        findAndHookMethod(Instrumentation.class, "newActivity", ClassLoader.class, String.class, Intent.class, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam paramAnonymousMethodHookParam)
                    throws Throwable {
                currentActivity = (Activity) paramAnonymousMethodHookParam.getResult();
             }
        });
        findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam paramAnonymousMethodHookParam)
                    throws Throwable {
                if ((currentActivity != null) &&  (currentActivity.getPackageName().equals("com.gojek.driver.bike"))) {
                     currentActivity.getWindow().addFlags(FLAG_KEEP_SCREEN_ON);

                 }
            }
        });

        if (Build.VERSION.SDK_INT >= 23) {
            opHook = new XC_MethodHook() {
                @SuppressLint({"InlinedApi"})
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam paramAnonymousMethodHookParam)
                        throws Throwable {
                    Object localObject = paramAnonymousMethodHookParam.args[0];
                    if ((localObject.equals(58)) || (localObject.equals("android:mock_location"))) {
                        paramAnonymousMethodHookParam.setResult(0);
                     }
                }
            };
            finishOpHook = new XC_MethodHook() {
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam paramAnonymousMethodHookParam)
                        throws Throwable {
                    Object localObject = paramAnonymousMethodHookParam.args[0];
                    if ((localObject.equals(58)) || (localObject.equals("android:mock_location"))) {
                        paramAnonymousMethodHookParam.setResult(null);
                     }
                }
            };

        }
    }

    private String[] buildGrepArraySingle(String[] original, boolean addSH) {
        StringBuilder builder = new StringBuilder();
        ArrayList<String> originalList = new ArrayList<String>();
        if (addSH) {
            originalList.add("sh");
            originalList.add("-c");

        }
        for (String temp : original) {
            builder.append(" ");
            builder.append(temp);
        }
        //originalList.addAll(Arrays.asList(original));
        // ***TODO: Switch to using -e with alternation***
        for (String temp : keywordSet) {
            builder.append(" | grep -v ");
            builder.append(temp);
        }
        //originalList.addAll(Common.DEFAULT_GREP_ENTRIES);
        originalList.add(builder.toString());
        return originalList.toArray(new String[0]);
    }

    private void bypassFa(XC_LoadPackage.LoadPackageParam lpparam)
    {
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getInstalledPackages", Integer.TYPE, new a());
        findAndHookMethod("android.os.SystemProperties", lpparam.classLoader, "get", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                if (param.args[0].equals("ro.build.selinux")) {
                    param.setResult("0");

                }
                String a = param.args[0].toString();
                if (a.contains("selinux")) {
                    param.setResult("0");
                }

            }

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                if (param.args[0].equals("ro.build.selinux")) {
                    param.setResult("0");

                }
            }

        });

    }


    public boolean stringContainsFromSet(String base, Set<String> values) {
        if (base != null && values != null) {
            for (String tempString : values) {
                if (base.matches(".*(\\W|^)" + tempString + "(\\W|$).*")) {
                    return true;
                }
            }
        }

        return false;
    }

    private void initFile(XC_LoadPackage.LoadPackageParam lpparam)
    {
        XposedBridge.hookMethod(XposedHelpers.findConstructorExact(File.class, String.class), new XC_MethodHook(10000)
        {
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam paramAnonymousMethodHookParam)
                    throws Throwable
            {
                if (((String)paramAnonymousMethodHookParam.args[0]).contains("su"))
                {
                    paramAnonymousMethodHookParam.args[0] = "";
                    return;
                }
                if (((String)paramAnonymousMethodHookParam.args[0]).contains("busybox"))
                {
                    paramAnonymousMethodHookParam.args[0] = "";
                    return;
                }
                if (((String)paramAnonymousMethodHookParam.args[0]).contains("magisk"))
                {
                    paramAnonymousMethodHookParam.args[0] = "";
                    return;
                }
                if (((String)paramAnonymousMethodHookParam.args[0]).contains("xposed"))
                {
                    paramAnonymousMethodHookParam.args[0] = "";
                    return;
                }
                if (stringContainsFromSet((String)paramAnonymousMethodHookParam.args[0], keywordSet)) {
                    paramAnonymousMethodHookParam.args[0] = "";
                }
            }
        });
    }

    private Boolean anyWordEndingWithKeyword(String paramString, String[] paramArrayOfString)
    {
        int j = paramArrayOfString.length;
        int i = 0;
        while (i < j)
        {
            if (paramArrayOfString[i].endsWith(paramString)) {
                return Boolean.TRUE;
            }
            i += 1;
        }
        return Boolean.FALSE;
    }

    public boolean stringEndsWithFromSet(String base, Set<String> values) {
        if (base != null && values != null) {
            for (String tempString : values) {
                if (base.endsWith(tempString)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void initRuntime(final XC_LoadPackage.LoadPackageParam lpparam) {
        /**
         * Hooks exec() within java.lang.Runtime.
         * is the only version that needs to be hooked, since all of the others are "convenience" variations.
         * takes the form: exec(String[] cmdarray, String[] envp, File dir).
         * There are a lot of different ways that exec can be used to check for a rooted device. See the comments within section for more details.
         */
        findAndHookMethod("java.lang.Runtime", lpparam.classLoader, "exec", String[].class, String[].class, File.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {


                String[] execArray = (String[]) param.args[0]; // Grab the tokenized array of commands
                if ((execArray != null) && (execArray.length >= 1)) { // Do some checking so we don't break anything
                    String firstParam = execArray[0]; // firstParam is going to be the main command/program being run

                    String tempString = "Exec Command:";
                    for (String temp : execArray) {
                        tempString = tempString + " " + temp;
                    }


                    if (stringEndsWithFromSet(firstParam, commandSet)) { // Check if the firstParam is one of the keywords we want to filter


                        // A bunch of logic follows since the solution depends on which command is being called
                        // TODO: ***Clean up logic***
                        if (firstParam.equals("su") || firstParam.endsWith("/su")) { // If its su or ends with su (/bin/su, /xbin/su, etc)
                            param.setThrowable(new IOException()); // Throw an exception to imply the command was not found
                        } else if (commandSet.contains("pm") && (firstParam.equals("pm") || firstParam.endsWith("/pm"))) {
                            // Trying to run the pm (package manager) using exec. Now let's deal with the subcases
                            if (execArray.length >= 3 && execArray[1].equalsIgnoreCase("list") && execArray[2].equalsIgnoreCase("packages")) {
                                // Trying to list out all of the packages, so we will filter out anything that matches the keywords
                                //param.args[0] = new String[] {"pm", "list", "packages", "-v", "grep", "-v", "\"su\""};
                                param.args[0] = buildGrepArraySingle(execArray, true);
                            } else if (execArray.length >= 3 && (execArray[1].equalsIgnoreCase("dump") || execArray[1].equalsIgnoreCase("path"))) {
                                // Trying to either dump package info or list the path to the APK (both will tell the app that the package exists)
                                // If it matches anything in the keywordSet, stop it from working by using a fake package name
                                if (stringContainsFromSet(execArray[2], keywordSet)) {
                                    param.args[0] = new String[]{execArray[0], execArray[1], ""};
                                }
                            }
                        } else if (commandSet.contains("ps") && (firstParam.equals("ps") || firstParam.endsWith("/ps"))) { // is a process list command
                            // Trying to run the ps command to see running processes (e.g. looking for things running as su or daemonsu). Filter out.
                            param.args[0] = buildGrepArraySingle(execArray, true);
                        } else if (commandSet.contains("which") && (firstParam.equals("which") || firstParam.endsWith("/which"))) {
                            // Busybox "which" command. Thrown an excepton
                            param.setThrowable(new IOException());
                        } else if (commandSet.contains("busybox") && anyWordEndingWithKeyword("busybox", execArray)) {
                            param.setThrowable(new IOException());
                        } else {
                            param.setThrowable(new IOException());
                        }

                        if (param.getThrowable() == null) { // Print out the new command if debugging is on
                            tempString = "New Exec Command:";
                            for (String temp : (String[]) param.args[0]) {
                                tempString = tempString + " " + temp;
                            }
                        }
                    }


                }
            }
        });
        findAndHookMethod("java.lang.Runtime", lpparam.classLoader, "loadLibrary", String.class, ClassLoader.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam paramAnonymousMethodHookParam)
                    throws Throwable {
                String str = (String) paramAnonymousMethodHookParam.args[0];
                if ((str != null) && (stringContainsFromSet(str, libnameSet))) {
                    paramAnonymousMethodHookParam.setResult("null");
                }
            }
        });
    }



    private void bacod()
    {
        findAndHookMethod("java.security.MessageDigest", null, "isEqual", byte[].class, byte[].class, new XC_MethodHook()
        {
            protected void afterHookedMethod(MethodHookParam paramAnonymousMethodHookParam)
            {
                paramAnonymousMethodHookParam.setResult(Boolean.TRUE);
            }
        });
        findAndHookMethod("java.security.Signature", null, "verify", byte[].class, new XC_MethodHook()
        {
            protected void afterHookedMethod(MethodHookParam paramAnonymousMethodHookParam)
                    throws Throwable
            {
                paramAnonymousMethodHookParam.setResult(Boolean.TRUE);
            }
        });
        findAndHookMethod("java.security.Signature", null, "verify", byte[].class, Integer.TYPE, Integer.TYPE, new XC_MethodHook()
        {
            protected void afterHookedMethod(MethodHookParam paramAnonymousMethodHookParam)
                    throws Throwable
            {
                paramAnonymousMethodHookParam.setResult(Boolean.TRUE);
            }
        });

    }

    private void bohong(XC_LoadPackage.LoadPackageParam lpparam){
        if (lpparam.packageName.toLowerCase().equals("com.kozhevin.rootchecks")) {

            XposedHelpers.findAndHookMethod("com.kozhevin.rootchecks.util.MeatGrinder", lpparam.classLoader, "isPermissiveSelinux", new XC_MethodHook() {
                /* access modifiers changed from: protected */
                public void afterHookedMethod(MethodHookParam methodHookParam) {
                    methodHookParam.setResult(false);
                }
            });
        }
    }

    private void selinux(XC_LoadPackage.LoadPackageParam lpparam){
    findAndHookMethod("android.os.SystemProperties", lpparam.classLoader, "get", String.class, new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
              if (((String) param.args[0]).equals("ro.build.selinux")) {
                  param.setResult("1");
              }

        }
        protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
             if (((String) param.args[0]).equals("ro.build.selinux")) {
                 param.setResult("1");

             }
        }
        });
    }

    public static final class kon extends XC_MethodHook {
        public void afterHookedMethod(XC_MethodHook.MethodHookParam param1MethodHookParam) {
            List<String> list = Arrays.asList("/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su", "/system/app/Superuser.apk");
            if (param1MethodHookParam != null) {
                String str = XposedHelpers.getObjectField(param1MethodHookParam.thisObject, "path").toString();
                if (list.contains(str)) {
                    param1MethodHookParam.setResult(Boolean.FALSE);
                }
                if (str.equals("/etc/security/otacerts.zip")) {
                    param1MethodHookParam.setResult(Boolean.TRUE);
                }
            }
        }
    }



    public void hideXposed(XC_LoadPackage.LoadPackageParam paramLoadPackageParam) {
        findAndHookMethod("java.lang.Class", paramLoadPackageParam.classLoader, "forName", String.class, Boolean.TYPE, ClassLoader.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam paramAnonymousMethodHookParam)
                    throws Throwable {
                String str = (String) paramAnonymousMethodHookParam.args[0];
                if ((str != null) && ((str.equals("de.robv.android.xposed.XposedBridge")) || (str.equals("de.robv.android.xposed.XC_MethodReplacement")))) {
                    paramAnonymousMethodHookParam.setThrowable(new ClassNotFoundException());

                }
            }
        });
        Class clazz2 = XposedHelpers.findClass("java.io.File", paramLoadPackageParam.classLoader);
        XposedHelpers.findAndHookMethod(clazz2, "exists", new kon());
        String str = Build.TAGS;
        if (!str.equals("release-keys")) {
            XposedHelpers.setStaticObjectField(android.os.Build.class, "TAGS", "release-keys");

        }
    }
    private void bridge(XC_LoadPackage.LoadPackageParam lpparam){
            XposedHelpers.findAndHookMethod(File.class, "exists", new XC_MethodHook() {
                /* access modifiers changed from: protected */
                public void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    File file = (File) methodHookParam.thisObject;
                    if (new File("/sys/fs/selinux/enforce").equals(file)) {
                        methodHookParam.setResult(true);
                    } else if (new File("/system/bin/su").equals(file) || new File("/system/xbin/su").equals(file)) {
                        methodHookParam.setResult(false);
                    }
                }
            });
         XposedHelpers.findAndHookMethod(JSONObject.class, "getBoolean", String.class, new XC_MethodHook() {
            /* access modifiers changed from: protected */
            public void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                String str = (String) methodHookParam.args[0];
                if ("ctsProfileMatch".equals(str) || "basicIntegrity".equals(str) || "isValidSignature".equals(str)) {
                    methodHookParam.setResult(true);
                }
            }
        });



        XposedHelpers.findAndHookMethod(BufferedReader.class, "readLine", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String result = (String) param.getResult();
                if(result != null) {
                    if (result.contains("/data/data/de.robv.android.xposed.installer/bin/XposedBridge.jar")) {
                        param.setResult("");new File("").lastModified();
                    }
                }
                super.afterHookedMethod(param);
            }
        });
        XposedHelpers.findAndHookMethod(File.class, "exists", new XC_MethodHook() {
            /* access modifiers changed from: protected */
            public void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                File file = (File) methodHookParam.thisObject;
                if (new File("/sys/fs/selinux/enforce").equals(file)) {
                    methodHookParam.setResult(true);
                } else if (new File("/system/bin/su").equals(file) || new File("/system/xbin/su").equals(file) || new File("/system/framework/XposedBridge.jar").equals(file)||  new File("/data/adb/magisk").equals(file) ||  new File("/system/xposed.prop").equals(file)) {
                    methodHookParam.setResult(false);
                }
            }
        });


        XposedHelpers.findAndHookMethod("android.os.Debug", lpparam.classLoader, "isDebuggerConnected", XC_MethodReplacement.returnConstant(false));
        if (!Build.TAGS.equals("release-keys")) {
            XposedHelpers.setStaticObjectField(Build.class, "TAGS", "release-keys");
        }
        XposedHelpers.findAndHookMethod("android.os.SystemProperties", lpparam.classLoader, "get", String.class, new XC_MethodHook() {
            /* access modifiers changed from: protected */
            public void beforeHookedMethod(MethodHookParam methodHookParam) {
                if (methodHookParam.args[0].equals("ro.build.selinux")) {
                    methodHookParam.setResult("1");
                }
            }
        });


        XposedHelpers.findAndHookMethod(File.class, "exists", new XC_MethodHook() {
            /* access modifiers changed from: protected */
            public void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                File file = (File) methodHookParam.thisObject;
                if (new File("/sys/fs/selinux/enforce").equals(file)) {
                    methodHookParam.setResult(true);
                } else if (new File("/system/bin/su").equals(file) || new File("/system/xbin/su").equals(file)) {
                    methodHookParam.setResult(false);
                }
            }
        });


        XposedHelpers.findAndHookMethod("java.lang.Class", lpparam.classLoader, "forName", String.class, Boolean.TYPE, ClassLoader.class, new XC_MethodHook() {
            /* access modifiers changed from: protected */
            public void beforeHookedMethod(MethodHookParam methodHookParam) {
                String str = (String) methodHookParam.args[0];
                if (str == null) {
                    return;
                }
                if (str.contains("XposedBridge") || str.contains("XC_MethodReplacement")) {
                    methodHookParam.setThrowable(new ClassNotFoundException());
                }
            }
        });
        XposedHelpers.findAndHookMethod(StringWriter.class, "toString", new XC_MethodHook() {
            /* access modifiers changed from: protected */
            public void afterHookedMethod(MethodHookParam methodHookParam) {
                if (((String) methodHookParam.getResult()).toLowerCase().contains("de.robv.android.xposed")) {
                    methodHookParam.setResult("");
                }
            }
        });
        XposedHelpers.findAndHookMethod(StringWriter.class, "toString", new XC_MethodHook() {
            /* access modifiers changed from: protected */
            public void afterHookedMethod(MethodHookParam methodHookParam) {
                if (((String) methodHookParam.getResult()).toLowerCase().contains("meowcat")) {
                    methodHookParam.setResult("");
                }
            }
        });
        XposedHelpers.findAndHookMethod(StringWriter.class, "toString", new XC_MethodHook() {
            /* access modifiers changed from: protected */
            public void afterHookedMethod(MethodHookParam methodHookParam) {
                if (((String) methodHookParam.getResult()).toLowerCase().contains("com.android.internal.os.ZygoteInit")) {
                    methodHookParam.setResult("");
                }
            }
        });

        XposedHelpers.findAndHookMethod(JSONObject.class, "getBoolean", String.class, new XC_MethodHook() {
            /* access modifiers changed from: protected */
            public void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                String str = (String) methodHookParam.args[0];
                if ("ctsProfileMatch".equals(str) || "basicIntegrity".equals(str) || "isValidSignature".equals(str)) {
                    methodHookParam.setResult(true);
                }

            }
        });

    }
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {

        XposedHelpers.findAndHookMethod(JSONObject.class, "getBoolean", String.class, new XC_MethodHook() {
            /* access modifiers changed from: protected */
            public void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                String str = (String) methodHookParam.args[0];
                if ("ctsProfileMatch".equals(str) || "basicIntegrity".equals(str) || "isValidSignature".equals(str)) {
                    methodHookParam.setResult(true);
                }
            }
        });
        moremock(lpparam);
        if (lpparam.packageName.equals("com.gojek.driver.bike") || lpparam.packageName.equals("com.grabtaxi.driver2")) {
            selinux(lpparam);
            bridge(lpparam);
            moremock(lpparam);
            XposedHelpers.findAndHookMethod(File.class, "exists", new XC_MethodHook() {
                /* access modifiers changed from: protected */
                public void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    File file = (File) methodHookParam.thisObject;
                    if (new File("/sys/fs/selinux/enforce").equals(file)) {
                        methodHookParam.setResult(true);
                    } else if (new File("/system/bin/su").equals(file) || new File("/system/xbin/su").equals(file)) {
                        methodHookParam.setResult(false);
                    }
                }
            });

        XposedHelpers.findAndHookMethod(JSONObject.class, "getBoolean", String.class, new XC_MethodHook() {
            /* access modifiers changed from: protected */
            public void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                String str = (String) methodHookParam.args[0];
                if ("ctsProfileMatch".equals(str) || "basicIntegrity".equals(str) || "isValidSignature".equals(str)) {
                    methodHookParam.setResult(true);
                }
            }
        });

             act = new String[]{"com.gojek.driver.car", "com.gojek.goboxdriver", "com.grabtaxi.driver2"};
             pkg1 = new String[]{"id.co.cimbniaga.mobile.android", "com.deuxvelva.satpolapp", "com.telkom.mwallet", "com.gojek.driver.bike", "com.gojek.driver.bike", "com.gojek.driver.car", "com.gojek.goboxdriver", "com.grabtaxi.driver2"};
             key1 = new String[]{"magisksu", "supersu", "magisk", "superuser", "Superuser", "noshufou", "xposed", "rootcloak", "chainfire", "titanium", "Titanium", "substrate", "greenify", "daemonsu", "root", "busybox", "titanium", ".tmpsu", "su", "rootcloak2", "xposed"};
             cmd1 = new String[]{"su", "which", "busybox", "pm", "am", "sh", "ps", "magisk"};
             lib1 = new String[]{"tool-checker"};
             this.appSet = new HashSet(Arrays.asList(this.pkg1));
             this.keywordSet = new HashSet(Arrays.asList(this.key1));
             this.commandSet = new HashSet<>(Arrays.asList(this.cmd1));
             this.libnameSet = new HashSet<>(Arrays.asList(this.lib1));
             this.activity = new HashSet<>(Arrays.asList(this.act));

            XposedBridge.log("xxxxxxxxxxx");
            moremock(lpparam);
            initFile(lpparam);
            initRuntime(lpparam);
            hideXposed(lpparam);
            bypassFa(lpparam);

            systemContext = (Context) XposedHelpers.callMethod(XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.app.ActivityThread", lpparam.classLoader), "currentActivityThread", new Object[0]), "getSystemContext", new Object[0]);
            MockLocation(lpparam);
             if ("android".equals(lpparam.packageName)) {
                 XposedHelpers.findAndHookMethod(File.class, "exists", new XC_MethodHook() {
                     /* access modifiers changed from: protected */
                     public void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                         File file = (File) methodHookParam.thisObject;
                         if (new File("/sys/fs/selinux/enforce").equals(file)) {
                             methodHookParam.setResult(true);
                         } else if (new File("/system/bin/su").equals(file) || new File("/system/xbin/su").equals(file)) {
                             methodHookParam.setResult(false);
                         }
                     }
                 });
             }
             XposedHelpers.findAndHookMethod(JSONObject.class, "getBoolean", String.class, new XC_MethodHook() {
                 /* access modifiers changed from: protected */
                 public void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                     String str = (String) methodHookParam.args[0];
                     if ("ctsProfileMatch".equals(str) || "basicIntegrity".equals(str) || "isValidSignature".equals(str)) {
                         methodHookParam.setResult(true);
                     }
                 }
             });

            moremock(lpparam);


            XposedHelpers.findAndHookMethod(JSONObject.class, "getBoolean", String.class, new XC_MethodHook() {
                /* access modifiers changed from: protected */
                public void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    String str = (String) methodHookParam.args[0];
                    if ("ctsProfileMatch".equals(str) || "basicIntegrity".equals(str) || "isValidSignature".equals(str)) {
                        methodHookParam.setResult(true);
                    }
                }
            });

                XposedBridge.log("Load Package name " + lpparam.packageName);

            }

        if (lpparam.packageName.toLowerCase().equals("com.kozhevin.rootchecks")) {

            XposedHelpers.findAndHookMethod("com.kozhevin.rootchecks.util.MeatGrinder", lpparam.classLoader, "isPermissiveSelinux", new XC_MethodHook() {
                /* access modifiers changed from: protected */
                public void afterHookedMethod(MethodHookParam methodHookParam) {
                    methodHookParam.setResult(false);
                }
            });
        }
        moremock(lpparam);
        XposedHelpers.findAndHookMethod(JSONObject.class, "getBoolean", String.class, new XC_MethodHook() {
            /* access modifiers changed from: protected */
            public void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                String str = (String) methodHookParam.args[0];
                if ("ctsProfileMatch".equals(str) || "basicIntegrity".equals(str) || "isValidSignature".equals(str)) {
                    methodHookParam.setResult(true);
                }
            }
        });
        }


    private void moremock(XC_LoadPackage.LoadPackageParam lpparam)
    {


        if (Build.VERSION.SDK_INT >= 23)
        {
            findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "checkOp", String.class, Integer.TYPE, String.class, opHook);
            findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "checkOp", Integer.TYPE, Integer.TYPE, String.class, opHook);
            findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "checkOpNoThrow", String.class, Integer.TYPE, String.class, opHook);
            findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "checkOpNoThrow", Integer.TYPE, Integer.TYPE, String.class, opHook);
            findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "noteOp", String.class, Integer.TYPE, String.class, opHook);
            findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "noteOp", Integer.TYPE, Integer.TYPE, String.class, opHook);
            findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "noteOpNoThrow", String.class, Integer.TYPE, String.class, opHook);
            findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "noteOpNoThrow", Integer.TYPE, Integer.TYPE, String.class, opHook);
            findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "noteProxyOp", String.class, String.class, opHook);
            findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "noteProxyOp", Integer.TYPE, String.class, opHook);
            findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "noteProxyOpNoThrow", String.class, String.class, opHook);
            findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "noteProxyOpNoThrow", Integer.TYPE, String.class, opHook);
            findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "startOp", String.class, Integer.TYPE, String.class, opHook);
            findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "startOp", Integer.TYPE, Integer.TYPE, String.class, opHook);
            findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "startOpNoThrow", String.class, Integer.TYPE, String.class, opHook);
            findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "startOpNoThrow", Integer.TYPE, Integer.TYPE, String.class, opHook);

        }
    }
    private void MockLocation(XC_LoadPackage.LoadPackageParam lpparam)
    {
        Class clazz1 = XposedHelpers.findClass("android.provider.Settings.Secure", lpparam.classLoader);
        XposedHelpers.findAndHookMethod(clazz1, "getInt", ContentResolver.class, String.class, int.class, new b());


        findAndHookMethod("android.provider.Settings.Secure", lpparam.classLoader, "getString", ContentResolver.class, String.class, new XC_MethodHook()
        {
            protected void beforeHookedMethod(MethodHookParam paramAnonymousMethodHookParam)

            {
                if (paramAnonymousMethodHookParam.args[1].equals("mock_location")) {
                    paramAnonymousMethodHookParam.setResult("0");
                }
            }
        });
        findAndHookMethod("android.provider.Settings.Secure", lpparam.classLoader, "getInt", ContentResolver.class, String.class, new XC_MethodHook()
        {
            protected void beforeHookedMethod(MethodHookParam paramAnonymousMethodHookParam)
                    throws Throwable
            {
                if (paramAnonymousMethodHookParam.args[1].equals("mock_location")) {
                    paramAnonymousMethodHookParam.setResult("0");
                }
            }
        });
        findAndHookMethod("android.provider.Settings.Secure", lpparam.classLoader, "getInt", ContentResolver.class, String.class, Integer.TYPE, new XC_MethodHook()
        {
            protected void beforeHookedMethod(MethodHookParam paramAnonymousMethodHookParam)
                    throws Throwable
            {
                if (paramAnonymousMethodHookParam.args[1].equals("mock_location")) {
                    paramAnonymousMethodHookParam.setResult("0");
                }
            }
        });
        findAndHookMethod("android.provider.Settings.Secure", lpparam.classLoader, "getFloat", ContentResolver.class, String.class, new XC_MethodHook()
        {
            protected void beforeHookedMethod(MethodHookParam paramAnonymousMethodHookParam)
                    throws Throwable
            {
                if (paramAnonymousMethodHookParam.args[1].equals("mock_location")) {
                    paramAnonymousMethodHookParam.setResult("0.0f");
                }
            }
        });
        findAndHookMethod("android.provider.Settings.Secure", lpparam.classLoader, "getFloat", ContentResolver.class, String.class, Float.TYPE, new XC_MethodHook()
        {
            protected void beforeHookedMethod(MethodHookParam paramAnonymousMethodHookParam)
                    throws Throwable
            {
                if (paramAnonymousMethodHookParam.args[1].equals("mock_location")) {
                    paramAnonymousMethodHookParam.setResult("0.0f");
                }
            }
        });
        findAndHookMethod("android.provider.Settings.Secure", lpparam.classLoader, "getLong", ContentResolver.class, String.class, new XC_MethodHook()
        {
            protected void beforeHookedMethod(MethodHookParam paramAnonymousMethodHookParam)
                    throws Throwable
            {
                if (paramAnonymousMethodHookParam.args[1].equals("mock_location")) {
                    paramAnonymousMethodHookParam.setResult("0.0f");
                }
            }
        });
        findAndHookMethod("android.provider.Settings.Secure", lpparam.classLoader, "getLong", ContentResolver.class, String.class, Long.TYPE, new XC_MethodHook()
        {
            protected void beforeHookedMethod(MethodHookParam paramAnonymousMethodHookParam)
                    throws Throwable
            {
                if (paramAnonymousMethodHookParam.args[1].equals("mock_location")) {
                    paramAnonymousMethodHookParam.setResult("0L");
                }
            }
        });
        findAndHookMethod("android.location.Location", lpparam.classLoader, "getExtras", new XC_MethodHook()
        {
            protected void afterHookedMethod(MethodHookParam paramAnonymousMethodHookParam)
            {
                Bundle localBundle = (Bundle)paramAnonymousMethodHookParam.getResult();
                if ((localBundle != null) && (localBundle.getBoolean("mockLocation"))) {
                    localBundle.putBoolean("mockLocation", false);
                }
                paramAnonymousMethodHookParam.setResult(Boolean.FALSE);
            }
        });

        findAndHookMethod("android.location.Location", lpparam.classLoader, "isFromMockProvider", new XC_MethodHook()
        {
            protected void beforeHookedMethod(MethodHookParam paramAnonymousMethodHookParam)
            {
                paramAnonymousMethodHookParam.setResult(Boolean.FALSE);
            }
        });


    }

    private void next(XC_LoadPackage.LoadPackageParam lpparam) {


        moremock(lpparam);
        XC_MethodHook hookClass = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                String packageName = (String) param.args[0];
                if (packageName.toLowerCase().contains("xposed")) {
                    param.setThrowable(new ClassNotFoundException(packageName));
                }
            }
        };
        // FIXME: 18-6-23 w568w: It's very dangerous to hook these methods, thinking to replace them.
        XposedHelpers.findAndHookMethod(
                ClassLoader.class,
                "loadClass",
                String.class,
                boolean.class,
                hookClass
        );
        XposedHelpers.findAndHookMethod(
                Class.class,
                "forName",
                String.class,
                boolean.class,
                ClassLoader.class,
                hookClass
        );

        XposedHelpers.findAndHookConstructor(
                File.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String path = (String) param.args[0];
                        boolean shouldDo = path.matches("/proc/[0-9]+/maps") ||
                                (path.toLowerCase().contains(C.KW_XPOSED) || path.toLowerCase().contains("magisk") || path.toLowerCase().contains("XposedBridge.jar") && !path.contains("fkzhang"));
                        if (shouldDo) {
                            param.args[0] = "/system/build.prop";
                        }
                    }
                }
        );

        XC_MethodHook hookStack = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                StackTraceElement[] elements = (StackTraceElement[]) param.getResult();
                List<StackTraceElement> clone = new ArrayList<>();
                for (StackTraceElement element : elements) {
                    if (!element.getClassName().toLowerCase().contains(C.KW_XPOSED)) {
                        clone.add(element);
                    }
                }
                param.setResult(clone.toArray(new StackTraceElement[0]));
            }
        };
        XposedHelpers.findAndHookMethod(
                Throwable.class,
                "getStackTrace",
                hookStack
        );
        XposedHelpers.findAndHookMethod(
                Thread.class,
                "getStackTrace",
                hookStack
        );

        XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "getInstalledPackages",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        List<PackageInfo> apps = (List<PackageInfo>) param.getResult();
                        List<PackageInfo> clone = new ArrayList<>();
                        // foreach is very slow.
                        final int len = apps.size();
                        for (int i = 0; i < len; i++) {
                            PackageInfo app = apps.get(i);
                            if (!app.packageName.toLowerCase().contains(C.KW_XPOSED)  || !app.packageName.toLowerCase().contains("magisk") || !app.packageName.toLowerCase().contains("Superuser") ||  !app.packageName.toLowerCase().contains("hide")) {
                                clone.add(app);
                            }
                        }
                        param.setResult(clone);
                    }
                }
        );
        XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "getInstalledApplications",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        List<ApplicationInfo> apps = (List<ApplicationInfo>) param.getResult();
                        List<ApplicationInfo> clone = new ArrayList<>();
                        final int len = apps.size();
                        for (int i = 0; i < len; i++) {
                            ApplicationInfo app = apps.get(i);
                            boolean shouldRemove = app.metaData != null && app.metaData.getBoolean("xposedmodule") ||
                                    app.packageName != null && app.packageName.toLowerCase().contains(C.KW_XPOSED) ||
                                    app.packageName != null && app.packageName.toLowerCase().contains("aldyjrz") ||
                                    app.className != null && app.className.toLowerCase().contains(C.KW_XPOSED) ||
                                    app.className != null && app.className.toLowerCase().contains("magisk") ||
                                    app.processName != null && app.processName.toLowerCase().contains("magisk") ||
                                    app.processName != null && app.processName.toLowerCase().contains("su") ||
                                    app.packageName != null && app.packageName.toLowerCase().contains("meowcat") ||
                                    app.processName != null && app.processName.toLowerCase().contains(C.KW_XPOSED);
                            if (!shouldRemove) {
                                clone.add(app);
                            }
                        }
                        param.setResult(clone);
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                Modifier.class,
                "isNative",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        param.setResult(false);
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                System.class,
                "getProperty",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if ("vxp".equals(param.args[0])) {
                            param.setResult(null);
                        }
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                File.class,
                "list",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String[] fs = (String[]) param.getResult();
                        if (fs == null) {
                            return;
                        }
                        List<String> list = new ArrayList<>();
                        for (String f : fs) {
                            if (!f.toLowerCase().contains(C.KW_XPOSED)
                                    && !f.equals("su")
                                    && !f.contains("magisk")
                                    && !f.contains("supersu")
                                    && !f.contains("xposed")
                                    && !f.contains("sudohide")
                            ) {
                                list.add(f);
                            }
                        }
                        param.setResult(list.toArray(new String[0]));
                    }
                }
        );

        Class<?> clazz = null;
        try {
            clazz = Runtime.getRuntime().exec("echo").getClass();
        } catch (IOException ignore) {
            XposedBridge.log("[Toixposed] Cannot hook Process#getInputStream");
        }
        if (clazz != null) {
            XposedHelpers.findAndHookMethod(
                    clazz,
                    "getInputStream",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            InputStream is = (InputStream) param.getResult();
                            if (is instanceof FilterXpInputStream) {
                                param.setResult(is);
                            } else {
                                param.setResult(new FilterXpInputStream(is));
                            }
                        }
                    }
            );
        }

        XposedBridge.hookAllMethods(System.class, "getenv",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.args.length == 0) {
                            Map<String, String> res = (Map<String, String>) param.getResult();
                            String classpath = res.get("CLASSPATH");
                            param.setResult(filter(classpath));
                        } else if ("CLASSPATH".equals(param.args[0])) {
                            String classpath = (String) param.getResult();
                            param.setResult(filter(classpath));
                        }
                    }

                    private String filter(String s) {
                        List<String> list = Arrays.asList(s.split(":"));
                        List<String> clone = new ArrayList<>();
                        for (int i = 0; i < list.size(); i++) {
                            if (!list.get(i).toLowerCase().contains(C.KW_XPOSED)) {
                                clone.add(list.get(i));
                            }
                        }
                        StringBuilder res = new StringBuilder();
                        for (int i = 0; i < clone.size(); i++) {
                            res.append(clone);
                            if (i != clone.size() - 1) {
                                res.append(":");
                            }
                        }
                        return res.toString();
                    }
                }
        );
    }
}
