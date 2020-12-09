FROM node:14

WORKDIR /app

COPY *.js ./
COPY package.json ./
COPY tracehandler.proto ./

RUN npm install

CMD ["node", "trace_handler_server.js"]
