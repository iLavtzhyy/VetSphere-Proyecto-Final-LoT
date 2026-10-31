# ── VetSphere · Frontend (Angular) ────────────────────────────────────────
# Etapa 1: compilar el SPA
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build -- --configuration production

# Etapa 2: servir con nginx (hace de proxy de /api hacia el backend)
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/vetsphere-frontend /usr/share/nginx/html
EXPOSE 80
