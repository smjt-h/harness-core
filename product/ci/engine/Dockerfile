# Create image for ci-lite-engine which will orchestrate steps in a stage
#
# Build the ci-engine docker image using:
# > bazel build --platforms=@io_bazel_rules_go//go/toolchain:linux_amd64 //product/ci/engine/...
# > docker build -t harness/ci-lite-engine:<tag> -f product/ci/engine/Dockerfile $(bazel info bazel-bin)/product/ci/engine/

FROM alpine:3.12 AS builder
RUN apk add --update \
    curl \
    && rm -rf /var/cache/apk/*
RUN apk add git

RUN adduser -g harness -s /bin/sh -D -u 1000 harness
USER harness:harness

From scratch
COPY --from=builder /usr/bin/git /usr/local/bin/
COPY engine_/engine /usr/local/bin/ci-lite-engine
COPY --from=0 /etc/passwd /etc/passwd
COPY --from=builder /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/

USER harness
CMD ["/usr/local/bin/ci-lite-engine"]