FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG EXPO_PUBLIC_API_URL=/api
ARG EXPO_PUBLIC_COGNITO_REGION=us-east-1
ARG EXPO_PUBLIC_COGNITO_CLIENT_ID
ENV EXPO_PUBLIC_API_URL=$EXPO_PUBLIC_API_URL
ENV EXPO_PUBLIC_COGNITO_REGION=$EXPO_PUBLIC_COGNITO_REGION
ENV EXPO_PUBLIC_COGNITO_CLIENT_ID=$EXPO_PUBLIC_COGNITO_CLIENT_ID
RUN npm run build:web

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --retries=10 CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1

