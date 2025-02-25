#!/bin/bash
set -e

# Example 3: Invoice - provide schema
# This script demonstrates sending a document with a JSON schema to the API

# Check if API_KEY is set
if [ -z "$API_KEY" ]; then
  echo "Error: API_KEY environment variable is not set"
  echo "Usage: API_KEY=your_api_key ./example3_schema.sh"
  exit 1
fi

echo "Running Example 3: Invoice - provide schema"

#

# Call the API
echo "Calling the API..."
response=$(curl -s -S -X POST https://api.docudevs.ai/document/upload-files/sync \
     -H "Authorization: $API_KEY" \
     -F "document=@invoice.pdf" \
     -F "schema=@schema.json")

# Output the response
echo "API Response:"
echo "$response" | json_pp

echo "Example 3 completed successfully"