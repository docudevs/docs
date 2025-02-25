#!/bin/bash
set -e

# Example 2: Invoice - provide instructions
# This script demonstrates sending a document with instructions to the API

# Check if API_KEY is set
if [ -z "$API_KEY" ]; then
  echo "Error: API_KEY environment variable is not set"
  echo "Usage: API_KEY=your_api_key ./example2_instructions.sh"
  exit 1
fi

echo "Running Example 2: Invoice - provide instructions"



# Call the API
echo "Calling the API..."
response=$(curl -s -S -X POST https://api.docudevs.ai/document/upload-files/sync \
     -H "Authorization: $API_KEY" \
     -F "document=@invoice.pdf" \
     -F "instructions=@instructions.txt")

# Output the response
echo "API Response:"
echo "$response" | json_pp

echo "Example 2 completed successfully"