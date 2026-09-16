package woyou.aidlservice.jiuiv5;

/**
 * Sunmi Printer Callback AIDL Interface
 */
interface ICallback {
    oneway void onRunResult(boolean isSuccess);
    oneway void onReturnString(String result);
    oneway void onRaiseException(int code, String msg);
    oneway void onPrintResult(int code, String msg);
}
