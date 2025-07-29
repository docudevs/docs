# Docudevs docs dev guide
The documentation should explain all features available in the API. Best way to know what has been from previous version is to analyze docudevs-sdk client, and compare the examples in the documentation.

Check from `docudevs-sdk/docudevs_client.py` what new methods has been added. Good place to find new functionality is also `docudevs-sdk/tests` which have a working example for each new functionality.

the documentation is a Docusaurus project, and the markup files are in `docudevs-doc/docs`.

Check from `docudevs-doc/sidebars.ts` if there is already a suitable sidebar item for the missing documentation. 
Analyze all markdown if they are missing instructions to use new methods or parameters from the docudevs-sdk. 

Update documents for missing features. Use Tabs for python and CuRl documentation. Generate the cUrl based on examples from existing examples and the python docudevs-sdk test cases you know. Use same logic for the python part, using the docudevs-sdk/tests as example.

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">
```sh
curl -s -S -X POST https://api.docudevs.ai/document/upload-files \
     -H "Authorization: $API_KEY" \
     -F "document=@invoice.pdf"
```
  </TabItem>
  <TabItem value="python">
```python
from docudevs.docudevs_client import DocuDevsClient
import os

# Initialize the client with your API key
client = DocuDevsClient(token=os.getenv('API_KEY'))

# Process the document
with open("invoice.pdf", "rb") as f:
    document = f.read()

guid = await client.submit_document(document=document, document_mime_type="application/pdf")
result = await client.wait_until_ready(guid)
print(result)
```
</TabItem>
</Tabs>

For understanding what this new logic does, there should be a specification of all changes in `features` folder (as a markdown document). 