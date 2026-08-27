FROM node:20-slim AS builder

WORKDIR /app

# Real-size COPY -- should show the actual on-disk size (respecting .dockerignore).
COPY node_modules ./node_modules
COPY src ./src

# Known-expensive RUN pattern -- should be flagged (no size shown, never invented).
RUN apt-get update && apt-get install -y curl

# Multi-stage COPY --from -- should be recognized, never crash, honestly "not calculable".
FROM node:20-slim
COPY --from=builder /app /app

CMD ["node", "src/server.js"]
