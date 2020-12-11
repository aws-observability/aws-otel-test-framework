FROM node:14

WORKDIR /app

COPY *.js ./
COPY package.json ./
COPY metrichandler.proto ./

RUN npm install

CMD ["node", "metrics_handler_server.js"]
