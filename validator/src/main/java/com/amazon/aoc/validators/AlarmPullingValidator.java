package com.amazon.aoc.validators;

import com.amazon.aoc.callers.ICaller;
import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.fileconfigs.FileConfig;
import com.amazon.aoc.helpers.RetryHelper;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.services.CloudWatchAlarmService;
import com.amazon.aoc.services.CloudWatchService;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AlarmPullingValidator implements IValidator {
  private Context context;
  private Integer pullDuration;
  private Integer pullTimes;
  private CloudWatchAlarmService cloudWatchAlarmService;
  private CloudWatchService cloudWatchService;

  private static final String SOAKING_NAMESPACE = "AWSOtelCollector/SoakingTest";
  private static final String TEST_CASE_DIM_KEY = "testcase";
  private static final String METRIC_NAME = "Success";

  @Override
  public void init(
      Context context,
      ValidationConfig validationConfig,
      ICaller caller,
      FileConfig expectedDataTemplate)
      throws Exception {
    this.context = context;
    this.pullDuration = validationConfig.getPullingDuration();
    this.pullTimes = validationConfig.getPullingTimes();
    this.cloudWatchAlarmService = new CloudWatchAlarmService(context.getRegion());
    this.cloudWatchService = new CloudWatchService(context.getRegion());
  }

  @Override
  public void validate() throws Exception {
    Collections.sort(context.getAlarmNameList());
    Dimension dimension =
        new Dimension().withName(TEST_CASE_DIM_KEY).withValue(context.getTestcase());
    RetryHelper.retry(
        this.pullTimes,
        this.pullDuration * 1000,
        false,
        () -> {
          List<MetricAlarm> alarmList =
              this.cloudWatchAlarmService.listAlarms(context.getAlarmNameList());

          // compare the alarm name
          alarmList.sort(Comparator.comparing(MetricAlarm::getAlarmName));
          for (int i = 0; i != context.getAlarmNameList().size(); ++i) {
            if (!context.getAlarmNameList().get(i).equals(alarmList.get(i).getAlarmName())) {
              log.error("alarm {} cannot be found", context.getAlarmNameList().get(i));
              // emit soaking validation metric
              cloudWatchService.putMetricData(SOAKING_NAMESPACE, METRIC_NAME, 0.0, dimension);
              System.exit(1);
            }
          }

          // check the status of the alarms, exit if one of them is alarming
          for (MetricAlarm metricAlarm : alarmList) {
            log.info(metricAlarm.getStateValue());
            if (!metricAlarm.getStateValue().equals("OK")) {
              log.error(
                  "alarm {} is alarming, status is {}, "
                      + "metric is {}, "
                      + "matric error : {}, "
                      + "failing to bake",
                  metricAlarm.getAlarmName(),
                  metricAlarm.getStateValue(),
                  metricAlarm.getMetrics(),
                  metricAlarm.getStateReason());
              cloudWatchService.putMetricData(SOAKING_NAMESPACE, METRIC_NAME, 0.0, dimension);
              System.exit(1);
            }
          }

          log.info("all alarms look good, continue to bake");
          cloudWatchService.putMetricData(SOAKING_NAMESPACE, METRIC_NAME, 1.0, dimension);

          // throw a dummy exception here to make it retry
          throw new BaseException(ExceptionCode.ALARM_BAKING);
        });
  }
}
