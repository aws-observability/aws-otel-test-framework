'use strict';

const tracer = require('./tracer')('example-http-client');
// eslint-disable-next-line import/order
const http = require('http');

/** A function which makes requests and handles response. */
function makeRequest() {
  // span corresponds to outgoing requests. Here, we have manually created
  // the span, which is created to track work that happens outside of the
  // request lifecycle entirely.
  const span = tracer.startSpan('makeRequest');
  tracer.withSpan(span, () => {
    http.get({
      host: 'localhost',
      port: 8080,
      path: '/helloworld',
    }, (response) => {
      const body = [];
      response.on('data', (chunk) => body.push(chunk));
      response.on('end', () => {
        console.log(body.toString());
        span.end();
      });
    });
  });
  returnTraceId(span);

  // The process must live for at least the interval past any traces that
  // must be exported, or some risk being lost if they are recorded after the
  // last export.
  console.log('Sleeping 5 seconds before shutdown to ensure all records are flushed.');
  setTimeout(() => { console.log('Completed.'); }, 5000);
}

function returnTraceId(span) {
  const traceId = span.context().traceId.toString();
  const xrayTraceId = "1-" + traceId.substring(0, 8) + "-" + traceId.substring(8);
  return xrayTraceId;
}

makeRequest();
