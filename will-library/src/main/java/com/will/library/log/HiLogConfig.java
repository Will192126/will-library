package com.will.library.log;

public abstract class HiLogConfig {
    static int MAX_LEN = 512;
    static HiThreadFormater HI_THREAD_FORMATER = new HiThreadFormater();
    static HiStackTraceFormater HI_STACK_TRACE_FORMATER = new HiStackTraceFormater();
    public JsonParser injectJsonParser() {
        return null;
    }

    public String getGlobalTag() {
        return "HiLog";
    }

    public boolean enable() {
        return true;
    }

    public boolean includeThread() {
        return false;
    }

    public int stackTraceDepth() {
        return 5;
    }

    public HiLogPrinter[] printers() {
        return null;
    }

    public interface JsonParser {
        String toJsonString(Object src);
    }
}
