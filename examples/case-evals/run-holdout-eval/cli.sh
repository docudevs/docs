#!/usr/bin/env bash
set -euo pipefail

# docs:start
cat > variants.json <<'EOF'
[{"name": "baseline"}, {"name": "with-rule", "memo_include": ["m_88"]}]
EOF
cat > config.json <<'EOF'
{"llmLevel": "MINI"}
EOF

docudevs eval holdout 42 --variants-file variants.json --sample-size 200 --seed 7 --grouping-mode nearDuplicate --grouping-threshold 0.98 --config-file config.json --wait
# docs:end
