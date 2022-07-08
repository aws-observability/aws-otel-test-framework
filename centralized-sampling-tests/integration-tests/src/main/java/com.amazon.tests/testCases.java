package com.amazon.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * File that contains all test cases to be used to make calls to the sample app while sample rules
 * are in place
 */
public class testCases {
  public List<String> matches;
  public String name;
  public String user;
  public String required;
  public String method;
  public String endpoint;

  /**
   * Test case used to make calls to specific endpoints with specific headers
   *
   * @param user - type of user - ex - service, admin, test
   * @param name - name that should be assigned to the spans service name
   * @param required - header that is either "true" or "false" used to check attributes
   * @param matches - Sample rules that should trigger on this test case. Names are derived from
   *     SampleRules.java
   * @param endpoint - endpoint to hit, either /getSampled or /importantEndpoint
   * @param method - Method used to make the call, either "POST" or "GET"
   */
  public testCases(
      String user,
      String name,
      String required,
      List<String> matches,
      String endpoint,
      String method) {
    this.matches = matches;
    this.name = name;
    this.user = user;
    this.required = required;
    this.endpoint = endpoint;
    this.method = method;
  }

  /**
   * Get rules that will be applied to any test case
   *
   * @return list of ruleNames
   */
  public static List<String> getDefaultMatches() {
    return Arrays.asList("AcceptAll", "SampleNone", "HighReservoir", "LowReservoir", "MixedReservoir");
  }

  /**
   * adds default matches to another list of specific rule matches
   *
   * @param matches - list of specific rule matches to have defaults added onto
   * @return list of rule names that match with a testCase
   */
  public static List<String> getMatches(List<String> matches) {
    List<String> defaultMatches = getDefaultMatches();
    matches.addAll(defaultMatches);
    return matches;
  }

  /**
   * Tests default user with nothing extra tested
   *
   * @return default user
   */
  public static testCases getDefaultUser() {
    List<String> matches = getDefaultMatches();
    return new testCases("test", "default", "false", matches, "/getSampled", "GET");
  }

  /**
   * Tests the importantEndpoint endpoint, specifically test ImportantEndpoint SampleRule
   *
   * @return testCases importantTest user
   */
  private static testCases getTestImportantEndpoint() {
    List<String> matches =
        getMatches(new ArrayList<>(Arrays.asList("ImportantEndpoint", "SampleNoneAtEndpoint")));
    return new testCases("test", "importantTest", "false", matches, "/importantEndpoint", "GET");
  }

  /**
   * Tests the user attribute with admin, specifically tests ImportantAttribute SampleRule
   *
   * @return testCases admin User
   */
  private static testCases getAdminGetSampled() {
    List<String> matches = getMatches(new ArrayList<>(Arrays.asList("ImportantAttribute")));
    return new testCases("admin", "adminGetSampled", "false", matches, "/getSampled", "GET");
  }

  /**
   * Tests a post with an admin user, Specifically tests priority with PostRule SampleRule and
   * ImportantAttribute SampleRule
   *
   * @return testCases adminPost user
   */
  private static testCases getAdminPostSampled() {
    List<String> matches =
        getMatches(new ArrayList<>(Arrays.asList("ImportantAttribute", "PostRule")));
    return new testCases("admin", "adminPostSampled", "false", matches, "/getSampled", "POST");
  }

  /**
   * Tests an admin user hitting importantEndpoint, Specifically tests priority with
   * ImportantEndpoint SampleRule and ImportantAttribute SampleRule
   *
   * @return testCases adminPost user
   */
  private static testCases getAdminImportantEndpoint() {
    List<String> matches =
        getMatches(
            new ArrayList<>(
                Arrays.asList("ImportantAttribute", "SampleNoneAtEndpoint", "ImportantEndpoint")));
    return new testCases("admin", "importantAdmin", "false", matches, "/importantEndpoint", "GET");
  }

  /**
   * Tests a service user at getSampled, Specifically tests AttributeAtEndpoint SampleRule
   *
   * @return testCases serviceGetSamped user
   */
  private static testCases getServiceGetSampled() {
    List<String> matches = getMatches(new ArrayList<>(Arrays.asList("AttributeAtEndpoint")));
    return new testCases("service", "serviceGetSampled", "false", matches, "/getSampled", "GET");
  }

  /**
   * Tests a service user at getSampled using Post Method, Specifically tests priority with
   * AttributeAtEndpoint SampleRule and PostRule SampleRule
   *
   * @return testCases servicePostSampled user
   */
  private static testCases getServicePostSampled() {
    List<String> matches =
        getMatches(new ArrayList<>(Arrays.asList("AttributeAtEndpoint", "PostRule")));
    return new testCases("service", "servicePostSampled", "false", matches, "/getSampled", "POST");
  }

  /**
   * Tests a service user at importantEndpoint, Specifically tests ImportantEndpoint Sample Rule and
   * makes sure AttributeatEndpoint is not giving false positives
   *
   * @return testCases serviceImportantEndpoint user
   */
  private static testCases getServiceImportantEndpoint() {
    List<String> matches =
        getMatches(new ArrayList<>(Arrays.asList("ImportantEndpoint", "SampleNoneAtEndpoint")));
    return new testCases(
        "service", "serviceImportant", "false", matches, "/importantEndpoint", "GET");
  }

  /**
   * Tests a user with admin and required=true attributes, Specifically used to test
   * MultipleAttributes Sample Rule
   *
   * @return testCases multipleAttributes user
   */
  private static testCases getMultAttributesGetSampled() {
    List<String> matches =
        getMatches(new ArrayList<>(Arrays.asList("MultipleAttributes", "ImportantAttribute")));
    return new testCases("admin", "multAttributeGetSampled", "true", matches, "/getSampled", "GET");
  }

  /**
   * Tests a user with admin and required=true attributes, Specifically used to test priority with
   * MultipleAttributes Sample Rule and PostRule user
   *
   * @return testCases multipleAttributesPost User
   */
  private static testCases getMultAttributesPostSampled() {
    List<String> matches =
        getMatches(
            new ArrayList<>(Arrays.asList("MultipleAttributes", "ImportantAttribute", "PostRule")));
    return new testCases(
        "admin", "multAttributePostSampled", "true", matches, "/getSampled", "POST");
  }

  /**
   * Tests a user making a post getSampled call, Specifically used to test PostRule Sample Rule
   *
   * @return testCases postSampled user
   */
  private static testCases getPostOnly() {
    List<String> matches = getMatches(new ArrayList<>(Arrays.asList("PostRule")));
    return new testCases("test", "PostOnly", "false", matches, "/getSampled", "POST");
  }

  /**
   * Tests a user with admin and required=true attributes, Specifically used to test priority with
   * MultipleAttributes Sample Rule and ImportantEndpoint user
   *
   * @return testCases multipleAttributesImportantEndpoint User
   */
  private static testCases getMultAttributesImportantEndpoint() {
    List<String> matches =
        getMatches(
            new ArrayList<>(
                Arrays.asList(
                    "MultipleAttributes",
                    "ImportantAttribute",
                    "ImportantEndpoint",
                    "SampleNoneAtEndpoint")));
    return new testCases(
        "admin", "multAttributeImportant", "true", matches, "/importantEndpoint", "GET");
  }

  /**
   * Tests a user that sets the ServiceName of its spans, Specifically used to test
   * ImportantServiceName Sample Rule
   *
   * @return testCases serviceName user
   */
  private static testCases getServiceNameTest() {
    List<String> matches = getMatches(new ArrayList<>(Arrays.asList("ImportantServiceName")));
    return new testCases("test", "ImportantServiceName", "false", matches, "/getSampled", "GET");
  }

  /**
   * Tests an admin user that sets the ServiceName of its spans, Specifically used to test priority
   * with ImportantServiceName Sample Rule and ImportantAttribute Sample Rule
   *
   * @return testCases adminServiceName user
   */
  private static testCases getAdminServiceNameTest() {
    List<String> matches =
        getMatches(new ArrayList<>(Arrays.asList("ImportantServiceName", "ImportantAttribute")));
    return new testCases("admin", "ImportantServiceName", "false", matches, "/getSampled", "GET");
  }

  /**
   * Get all testCases
   *
   * @return list of all testCases
   */
  public static testCases[] getAllTestCases() {
    return new testCases[]{
      getDefaultUser(),
      getAdminGetSampled(),
      getAdminPostSampled(),
      getAdminImportantEndpoint(),
      getTestImportantEndpoint(),
      getServiceImportantEndpoint(),
      getServiceGetSampled(),
      getServicePostSampled(),
      getMultAttributesGetSampled(),
      getMultAttributesPostSampled(),
      getMultAttributesImportantEndpoint(),
      getPostOnly(),
      getServiceNameTest(),
      getAdminServiceNameTest()
    };
  }
}
