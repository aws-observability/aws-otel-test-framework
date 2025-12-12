FROM golang:1.23 as build
WORKDIR $GOPATH/main
COPY . .
RUN go env -w GOPROXY=direct
# Retry logic for go mod download to handle transient network issues
RUN for i in 1 2 3; do GO111MODULE=on go mod download && break || sleep 5; done
RUN GO111MODULE=on CGO_ENABLED=0 GOOS=linux go build -o=/bin/main .

FROM scratch
COPY --from=build /bin/main /bin/main
ENTRYPOINT ["/bin/main"]
