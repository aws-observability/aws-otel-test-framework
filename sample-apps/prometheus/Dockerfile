FROM golang:1.23 as build

# Update CA certificates to fix TLS handshake issues on ARM64
RUN apt-get update && apt-get install -y ca-certificates && update-ca-certificates && rm -rf /var/lib/apt/lists/*

WORKDIR $GOPATH/main
COPY . .
RUN go env -w GOPROXY=direct
RUN GO111MODULE=on go mod download
RUN GO111MODULE=on CGO_ENABLED=0 GOOS=linux go build -o=/bin/main .

FROM scratch
COPY --from=build /bin/main /bin/main
ENTRYPOINT ["/bin/main"]
