package com.amazon.aoc.validators;

import com.amazon.aoc.callers.ICaller;
import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.fileconfigs.FileConfig;
import com.amazon.aoc.helpers.RetryHelper;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.services.CloudWatchAlarmService;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import lombok.extern.log4j.Log4j2;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Log4j2
public class AlarmPullingValidator implements IValidator {
  private Context context;
  private Integer pullDuration;
  private Integer pullTimes;
  private CloudWatchAlarmService cloudWatchAlarmService;

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
  }

  @Override
  public void validate() throws Exception {
    Collections.sort(context.getAlarmNameList());
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
              log.error("alarm {} can not be found", context.getAlarmNameList().get(i));
              System.exit(1);
            }
          }

          // check the status of the alarms, exit if one of them is alarming
          for (MetricAlarm metricAlarm : alarmList) {
            log.info(metricAlarm.getStateValue());
            if (metricAlarm.getStateValue().equals("ALARM")) {
              log.error(
                  "alarm {} is alarming, metric is {}, failing to bake",
                  metricAlarm.getAlarmName(),
                  metricAlarm.getMetrics());
              System.exit(1);
            }
          }

          log.info("alarms look good, continue to bake");
          // throw a dummy exception here to make it retry
          throw new BaseException(ExceptionCode.ALARM_BAKING);
        });
  }
}
