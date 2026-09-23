FROM node:20-slim AS builder

WORKDIR /app

# Real-size COPY -- should show the actual on-disk size (respecting .dockerignore).
COPY node_modules ./node_modules
COPY src ./src

# Known-expensive RUN pattern -- should be flagged (no size shown, never invented).
RUN apt-get update && apt-get install -y curl

# ADD from a URL -- a real Docker feature (downloads at build time), so this
# should show "not calculable" (network source), never "source not found".
ADD https://example.com/healthcheck.sh /usr/local/bin/healthcheck.sh

# Installs the curl TOOL only, cleanly -- should NOT be flagged. Installing
# curl is not the same as invoking it to download something. (Separated from
# the RUN above by the ADD, so this also isn't grouped with it as
# "consecutive RUNs that could be combined".)
RUN apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Multi-stage COPY --from -- should be recognized, never crash, honestly "not calculable".
FROM node:20-slim
COPY --from=builder /app /app

CMD ["node", "src/server.js"]
