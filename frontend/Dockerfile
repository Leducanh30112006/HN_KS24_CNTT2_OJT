# Production Runtime for ReactJS Frontend
FROM nginx:alpine

# Clean default nginx static assets
RUN rm -rf /usr/share/nginx/html/*

# Copy pre-built production bundle
COPY dist /usr/share/nginx/html
COPY nginx-spa.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
