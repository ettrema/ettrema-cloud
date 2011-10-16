package com.ettrema.client.util;

public class ScriptException extends Exception {
  private int returnCode;
  private String output;
  
  public ScriptException(int returnCode, String output) {
    super("return code indicates failure:" + returnCode);
    this.returnCode = returnCode;
    this.output = output;
  }
  
  public ScriptException(String message, Throwable cause) {
    super(message,cause);
  }
  
  public ScriptException( Throwable cause) {
    super(cause);
  }

  public ScriptException(String message) {
    super(message);
  }
  
  public int getReturnCode() {
    return returnCode;
  }
  
  public String getOutput() {
    return output;
  }
}
