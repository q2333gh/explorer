package com.explorer.shell;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;

public class test1 {

  public static void main(String[] args) throws IOException {
    if (isRunning("explorer-test01")) {
      run("pkill -f java.*explorer-test01.jar");
      if (!isRunning("explorer-test01")) {
        stdOut("explorer-test01 stopped successfully!");
      }
    } else {
      stdOut("No such explorer-test01.jar process found.");
    }
  }

  private static void stdOut(String x) {
    System.out.println(x);
  }

  private static boolean isRunning(String processName) throws IOException {
    String psOutput = run("ps aux");
    String grepOutput = run("grep -q [e]xplorer-test01", psOutput);
    return grepOutput.isEmpty();
  }

  private static String run(String command) throws IOException {
    CommandLine cmdLine = CommandLine.parse(command);
    DefaultExecutor executor = new DefaultExecutor();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
    executor.setStreamHandler(streamHandler);
    executor.execute(cmdLine);
    return outputStream.toString();
  }

  private static String run(String command, String input) throws IOException {
    CommandLine cmdLine = CommandLine.parse(command);
    DefaultExecutor executor = new DefaultExecutor();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, null, inputStream);
    executor.setStreamHandler(streamHandler);
    executor.execute(cmdLine);
    return outputStream.toString();
  }
}
