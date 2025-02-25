---
title: Connect
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

To connect to the DocuDevs platform, you need to have a valid API-KEY.
This can be created in the DocuDevs.ai portal.

You find the API-KEY in the settings / api-key part after login.
The api-key looks like a long string of characters, for example: `1234567890abcdef1234567890abcdef`.

All examples assume you have set your api-key in the environment variable `API_KEY`.
<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">
Linux / MacOS:
```sh
export API_KEY=1234567890abcdef1234567890abcdef
```
Windows:
```sh
set API_KEY=1234567890abcdef1234567890abcdef
```
  </TabItem>
  <TabItem value="python">
First, install the DocuDevs Python client:
```bash
pip install docu-devs-api-client
```

Then you can use it in your code:
```python
import os
from docudevs.docudevs_client import DocuDevsClient

api_key = os.getenv('API_KEY')
client = DocuDevsClient(token=api_key)
```
  </TabItem>
</Tabs>

Remember to store api-key like a password and do not share it with others.
- You can revoke the api-key in the DocuDevs.ai portal at any time.
- You can create multiple api-keys for different projects or purposes.
- Don't include API keys in client code or commit them to code repositories. If you do, you should revoke the key and generate a new one.
