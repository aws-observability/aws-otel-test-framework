FROM alpine:latest

RUN apk --no-cache add curl bash git dumb-init openssl

ENTRYPOINT ["/usr/bin/dumb-init", "--"]