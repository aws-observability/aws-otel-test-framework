FROM --platform=$BUILDPLATFORM golang:1.23 AS build
ARG TARGETARCH

# Update CA certificates to fix TLS handshake issues on ARM64
RUN apt-get update && apt-get install -y ca-certificates && update-ca-certificates && rm -rf /var/lib/apt/lists/*

WORKDIR $GOPATH/main
COPY . .

RUN GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=$TARGETARCH go build -o=/bin/main .

FROM scratch
COPY --from=build /bin/main /bin/main
COPY certificates certificates
ENTRYPOINT ["/bin/main"]
