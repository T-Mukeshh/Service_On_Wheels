# ──────────────────────────────────────────────
# Stage 1: Build the Angular SSR application
# ──────────────────────────────────────────────
FROM node:22-alpine AS build

WORKDIR /app

# Copy package files first to leverage Docker layer caching
COPY package.json package-lock.json ./
RUN npm ci

# Copy source code and build for production
COPY . .
RUN npm run build

# ──────────────────────────────────────────────
# Stage 2: Run the SSR Node.js server
# ──────────────────────────────────────────────
FROM node:22-alpine AS runtime

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the built output from the build stage
COPY --from=build /app/dist ./dist

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Expose the SSR server port
EXPOSE 4000

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:4000/ || exit 1

# Run the Angular SSR server
CMD ["node", "dist/customer-app/server/server.mjs"]
