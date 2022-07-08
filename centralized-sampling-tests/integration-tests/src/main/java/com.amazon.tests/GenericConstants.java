package com.amazon.tests;

public class GenericConstants {
  public static final int MAX_RETRIES = 4;
  public static final int WAIT_FOR_RESERVOIR = 20;
  public static final int TOTAL_CALLS = 1000;
  public static final int DEFAULT_RATE = (int) (.05 * TOTAL_CALLS) + 1;
  public static final int DEFAULT_RANGE = 10;
  public static final int RETRY_WAIT = 1;
}
