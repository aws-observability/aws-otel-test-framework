# Pinned to a release series (not :latest) so the weekly unattended rebuild cannot move the image to
# a new Alpine release without a reviewed change. The series tag still floats across point releases.
FROM alpine:3.24

# Pick up fixes published after the base image was built. The image is rebuilt weekly by
# .github/workflows/update-traffic-generator-image.yml, so the series above only needs a bump
# when it nears end of life (Alpine 3.24 EOL: 2028-06-01).
RUN apk --no-cache upgrade \
    && apk --no-cache add curl bash dumb-init

ENTRYPOINT ["/usr/bin/dumb-init", "--"]
