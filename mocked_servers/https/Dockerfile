FROM node:14

WORKDIR /app

COPY certificates ./certificates
COPY *.js ./
COPY package.json ./

RUN npm install

CMD ["node", "index.js"]
