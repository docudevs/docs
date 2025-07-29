---
title: Installation & Setup
sidebar_position: 2
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Installation & Setup

Get up and running with DocuDevs in just a few minutes. This guide covers everything you need to start processing documents with our platform.

## Prerequisites

- **Python 3.8+** (for SDK usage)
- **API Key** from DocuDevs platform
- **Internet connection** for API access

## Step 1: Get Your API Key

Your API key is required to authenticate with the DocuDevs platform.

### Creating an API Key

1. **Sign up** at [DocuDevs.ai](https://docudevs.ai) (currently in private beta)
2. **Navigate** to Settings → API Keys after login
3. **Create** a new API key for your project
4. **Copy** the API key (it looks like: `1234567890abcdef1234567890abcdef`)

:::warning Security Note
- Store your API key securely like a password
- Never commit API keys to code repositories
- Don't include API keys in client-side code
- Revoke and regenerate if compromised
:::

### Managing API Keys

- **Multiple Keys**: Create separate keys for different projects/environments
- **Revocation**: Revoke keys anytime from the portal
- **Regeneration**: Generate new keys as needed
- **Monitoring**: Track usage per API key

## Step 2: Install the SDK

<Tabs
  defaultValue="pip"
  values={[
    {label: 'pip', value: 'pip'},
    {label: 'poetry', value: 'poetry'},
    {label: 'conda', value: 'conda'},
  ]}>
  <TabItem value="pip">
```bash
# Install the latest version
pip install docudevs-sdk

# Or install a specific version
pip install docudevs-sdk==1.0.0
```
  </TabItem>
  <TabItem value="poetry">
```bash
# Add to your project
poetry add docudevs-sdk

# Or specify version
poetry add docudevs-sdk==1.0.0
```
  </TabItem>
  <TabItem value="conda">
```bash
# Install from PyPI via conda
conda install -c conda-forge pip
pip install docudevs-sdk
```
  </TabItem>
</Tabs>

### Verify Installation

```python
# Test your installation
from docudevs.docudevs_client import DocuDevsClient
print("DocuDevs SDK installed successfully!")
```

## Step 3: Configure Your Environment

Set up your API key for easy access across your projects.

<Tabs
  defaultValue="env_var"
  values={[
    {label: 'Environment Variable', value: 'env_var'},
    {label: 'Python Code', value: 'python'},
    {label: '.env File', value: 'env_file'},
  ]}>
  <TabItem value="env_var">

**Linux / macOS:**
```bash
export API_KEY="your-api-key-here"
```

**Windows (Command Prompt):**
```cmd
set API_KEY=your-api-key-here
```

**Windows (PowerShell):**
```powershell
$env:API_KEY="your-api-key-here"
```

  </TabItem>
  <TabItem value="python">
```python
import os
from docudevs.docudevs_client import DocuDevsClient

# Option 1: Direct initialization
client = DocuDevsClient(token="your-api-key-here")

# Option 2: From environment variable
api_key = os.getenv('API_KEY')
client = DocuDevsClient(token=api_key)

# Option 3: With error handling
api_key = os.getenv('API_KEY')
if not api_key:
    raise ValueError("API_KEY environment variable not set")

client = DocuDevsClient(token=api_key)
```
  </TabItem>
  <TabItem value="env_file">
Create a `.env` file in your project root:

```bash
# .env file
API_KEY=your-api-key-here
DOCUDEVS_API_URL=https://api.docudevs.ai
```

Then load it in your Python code:
```python
from dotenv import load_dotenv
import os
from docudevs.docudevs_client import DocuDevsClient

# Load environment variables from .env file
load_dotenv()

client = DocuDevsClient(token=os.getenv('API_KEY'))
```

**Install python-dotenv:**
```bash
pip install python-dotenv
```
  </TabItem>
</Tabs>

## Step 4: Test Your Connection

Verify everything is working with a quick test:

```python
import asyncio
import os
from docudevs.docudevs_client import DocuDevsClient

async def test_connection():
    # Initialize client
    client = DocuDevsClient(token=os.getenv('API_KEY'))
    
    try:
        # Test connection by listing configurations
        response = await client.list_configurations()
        print("✅ Connection successful!")
        print(f"Status code: {response.status_code}")
        return True
        
    except Exception as e:
        print("❌ Connection failed!")
        print(f"Error: {e}")
        return False

# Run the test
success = asyncio.run(test_connection())
if success:
    print("🎉 You're ready to start processing documents!")
else:
    print("🔧 Please check your API key and network connection.")
```

## Alternative Integration Methods

### Direct API Usage (cURL)

If you prefer direct API calls without the SDK:

```bash
# Test your API key
curl -X GET https://api.docudevs.ai/configuration \
  -H "Authorization: Bearer $API_KEY"

# Upload and process a document
curl -X POST https://api.docudevs.ai/document/upload-files \
  -H "Authorization: Bearer $API_KEY" \
  -F "document=@your-document.pdf"
```

### CLI Tool

The CLI tool is included with the SDK installation and provides convenient command-line access to all DocuDevs functionality:

```bash
# CLI is included with SDK installation
pip install docudevs-api-client

# Set up authentication (choose one method)
export DOCUDEVS_TOKEN=your-api-key-here
# OR
export API_KEY=your-api-key-here
# OR pass token with each command using --token

# High-level document processing (upload + process + wait for result)
docudevs process document.pdf --prompt="Extract invoice data"

# OCR-only processing
docudevs ocr document.pdf --format=markdown

# Upload and process with a saved configuration
docudevs process-with-config document.pdf my-invoice-config

# Case management
docudevs create-case "Invoice Batch 2024-01"
docudevs list-cases
docudevs upload-case-document 123 document.pdf

# Configuration management
docudevs list-configurations
docudevs save-configuration my-config config.json

# Template management
docudevs list-templates
docudevs fill template-name data.json

# Operations (error analysis, etc.)
docudevs error-analysis job-guid-here

# Low-level commands (for advanced usage)
docudevs upload-document document.pdf    # Upload only
docudevs process-document GUID config.json  # Process uploaded doc
docudevs status GUID                     # Check job status
docudevs result GUID                     # Get job result
docudevs wait GUID                       # Wait for completion
```

#### CLI Authentication

The CLI supports multiple authentication methods:

1. **Environment Variable (Recommended)**:
   ```bash
   export DOCUDEVS_TOKEN=your-api-key-here
   docudevs process document.pdf
   ```

2. **Legacy Environment Variable**:
   ```bash
   export API_KEY=your-api-key-here
   docudevs process document.pdf
   ```

3. **Command Line Option**:
   ```bash
   docudevs --token=your-api-key-here process document.pdf
   ```

#### CLI Features

- **High-level Commands**: `process`, `ocr`, `process-with-config` handle the full workflow
- **Case Management**: Create and manage document cases
- **Configuration Management**: Save and reuse processing configurations  
- **Template Operations**: Fill and manage document templates
- **Operations**: Submit and monitor advanced operations like error analysis
- **Automatic Waiting**: Commands automatically wait for results by default
- **Environment Integration**: Respects environment variables for authentication

For complete CLI documentation including all commands and options, see the **[CLI Reference](/docs/reference/cli-reference)**.

## Configuration Options

### API Endpoint Configuration

```python
# Default endpoint (production)
client = DocuDevsClient(token=api_key)

# Custom endpoint (if using different environment)
client = DocuDevsClient(
    api_url="https://api.staging.docudevs.ai",
    token=api_key
)
```

### Timeout Configuration

```python
# Configure custom timeouts
client = DocuDevsClient(
    token=api_key,
    timeout=30  # 30 seconds timeout
)
```

## Troubleshooting

### Common Issues

#### Authentication Errors
```
401 Unauthorized - Invalid API key
```
**Solution**: Verify your API key is correct and active

#### Network Errors
```
Connection timeout or network unreachable
```
**Solutions**:
- Check internet connection
- Verify firewall settings
- Try different network if behind corporate firewall

#### Import Errors
```
ModuleNotFoundError: No module named 'docudevs'
```
**Solutions**:
- Reinstall the SDK: `pip install --upgrade docudevs-sdk`
- Check Python environment: `pip list | grep docudevs`
- Verify Python version: `python --version`

#### Environment Variable Issues
```
API key not found or None
```
**Solutions**:
- Check environment variable: `echo $API_KEY`
- Restart terminal/IDE after setting variables
- Use absolute path for .env files

### Getting Help

If you encounter issues:

1. **Check the [Troubleshooting Guide](/docs/integration/troubleshooting)**
2. **Review [Common Error Codes](/docs/reference/error-codes)**
3. **Contact Support**: [support@docudevs.ai](mailto:support@docudevs.ai)

## Next Steps

Now that you have DocuDevs installed and configured:

### Quick Start
- **[5-Minute Tutorial](/docs/getting-started/quick-start)** - Process your first document
- **[First Document Guide](/docs/getting-started/first-document)** - Comprehensive walkthrough

### Learn Core Features
- **[Simple Documents](/docs/basics/SimpleDocuments)** - Basic document processing
- **[Cases](/docs/advanced/cases)** - Organize related documents
- **[Templates](/docs/templates/Templates)** - Reusable processing workflows

### Explore Advanced Features
- **[SDK Methods](/docs/reference/sdk-methods)** - Complete method reference
- **[Best Practices](/docs/integration/best-practices)** - Production deployment tips
- **[Use Cases](/docs/integration/use-cases)** - Real-world examples

Ready to process your first document? Continue with the [Quick Start tutorial](/docs/getting-started/quick-start)!
