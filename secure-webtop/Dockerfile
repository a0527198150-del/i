FROM lscr.io/linuxserver/webtop:alpine-xfce

# Webtop already includes nginx. Install htpasswd for bcrypt password generation.
RUN command -v nginx && apk add --no-cache apache2-utils

# Railway terminates public TLS. The only exposed service port is nginx:8080.
COPY webtop-proxy.conf /etc/nginx/webtop-proxy.conf
COPY --chmod=0755 webtop-proxy-run /custom-services.d/webtop-proxy
