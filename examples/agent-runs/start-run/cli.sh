#!/usr/bin/env bash
set -euo pipefail

# docs:start
cat > params.json <<'EOF'
{"job_id": 55, "max_rows": 10}
EOF

docudevs agent run start 42 investigate-flagged --params-file params.json --budget-tokens 60000 --wait
# docs:end
