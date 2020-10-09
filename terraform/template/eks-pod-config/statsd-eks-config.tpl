sample_app:
  image: ${data_emitter_image}
  command:
    - "/bin/sh"
    - "-c"
    - "while true; do echo 'testCounter.metric_${testing_id}:1.7|c|@0.1|#key:val,key1:val1' | socat -v -t 0 - UDP:127.0.0.1:8125; sleep 1; echo 'testGauge.metric_${testing_id}:1.8|c|@0.1|#keyg:valg,keyg1:valg1' | socat -v -t 0 - UDP:127.0.0.1:8125; sleep 1; done"
  args: []