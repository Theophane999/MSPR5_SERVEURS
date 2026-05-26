#!/bin/sh
set -eu

cat <<EOF >/usr/share/nginx/html/env.js
window.__env = {
  backendMotherUrl: "${BACKEND_MOTHER_PUBLIC_URL:-http://localhost:3200}"
};
EOF

exec nginx -g 'daemon off;'
