package com.amazon.tests;

import java.io.*;
import java.lang.Math;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.springframework.context.annotation.Bean;

/** File that runs all of the integrations tests and creates/deletes sample rules */
public class CentralizedSamplingIntegrationTests {

  /**
   * Set up new httpClient
   *
   * @return OkhttpClient
   */
  @Bean
  public static Call.Factory httpClient() {
    return new OkHttpClient();
  }

  /**
   * Main function that runs the three tests for Centralized Sampling SampleRulesTests - tests rules
   * filter and sample the correct number of targets ReservoirTests - tests that reservoir works
   * correctly with Sampling Rules PriorityTests - test Priority of sampling rules that higher
   * priority rules get sampled first
   *
   * @param args
   * @throws IOException - throws if calls to xray backend fail
   * @throws InterruptedException - throws if tests fail
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    sampleRulesTests();
    reservoirTests();
    priorityTests();
  }

  /**
   * Function to
   *
   * @param testCase - testCase to make call with, determines headers, method, and endpoint, etc.
   * @param sampleRule - sampleRule that is currently active
   * @return true if call is succesful and has expected sampling rate, false else
   */
  public static boolean makeCalls(testCases testCase, SampleRules sampleRule) {
    RequestBody reqbody = null;
    String stringResp = "";
    if (testCase.method.equals("POST")) {
      reqbody = RequestBody.create(null, new byte[0]);
    }
    try (Response response =
        httpClient()
            .newCall(
                new Request.Builder()
                    .addHeader("user", testCase.user)
                    .addHeader("service_name", testCase.name)
                    .addHeader("required", testCase.required)
                    .addHeader("totalSpans", String.valueOf(GenericConstants.TOTAL_CALLS))
                    .url("http://localhost:8080" + testCase.endpoint)
                    .method(testCase.method, reqbody)
                    .build())
            .execute()) {
      stringResp = response.body().string().toString();

    } catch (IOException e) {
      throw new UncheckedIOException("Could not fetch endpoint", e);
    }
    int expectedRate = GenericConstants.DEFAULT_RATE;
    if (testCase.matches.contains(sampleRule.name)) {
      expectedRate = (int) Math.round(sampleRule.ExpectedSampled * GenericConstants.TOTAL_CALLS);
    }
    double range = expectedRate * .1 + GenericConstants.DEFAULT_RANGE;
    int roundedRange = (int) Math.round(range);
    if (expectedRate == 0) {
      roundedRange = 0;
    }
    System.out.println(
        "Sampled rate: "
            + stringResp
            + ". Expected rate: "
            + expectedRate
            + " for Sample Rule "
            + sampleRule.name
            + " and test case "
            + testCase.name);
    if (Integer.parseInt(stringResp) > expectedRate + roundedRange
        || Integer.parseInt(stringResp) < expectedRate - roundedRange) {
      System.out.println("Sampled rate does not match expected rate");
      return false;
    }
    return true;
  }

  /**
   * Function to call xray backend to create a sampleRule
   *
   * @param jsonBody - JSONBody of the sampleRule
   * @param ruleName - name of the Rule
   * @throws IOException - throws if unable to connect to xray backend
   */
  @SuppressWarnings("checkstyle:EmptyBlock")
  public static void makeRule(String jsonBody, String ruleName) throws IOException {
    // Default rule exists in x-ray by default hence the name
    if (jsonBody.equals("Default")) {
      return;
    }
    System.out.println("Creating " + ruleName + " sample rule");
    MediaType json = MediaType.get("application/json; charset=utf-8");

    RequestBody reqbody = RequestBody.create(json, jsonBody);
    try (Response response =
        httpClient()
            .newCall(
                new Request.Builder()
                    .url("http://localhost:2000/CreateSamplingRule")
                    .method("POST", reqbody)
                    .build())
            .execute()) {
    } catch (IOException e) {
      throw new IOException("Could not fetch endpoint", e);
    }
  }

  /**
   * Function to call xray backend to delete a sampleRule
   *
   * @param ruleName - name of the Rule
   * @throws IOException - throws if unable to connect to xray backend
   */
  public static void deleteRule(String ruleName) throws IOException {
    System.out.println("Deleting " + ruleName + " sample rule");

    MediaType json = MediaType.get("application/json; charset=utf-8");
    String jsonBody = "{\n" + "   \"RuleName\": \"" + ruleName + "\"\n" + "}\n";
    RequestBody reqbody = RequestBody.create(json, jsonBody);
    try (Response response =
        httpClient()
            .newCall(
                new Request.Builder()
                    .url("http://localhost:2000/DeleteSamplingRule")
                    .method("POST", reqbody)
                    .build())
            .execute()) {
    } catch (IOException e) {
      throw new IOException("Could not fetch endpoint", e);
    }
  }

  /**
   * Function to run tests on Reservoir SampleRules. Creates one rule at a time, waits 20s for
   * reservoir to be set runs all test cases, then deletes the rule. Does this for all reservoir
   * sample rules
   *
   * @throws IOException throws if unable to connect to xray backend
   * @throws InterruptedException if a test fails after retries
   */
  public static void reservoirTests() throws IOException, InterruptedException {
    SampleRules[] sampleRules = SampleRules.getReservoirRules();
    for (int i = 0; i < sampleRules.length; i++) {
      makeRule(sampleRules[i].JSON, sampleRules[i].name);
      boolean passed = false;
      for (int j = 0; i < GenericConstants.MAX_RETRIES; j++) {
        TimeUnit.SECONDS.sleep(GenericConstants.WAIT_FOR_RESERVOIR);
        try {
          passed = makeCalls(testCases.getDefaultUser(), sampleRules[i]);
        } finally {
          if (passed) {
            break;
          } else if (j < GenericConstants.MAX_RETRIES - 1) {
            System.out.println("Test failed, attempting retry");
          } else {
            System.out.println(
                "Test failed for Sample rule: "
                    + sampleRules[i].name
                    + " and test case "
                    + testCases.getDefaultUser().name);
            throw new InterruptedException();
          }
        }
      }
      deleteRule(sampleRules[i].name);
    }
  }

  /**
   * Function to run tests on Priority SampleRules, creates all sample rules then makes calls and
   * verifies the priority rule for the testCase is the expected Sample result, then deletes all
   * rules
   *
   * @throws IOException if unable to connect to xray backend
   * @throws InterruptedException if tests fail after retries
   */
  public static void priorityTests() throws IOException, InterruptedException {
    testCases[] allTestCases = testCases.getAllTestCases();
    SampleRules[] sampleRules = SampleRules.getPriorityRules();
    for (SampleRules sampleRule : sampleRules) {
      makeRule(sampleRule.JSON, sampleRule.name);
    }
    TimeUnit.SECONDS.sleep(GenericConstants.RETRY_WAIT);
    for (testCases allTestCase : allTestCases) {
      int priority = sampleRules.length - 1;
      for (int j = 0; j < sampleRules.length; j++) {
        if (allTestCase.matches.contains(sampleRules[j].name)
            && priority == sampleRules.length - 1) {
          priority = j;
        }
      }
      boolean passed = false;
      for (int k = 0; k < GenericConstants.MAX_RETRIES; k++) {
        try {
          passed = makeCalls(allTestCase, sampleRules[priority]);
        } finally {
          if (passed) {
            break;
          } else if (k < GenericConstants.MAX_RETRIES - 1) {
            System.out.println("Test failed, attempting retry");
          }
        }
      }
      if (!passed) {
        System.out.println(
            "Test failed for Sample rule: "
                + sampleRules[priority].name
                + " and test case "
                + allTestCase.name);
        throw new InterruptedException();
      } else {
        System.out.println(
            "Test passed for Sample rule: "
                + sampleRules[priority].name
                + " and test case "
                + allTestCase.name);
      }
    }

    for (SampleRules sampleRule : sampleRules) {
      deleteRule(sampleRule.name);
    }
  }

  /**
   * Runs tests for each sample Rule individually. Creates a sample rule, waits 1s for rule to be
   * applied, verifies the expected sampling rate matches the expected rate for the testCase, then
   * deletes the rule. Repeats this for all sample rules
   *
   * @throws IOException if unable to connect to xray backend
   * @throws InterruptedException if tests fail
   */
  public static void sampleRulesTests() throws IOException, InterruptedException {
    SampleRules[] sampleRules = SampleRules.getSampleRules();
    testCases[] allTestCases = testCases.getAllTestCases();

    for (SampleRules sampleRule : sampleRules) {
      try {
        makeRule(sampleRule.JSON, sampleRule.name);
        TimeUnit.SECONDS.sleep(GenericConstants.RETRY_WAIT);
        for (testCases allTestCase : allTestCases) {
          boolean passed = false;
          for (int k = 0; k < GenericConstants.MAX_RETRIES; k++) {
            try {
              passed = makeCalls(allTestCase, sampleRule);
            } finally {
              if (passed) {
                break;
              } else if (k < GenericConstants.MAX_RETRIES - 1) {
                System.out.println("Test failed here, attempting retry");
              }
              TimeUnit.SECONDS.sleep(GenericConstants.RETRY_WAIT);
            }
          }
          if (!passed) {
            System.out.println(
                "Test failed for Sample rule: "
                    + sampleRule.name
                    + " and test case "
                    + allTestCase.name);

            throw new InterruptedException();
          } else {
            System.out.println(
                "Test passed for Sample rule: "
                    + sampleRule.name
                    + " and test case "
                    + allTestCase.name);
          }
        }
      } finally {
        deleteRule(sampleRule.name);
      }
    }
  }
}
