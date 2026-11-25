---
title: Installation & Setup
description: Install the DocuDevs Python SDK, configure your API key, and verify your setup with a test extraction.
sidebar_position: 2
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Installation & Setup

Get up and running with DocuDevs in just a few minutes. This guide covers everything you need to start processing documents with our platform.

## Prerequisites

- **Python 3.10+** (for SDK usage)
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
pip install docu-devs-api-client

# Or install a specific version
pip install docu-devs-api-client==1.15.0
```

  </TabItem>
  <TabItem value="poetry">
```bash
# Add to your project
poetry add docu-devs-api-client

# Or specify version

poetry add docu-devs-api-client==1.15.0

```
  </TabItem>
  <TabItem value="conda">
```bash
# Install from PyPI via conda
conda install -c conda-forge pip
pip install docu-devs-api-client
```

  </TabItem>
</Tabs>

### Verify Installation

```bash
python -c "import docudevs; print('DocuDevs SDK installed successfully!')"
```

## Step 3: Configure Your Environment

Set up your API key for easy access across your projects. The SDK, the CLI and the examples in this documentation read the key from the `API_KEY` environment variable.

<Tabs
  defaultValue="env_var"
  values={[
    {label: 'Environment Variable', value: 'env_var'},
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
  <TabItem value="env_file">
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

  </TabItem>
</Tabs>

The Python client takes the key as its `token` argument. Reading it from the environment keeps the key out of your source code.

## Step 4: Test Your Connection

Verify everything is working by listing your saved configurations. A `200` status code means the API key is valid and the API is reachable:

<Example name="install/verify-connection" />

The cURL example does not need the SDK. To process a document next, follow the [Quick Start](/docs/getting-started/quick-start).

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

2. **Alternative Environment Variable**:

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

For complete CLI documentation including all commands and options, see the **[CLI Reference](/docs/reference/cli-reference)**.

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
- **[Templates](/docs/templates/)** - Reusable processing workflows

### Explore Advanced Features

- **[SDK Methods](/docs/reference/sdk-methods)** - Complete method reference
- **[Best Practices](/docs/integration/best-practices)** - Production deployment tips
- **[Use Cases](/docs/integration/use-cases)** - Real-world examples

Ready to process your first document? Continue with the [Quick Start tutorial](/docs/getting-started/quick-start)!
