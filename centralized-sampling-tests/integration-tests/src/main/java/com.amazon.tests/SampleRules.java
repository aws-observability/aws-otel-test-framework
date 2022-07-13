package com.amazon.tests;

import org.json.simple.JSONObject;

/** File that contains all sample Rules that will be created to be used for testing */
public class SampleRules {
  /**
   * Sample rule that samples all targets
   *
   * @return AcceptAll SampleRule
   */
  private static SampleRule getAcceptAll() {
    return new SampleRule.SampleRuleBuilder(
            GenericConstants.SampleRuleName.AcceptAll.getSampleName(), 1000, 1, 1)
        .build();
  }

  /**
   * Sample rule that samples no targets
   *
   * @return SampleNone SampleRule
   */
  private static SampleRule getSampleNone() {
    return new SampleRule.SampleRuleBuilder(
            GenericConstants.SampleRuleName.SampleNone.getSampleName(), 1000, 0.0, 0.0)
        .setReservoir(0)
        .build();
  }

  /**
   * Sample rule that samples no targets at a specific endpoint
   *
   * @return SampleNoneAtEndpoint SampleRule
   */
  private static SampleRule getSampleNoneAtEndpoint() {
    return new SampleRule.SampleRuleBuilder(
            GenericConstants.SampleRuleName.SampleNoneAtEndpoint.getSampleName(), 1000, 0.0, 0.0)
        .setReservoir(0)
        .setPath("/importantEndpoint")
        .build();
  }

  /**
   * Sample rule that samples Post method targets at a rate of .1
   *
   * @return PostRule SampleRule
   */
  private static SampleRule getMethodRule() {
    return new SampleRule.SampleRuleBuilder(
            GenericConstants.SampleRuleName.PostRule.getSampleName(), 10, .1, .11)
        .setMethod("POST")
        .build();
  }

  /**
   * Sample rule that samples all targets at a specific endpoint
   *
   * @return ImportantEndpoint SampleRule
   */
  private static SampleRule getImportantRule() {
    return new SampleRule.SampleRuleBuilder(
            GenericConstants.SampleRuleName.ImportantEndpoint.getSampleName(), 1, 1.0, 1)
        .setPath("/importantEndpoint")
        .build();
  }

  /**
   * Sample rule that samples targets with certain attributes at a specific endpoint at a rate of .5
   *
   * @return AttributeAtEndpoint SampleRule
   */
  private static SampleRule getAttributeatEndpoint() {
    JSONObject attributes = new JSONObject();
    attributes.put("user", "service");
    return new SampleRule.SampleRuleBuilder(
            GenericConstants.SampleRuleName.AttributeAtEndpoint.getSampleName(), 8, 0.5, .51)
        .setPath("/getSampled")
        .setAttributes(attributes)
        .build();
  }

  /**
   * Sample rule that samples all targets with no reservoir at a rate of .8
   *
   * @return LowReservoir SampleRule
   */
  private static SampleRule getlowReservoirHighRate() {
    return new SampleRule.SampleRuleBuilder(
            GenericConstants.SampleRuleName.LowReservoir.getSampleName(), 10, .8, .80)
        .setReservoir(0)
        .build();
  }

  /**
   * Sample rule that samples 500 targets and the rest at a rate of 0
   *
   * @return HighReservoir SampleRule
   */
  public static SampleRule getHighReservoirLowRate() {
    return new SampleRule.SampleRuleBuilder(
            GenericConstants.SampleRuleName.HighReservoir.getSampleName(), 2000, 0.0, .50)
        .setReservoir(500)
        .build();
  }

  /**
   * Sample rule that samples 500 targets and the rest at a rate of .5
   *
   * @return HighReservoir SampleRule
   */
  public static SampleRule getMixedReservoir() {
    return new SampleRule.SampleRuleBuilder(
            GenericConstants.SampleRuleName.MixedReservoir.getSampleName(), 2000, .5, .75)
        .setReservoir(500)
        .build();
  }

  /**
   * Sample rule that samples targets that have important attribute at a rate of .5
   *
   * @return ImportantAttribute SampleRule
   */
  private static SampleRule getImportantAttribute() {
    JSONObject attributes = new JSONObject();
    attributes.put("user", "admin");
    return new SampleRule.SampleRuleBuilder(
            GenericConstants.SampleRuleName.ImportantAttribute.getSampleName(), 2, .5, .5)
        .setAttributes(attributes)
        .build();
  }

  /**
   * Sample rule that samples targets that have multiple attributes of importance at a rate of .4
   *
   * @return MultipleAttributes SampleRule
   */
  private static SampleRule getMultipleAttribute() {
    JSONObject attributes = new JSONObject();
    attributes.put("user", "admin");
    attributes.put("required", "true");
    return new SampleRule.SampleRuleBuilder(
            GenericConstants.SampleRuleName.MultipleAttributes.getSampleName(), 9, .4, .41)
        .setAttributes(attributes)
        .build();
  }

  /**
   * Default sample Rule that exists in Xray backend by default
   *
   * @return Default SampleRule
   */
  private static SampleRule getDefaultRule() {
    return new SampleRule.SampleRuleBuilder(
            GenericConstants.SampleRuleName.Default.getSampleName(), 10000, .06, .06)
        .build();
  }

  /**
   * Sample rule that samples targets that have an important service name at a rate of .4
   *
   * @return ImportantServiceName SampleRule
   */
  private static SampleRule getServiceNameRule() {
    return new SampleRule.SampleRuleBuilder(
            GenericConstants.SampleRuleName.ImportantServiceName.getSampleName(), 3, 1, 1)
        .setServiceName("ImportantServiceName")
        .build();
  }

  /**
   * get all sample rules to test individually except for reservoir rules
   *
   * @return list of SampleRules
   */
  public static SampleRule[] getSampleRules() {
    return new SampleRule[] {
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
  public static SampleRule[] getPriorityRules() {
    return new SampleRule[] {
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
  public static SampleRule[] getReservoirRules() {
    return new SampleRule[] {getHighReservoirLowRate(), getMixedReservoir()};
  }
}
