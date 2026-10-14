FROM rust:1.99-bookworm AS build
WORKDIR /app
COPY Cargo.toml Cargo.lock ./
COPY src ./src
COPY migrations ./migrations
RUN cargo build --locked --release

FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --uid 10001 --create-home backend
COPY --from=build /app/target/release/forkware-backend /usr/local/bin/forkware-backend
USER backend
ENV PORT=3000 RUST_LOG=info
EXPOSE 3000
CMD ["forkware-backend", "serve"]
