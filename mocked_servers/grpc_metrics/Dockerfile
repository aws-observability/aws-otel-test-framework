FROM --platform=$BUILDPLATFORM golang:1.24 AS build
ARG TARGETARCH
WORKDIR $GOPATH/main
COPY . .

RUN GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=$TARGETARCH go build -o=/bin/main .

FROM scratch
COPY --from=build /bin/main /bin/main
ENTRYPOINT ["/bin/main"]
