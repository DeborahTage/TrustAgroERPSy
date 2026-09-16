# Runtime image — package the verified CRA build from `CI=true npm run build`.
# Build order: npm ci && CI=true npm run build  &&  docker compose build
FROM nginx:1.25-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY build /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
CMD ["nginx", "-g", "daemon off;"]
