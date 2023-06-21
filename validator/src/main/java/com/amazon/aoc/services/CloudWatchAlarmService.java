package com.amazon.aoc.services;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import java.util.List;

public class CloudWatchAlarmService {
  AmazonCloudWatch amazonCloudWatch;

  public CloudWatchAlarmService(String region) {
    amazonCloudWatch = AmazonCloudWatchClientBuilder.standard().withRegion(region).build();
  }

  /**
   * Get alarm list base on name.
   *
   * @param alarmNameList alarm name list
   * @return the list of MetricAlarm Object
   */
  public List<MetricAlarm> listAlarms(List<String> alarmNameList) {
    DescribeAlarmsResult describeAlarmsResult =
        amazonCloudWatch.describeAlarms(new DescribeAlarmsRequest().withAlarmNames(alarmNameList));
    return describeAlarmsResult.getMetricAlarms();
  }
}
