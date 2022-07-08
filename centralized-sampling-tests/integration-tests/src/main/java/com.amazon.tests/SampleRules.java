package com.amazon.tests;

import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** File that contains all sample Rules that will be created to be used for testing */
public class SampleRules {
  public String name;
  public String JSON;

  public double ExpectedSampled;

  /**
   * @param name - name of the sample rule
   * @param JSON - JSON object to send to the xray to create sample rule
   * @param ExpectedSampled - rate of expected sampling
   */
  public SampleRules(String name, String JSON, double ExpectedSampled) {
    this.JSON = JSON;
    this.name = name;
    this.ExpectedSampled = ExpectedSampled;
  }

  /**
   * Function to get a string JSON variable to send to xray to create a sample rule
   *
   * @param name - String - name of the rule
   * @param priority - int - priority of the rule
   * @param reservoir - int - reservoir or minimum it should sample
   * @param rate - double - fixed rate of the sample rule
   * @param service_name - String - service name to filter by, "*" if none
   * @param method - String - REST method to filter by, "*" if none
   * @param path - PATH - path to filter by, "*" if none
   * @param attributes - JSONObject - attributes to filter by, can be null
   * @return
   */
  public static String getJSON(
      String name,
      int priority,
      int reservoir,
      double rate,
      String service_name,
      String method,
      String path,
      JSONObject attributes) {
    JSONObject jsonObject = new JSONObject();
    JSONObject jsonBody = new JSONObject();
    jsonBody.put("FixedRate", rate);
    jsonBody.put("Host", "*");
    jsonBody.put("HTTPMethod", method);
    jsonBody.put("Priority", priority);
    jsonBody.put("ReservoirSize", reservoir);
    jsonBody.put("ResourceARN", "*");
    jsonBody.put("RuleName", name);
    jsonBody.put("ServiceName", service_name);
    jsonBody.put("ServiceType", "*");
    jsonBody.put("URLPath", path);
    jsonBody.put("Version", 1);
    if (attributes != null) {
      jsonBody.put("Attributes", attributes);
    }
    jsonObject.put("SamplingRule", jsonBody);

    return jsonObject.toString();
  }

  /**
   * Sample rule that samples all targets
   *
   * @return AcceptAll SampleRule
   */
  private static SampleRules getAcceptAll() {
    String JSON = SampleRules.getJSON("AcceptAll", 1000, 1, 1.0, "*", "*", "*", null);
    return new SampleRules("AcceptAll", JSON, 1);
  }

  /**
   * Sample rule that samples no targets
   *
   * @return SampleNone SampleRule
   */
  private static SampleRules getSampleNone() {
    String JSON = SampleRules.getJSON("SampleNone", 1000, 0, 0.0, "*", "*", "*", null);
    return new SampleRules("SampleNone", JSON, 0);
  }

  /**
   * Sample rule that samples no targets at a specific endpoint
   *
   * @return SampleNoneAtEndpoint SampleRule
   */
  private static SampleRules getSampleNoneAtEndpoint() {
    String JSON =
        SampleRules.getJSON(
            "SampleNoneAtEndpoint", 1000, 0, 0.0, "*", "*", "/importantEndpoint", null);
    return new SampleRules("SampleNoneAtEndpoint", JSON, 0);
  }

  /**
   * Sample rule that samples Post method targets at a rate of .1
   *
   * @return PostRule SampleRule
   */
  private static SampleRules getMethodRule() {
    String JSON = SampleRules.getJSON("PostRule", 10, 1, 0.1, "*", "POST", "*", null);
    return new SampleRules("PostRule", JSON, .11);
  }

  /**
   * Sample rule that samples all targets at a specific endpoint
   *
   * @return ImportantEndpoint SampleRule
   */
  private static SampleRules getImportantRule() {
    String JSON =
        SampleRules.getJSON("ImportantEndpoint", 1, 1, 1.0, "*", "*", "/importantEndpoint", null);
    return new SampleRules("ImportantEndpoint", JSON, 1);
  }

  /**
   * Sample rule that samples targets with certain attributes at a specific endpoint at a rate of .5
   *
   * @return AttributeAtEndpoint SampleRule
   */
  private static SampleRules getAttributeatEndpoint() {
    JSONObject attributes = new JSONObject();
    attributes.put("user", "service");
    String JSON =
        SampleRules.getJSON("AttributeAtEndpoint", 8, 1, .5, "*", "*", "/getSampled", attributes);
    return new SampleRules("AttributeAtEndpoint", JSON, .51);
  }

  /**
   * Sample rule that samples all targets with no reservoir at a rate of .8
   *
   * @return LowReservoir SampleRule
   */
  private static SampleRules getlowReservoirHighRate() {
    String JSON = SampleRules.getJSON("LowReservoir", 10, 0, .8, "*", "*", "*", null);
    return new SampleRules("LowReservoir", JSON, .80);
  }

  /**
   * Sample rule that samples 500 targets and the rest at a rate of 0
   *
   * @return HighReservoir SampleRule
   */
  public static SampleRules getHighReservoirLowRate() {
    String JSON = SampleRules.getJSON("HighReservoir", 2000, 500, 0.0, "*", "*", "*", null);
    return new SampleRules("HighReservoir", JSON, .50);
  }

  /**
   * Sample rule that samples 500 targets and the rest at a rate of .5
   *
   * @return HighReservoir SampleRule
   */
  public static SampleRules getMixedReservoir() {
    String JSON = SampleRules.getJSON("MixedReservoir", 2000, 500, 0.5, "*", "*", "*", null);
    return new SampleRules("MixedReservoir", JSON, .75);
  }

  /**
   * Sample rule that samples targets that have important attribute at a rate of .5
   *
   * @return ImportantAttribute SampleRule
   */
  private static SampleRules getImportantAttribute() {
    JSONObject attributes = new JSONObject();
    attributes.put("user", "admin");
    String JSON = SampleRules.getJSON("ImportantAttribute", 2, 1, .5, "*", "*", "*", attributes);
    return new SampleRules("ImportantAttribute", JSON, .51);
  }

  /**
   * Sample rule that samples targets that have multiple attributes of importance at a rate of .4
   *
   * @return MultipleAttributes SampleRule
   */
  private static SampleRules getMultipleAttribute() {
    JSONObject attributes = new JSONObject();
    attributes.put("user", "admin");
    attributes.put("required", "true");
    String JSON = SampleRules.getJSON("MultipleAttributes", 9, 1, .4, "*", "*", "*", attributes);
    return new SampleRules("MultipleAttributes", JSON, .41);
  }

  /**
   * Default sample Rule that exists in Xray backend by default
   *
   * @return Default SampleRule
   */
  private static SampleRules getDefaultRule() {
    String JSON = "Default";
    return new SampleRules("Default", JSON, .06);
  }

  /**
   * Sample rule that samples targets that have an important service name at a rate of .4
   *
   * @return ImportantServiceName SampleRule
   */
  private static SampleRules getServiceNameRule() {
    String JSON =
        SampleRules.getJSON(
            "ImportantServiceName", 3, 1, 1.0, "ImportantServiceName", "*", "*", null);
    return new SampleRules("ImportantServiceName", JSON, 1);
  }

  /**
   * get all sample rules to test individually except for reservoir rules
   *
   * @return list of SampleRules
   */
  public static SampleRules[] getSampleRules() {
    return new SampleRules[]{
      getSampleNone(),
      getAcceptAll(),
      getImportantRule(),
      getImportantAttribute(),
      getAttributeatEndpoint(),
      getlowReservoirHighRate(),
      getMethodRule(),
      getMultipleAttribute(),
      getDefaultRule(),
      getServiceNameRule(),
      getSampleNoneAtEndpoint()
    };
  }

  /**
   * get a list of rules that tests priority
   *
   * @return list of SampleRules
   */
  public static SampleRules[] getPriorityRules() {
    return new SampleRules[]{
      getImportantRule(),
      getImportantAttribute(),
      getAttributeatEndpoint(),
      getMethodRule(),
      getServiceNameRule()
    };
  }

  /**
   * get a list of rules that tests reservoirs
   *
   * @return list of SampleRules
   */
  public static SampleRules[] getReservoirRules() {
    return new SampleRules[]{getHighReservoirLowRate(), getMixedReservoir()};
  }
}
