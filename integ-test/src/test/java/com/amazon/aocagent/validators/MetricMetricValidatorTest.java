/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazon.aocagent.validators;

import com.amazon.aocagent.fileconfigs.ExpectedMetric;
import com.amazon.aocagent.models.Context;
import com.amazonaws.services.cloudwatch.model.Metric;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class MetricMetricValidatorTest {

  @Test
  public void testGetExpectedMetricList()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Context context = new Context();
    context.setInstanceId("i-035a644c403f96199");
    context.setExpectedMetric(ExpectedMetric.DEFAULT_EXPECTED_METRIC);

    Method method = MetricValidator.class.getDeclaredMethod("getExpectedMetricList", Context.class);
    method.setAccessible(true);
    List<Metric> metricList = (List<Metric>) method.invoke(new MetricValidator(), context);
    System.out.println(metricList);
  }
}
