# Production Nginx Container for QuickShop Frontend (Non-root unprivileged)
FROM nginxinc/nginx-unprivileged:alpine

# Clean default nginx files and copy read-only production build assets
USER root
RUN rm -rf /usr/share/nginx/html/*
COPY dist /usr/share/nginx/html
COPY nginx-spa.conf /etc/nginx/conf.d/default.conf

USER nginx
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
