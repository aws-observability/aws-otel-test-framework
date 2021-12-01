apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: adot-collector
  namespace: default
  labels:
    app: aws-adot
    component: adot-collector
spec:
  selector:
    matchLabels:
      app: aws-adot
      component: adot-collector
  serviceName: adot-collector-service
  template:
    metadata:
      labels:
        app: aws-adot
        component: adot-collector
    spec:
      serviceAccountName: adot-collector-service-account
      securityContext:
        fsGroup: 65534
      containers:
        - image: ${AocRepo}:${AocTag}
          name: adot-collector
          imagePullPolicy: Always
          command:
            - "/awscollector"
            - "--config=/conf/adot-collector-config.yaml"
          env:
            - name: OTEL_RESOURCE_ATTRIBUTES
              value: "ClusterName=${ClusterName}"
          resources:
            limits:
              cpu: 2
              memory: 2Gi
            requests:
              cpu: 200m
              memory: 400Mi
          volumeMounts:
            - name: adot-collector-config-volume
              mountPath: /conf
      volumes:
        - configMap:
            name: adot-collector-config
            items:
              - key: adot-collector-config
                path: adot-collector-config.yaml
          name: adot-collector-config-volume