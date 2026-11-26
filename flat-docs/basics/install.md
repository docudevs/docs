---
title: Installation & Setup
description: Install the DocuDevs Python SDK, configure your API key, and verify your setup with a test extraction.
sidebar_position: 2
---
<!-- Generated from docs/basics/install.md. Do not edit by hand. -->

# Installation & Setup

Get up and running with DocuDevs in just a few minutes. This guide covers everything you need to start processing documents with our platform.

## Prerequisites

- **Python 3.10+** (for SDK usage)
- **API Key** from DocuDevs platform
- **Internet connection** for API access

## Step 1: Get Your API Key

Your API key is required to authenticate with the DocuDevs platform.

### Creating an API Key

1. **Sign up** at [DocuDevs.ai](https://docudevs.ai)
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

**pip**

```bash
# Install the latest version
pip install docu-devs-api-client

# Or install a specific version
pip install docu-devs-api-client==1.15.0
```

**poetry**

```bash
# Add to your project
poetry add docu-devs-api-client

# Or specify version

poetry add docu-devs-api-client==1.15.0

```

**conda**

```bash
# Install from PyPI via conda
conda install -c conda-forge pip
pip install docu-devs-api-client
```

**Java (Maven)**

```xml
<dependency>
  <groupId>ai.docudevs</groupId>
  <artifactId>docudevs-java-sdk</artifactId>
  <version>0.4.0</version>
</dependency>
```

**Java (Gradle)**

```kotlin
implementation("ai.docudevs:docudevs-java-sdk:0.4.0")
```

The Java SDK requires Java 17 or newer. The Gradle line is for the Kotlin DSL.

### Verify Installation

```bash
python -c "import docudevs; print('DocuDevs SDK installed successfully!')"
```

## Step 3: Configure Your Environment

Set up your API key for easy access across your projects. The examples in this documentation and the CLI read the key from the `API_KEY` environment variable; the Python client takes it as its `token` argument.

**Environment Variable**

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

**.env File**

Create a `.env` file in your project root:

```bash
# .env file
API_KEY=your-api-key-here
```

Then load it with `load_dotenv()` from `python-dotenv` before you create the client, and read the key with `os.getenv("API_KEY")`.

**Install python-dotenv:**

```bash
pip install python-dotenv
```

The Python client takes the key as its `token` argument. Reading it from the environment keeps the key out of your source code.

## Step 4: Test Your Connection

Verify everything is working by listing your saved configurations. A `200` status code means the API key is valid and the API is reachable:

**cURL**

```bash
# List your saved configurations to confirm the API key works
STATUS=$(curl -s -S -o /dev/null -w "%{http_code}" "https://api.docudevs.ai/configuration" \
  -H "Authorization: $API_KEY")

if [ "$STATUS" = "200" ]; then
  echo "Connection successful! Status code: $STATUS"
else
  echo "Connection failed! Status code: $STATUS" >&2
  exit 1
fi
```

**Python**

```python
import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    api_key = os.getenv("API_KEY")
    if not api_key:
        raise ValueError("API_KEY environment variable not set")

    client = DocuDevsClient(token=api_key)

    # List your saved configurations to confirm the API key works
    response = await client.list_configurations()

    if response.status_code == 200:
        print(f"Connection successful! Status code: {response.status_code}")
    else:
        print(f"Connection failed! Status code: {response.status_code}")


asyncio.run(main())
```

**Java**

```java
import ai.docudevs.client.DocuDevsApiException;

// List your saved configurations to confirm the API key works.
// The Java client throws on any status outside 2xx instead of returning it.
try {
    client.listConfigurations();
    System.out.println("Connection successful! Status code: 200");
} catch (DocuDevsApiException error) {
    System.out.println("Connection failed! Status code: " + error.getStatusCode());
}
```

**CLI**

```bash
docudevs list-configurations
```

The cURL example does not need the SDK. The CLI example prints nothing and exits with status 0 when the key is rejected, so it cannot verify a key: use the cURL or Python variant for that. To process a document next, follow the [Quick Start](https://docs.docudevs.ai/docs/getting-started/quick-start).

## Alternative Integration Methods

### Direct API Usage (cURL)

If you prefer direct API calls without the SDK, select the cURL tab of any example in this documentation. Requests go to `https://api.docudevs.ai` and send the key in the `Authorization` header.

### CLI Tool

The CLI tool is included with the SDK installation and provides convenient command-line access to DocuDevs:

```bash
# CLI is included with SDK installation
pip install docu-devs-api-client

# Set up authentication
export API_KEY=your-api-key-here

# List the available commands
docudevs --help
```

Every command group has its own help, for example `docudevs cases --help`. The most common commands:

| Task | Command |
| --- | --- |
| Upload, process and wait for a result | `docudevs process` |
| OCR-only processing | `docudevs ocr-only` |
| Process with a saved configuration | `docudevs process <file> --configuration <name>` |
| Manage cases | `docudevs cases create`, `docudevs cases list`, `docudevs cases upload-document` |
| Manage configurations | `docudevs list-configurations`, `docudevs save-configuration` |
| Manage templates | `docudevs list-templates`, `docudevs fill` |
| Run operations such as error analysis | `docudevs operations error-analysis` |
| Check a job | `docudevs status`, `docudevs result`, `docudevs wait` |

#### CLI Authentication

The CLI supports multiple authentication methods:

1. **Environment Variable (Recommended)**:

   ```bash
   export API_KEY=your-api-key-here
   ```

2. **Alternative Environment Variable**: the CLI also reads `DOCUDEVS_TOKEN`, and it takes precedence over `API_KEY` when both are set.

   ```bash
   export DOCUDEVS_TOKEN=your-api-key-here
   ```

3. **Command Line Option**: pass `--token your-api-key-here` before the command name.

#### CLI Features

- **High-level Commands**: `process` and `ocr-only` handle the full workflow
- **Case Management**: Create and manage document cases with `docudevs cases`
- **Configuration Management**: Save and reuse processing configurations  
- **Template Operations**: Fill and manage document templates
- **Operations**: Submit and monitor advanced operations like error analysis
- **Automatic Waiting**: Commands automatically wait for results by default
- **Environment Integration**: Respects environment variables for authentication

For complete CLI documentation including all commands and options, see the **[CLI Reference](https://docs.docudevs.ai/docs/reference/cli-reference)**.

## Configuration Options

### API Endpoint Configuration

The client uses the production endpoint `https://api.docudevs.ai` by default. Pass `api_url` to `DocuDevsClient` to use a different endpoint, and `--api-url` before the command name in the CLI.

### Timeout Configuration

Waiting for a result is bounded by the `timeout` argument of `wait_until_ready`, in seconds (default 180), and by `--timeout` on CLI commands that wait.

## Troubleshooting

### Common Issues

#### Authentication Errors

```text
401 Unauthorized - Invalid API key
```

**Solution**: Verify your API key is correct and active

#### Network Errors

```text
Connection timeout or network unreachable
```

**Solutions**:

- Check internet connection
- Verify firewall settings
- Try different network if behind corporate firewall

#### Import Errors

```text
ModuleNotFoundError: No module named 'docudevs'
```

**Solutions**:

- Reinstall the SDK: `pip install --upgrade docu-devs-api-client`
- Check Python environment: `pip list | grep docudevs`
- Verify Python version: `python --version`

#### Environment Variable Issues

```text
API key not found or None
```

**Solutions**:

- Check environment variable: `echo $API_KEY`
- Restart terminal/IDE after setting variables
- Use absolute path for .env files

### Getting Help

If you encounter issues:

1. **Check the [Troubleshooting Guide](https://docs.docudevs.ai/docs/integration/troubleshooting)**
2. **Review [Common Error Codes](https://docs.docudevs.ai/docs/reference/error-codes)**
3. **Contact Support**: [support@docudevs.ai](mailto:support@docudevs.ai)

## Next Steps

Now that you have DocuDevs installed and configured:

### Quick Start

- **[5-Minute Tutorial](https://docs.docudevs.ai/docs/getting-started/quick-start)** - Process your first document
- **[First Document Guide](https://docs.docudevs.ai/docs/getting-started/first-document)** - Comprehensive walkthrough

### Learn Core Features

- **[Simple Documents](https://docs.docudevs.ai/docs/basics/SimpleDocuments)** - Basic document processing
- **[Cases](https://docs.docudevs.ai/docs/advanced/cases)** - Organize related documents
- **[Templates](https://docs.docudevs.ai/docs/templates/)** - Reusable processing workflows

### Explore Advanced Features

- **[SDK Methods](https://docs.docudevs.ai/docs/reference/sdk-methods)** - Complete method reference
- **[Best Practices](https://docs.docudevs.ai/docs/integration/best-practices)** - Production deployment tips
- **[Use Cases](https://docs.docudevs.ai/docs/integration/use-cases)** - Real-world examples

Ready to process your first document? Continue with the [Quick Start tutorial](https://docs.docudevs.ai/docs/getting-started/quick-start)!
