FROM alpine:3.24

# Pick up security fixes published after the base image was built. The image is rebuilt weekly by
# .github/workflows/update-traffic-generator-image.yml, so the series above only needs a bump
# when it nears end of life.
RUN apk --no-cache upgrade \
    && apk --no-cache add curl bash git dumb-init openssl

ENTRYPOINT ["/usr/bin/dumb-init", "--"]