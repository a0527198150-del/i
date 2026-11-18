FROM lscr.io/linuxserver/webtop:alpine-xfce

# Fixed upstream release and pinned SHA-256 to prevent unverified binary drift.
ADD --chmod=0755 https://github.com/cloudflare/cloudflared/releases/download/2026.11.0/cloudflared-linux-amd64 /usr/local/bin/cloudflared
RUN echo "51f7c68b57ed7269164075d7e789efce5b59355982f06392deeb037d982a51fd  /usr/local/bin/cloudflared" | sha256sum -c -

# LinuxServer supervises this alongside Webtop; image keeps its normal /init.
COPY --chmod=0755 cloudflared-run /custom-services.d/cloudflared
