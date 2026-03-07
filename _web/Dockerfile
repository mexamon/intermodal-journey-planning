
FROM node:alpine AS builder
WORKDIR /taurena-roadmap
COPY . .
RUN yarn install && yarn build


FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
WORKDIR /usr/share/nginx/html
RUN rm -rf ./*
COPY --from=builder /taurena-roadmap/build .
ENTRYPOINT ["nginx", "-g", "daemon off;"]