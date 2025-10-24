# ==================================================
# STAGE 1: Build da Aplicação Angular
# ==================================================
FROM node:22-alpine AS builder

LABEL maintainer="Bruno Moraes"
LABEL description="Frontend Angular - Sistema de Gestão de Benefícios"

WORKDIR /app

# Copiar package files
COPY package*.json ./

# Instalar dependências
RUN npm ci && npm cache clean --force

# Copiar código-fonte
COPY . .

# Build de produção
RUN npm run build

# ==================================================
# STAGE 2: Servidor Nginx (Imagem Final)
# ==================================================
FROM nginx:1.25-alpine

LABEL maintainer="Bruno Moraes"
LABEL description="Frontend Angular com Nginx"

# Remover conteúdo padrão do Nginx
RUN rm -rf /usr/share/nginx/html/*

# Copiar build do Angular
COPY --from=builder /app/dist/frontend-angular/browser /usr/share/nginx/html

# Copiar configuração customizada do Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expor porta 80
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:80 || exit 1

# Comando para iniciar o Nginx
CMD ["nginx", "-g", "daemon off;"]