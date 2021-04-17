apiVersion: apps/v1
kind: Deployment
metadata:
  name: banana-app
  namespace: ${NAMESPACE}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: banana
  template:
    metadata:
      labels:
        app: banana
    spec:
      containers:
        - name: banana-app
          image: hashicorp/http-echo
          args:
            - "-text=banana"
          resources:
            limits:
              cpu:  100m
              memory: 100Mi
            requests:
              cpu: 50m
              memory: 50Mi
---

kind: Service
apiVersion: v1
metadata:
  name: banana-service
  namespace: ${NAMESPACE}
spec:
  selector:
    app: banana
  ports:
    - port: 5678 # Default port for image

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: apple-app
  namespace: ${NAMESPACE}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: apple
  template:
    metadata:
      labels:
        app: apple
    spec:
      containers:
        - name: apple-app
          image: hashicorp/http-echo
          args:
            - "-text=apple"
          resources:
            limits:
              cpu:  100m
              memory: 100Mi
            requests:
              cpu: 50m
              memory: 50Mi
---

kind: Service
apiVersion: v1
metadata:
  name: apple-service
  namespace: ${NAMESPACE}
spec:
  selector:
    app: apple
  ports:
    - port: 5678 # Default port for image

---

apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  name: ingress-nginx-demo
  namespace: ${NAMESPACE}
spec:
  rules:
  - host: ${EXTERNAL_IP}
    http:
      paths:
        - path: /apple
          backend:
            serviceName: apple-service
            servicePort: 5678
        - path: /banana
          backend:
            serviceName: banana-service
            servicePort: 5678

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: traffic-generator
  namespace: ${NAMESPACE}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: traffic-generator
  template:
    metadata:
      labels:
        app: traffic-generator
    spec:
      containers:
        - name: traffic-generator
          image: ellerbrock/alpine-bash-curl-ssl
          command: ["/bin/bash"]
          args: ["-c", "while :; do curl http://${EXTERNAL_IP}/apple > /dev/null 2>&1; curl http://${EXTERNAL_IP}/banana > /dev/null 2>&1; sleep 1; done"]
          resources:
            limits:
              cpu:  100m
              memory: 100Mi
            requests:
              cpu: 50m
              memory: 50Mi