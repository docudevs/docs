#!/bin/bash
set -e

# Example 1: Invoice - minimal using just the document
# This script demonstrates sending only a document to the API

# Check if API_KEY is set
if [ -z "$API_KEY" ]; then
  echo "Error: API_KEY environment variable is not set"
  echo "Usage: API_KEY=your_api_key ./example1_minimal.sh"
  exit 1
fi

echo "Running Example 1: Invoice - minimal using just the document"


# Call the API
echo "Calling the API..."
response=$(curl -s -S -X POST https://api.docudevs.ai/document/upload-files/sync \
     -H "Authorization: $API_KEY" \
     -F "document=@invoice.pdf")

# Output the response
echo "API Response:"
echo "$response" 

echo "Example 1 completed successfully"