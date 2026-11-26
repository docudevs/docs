import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const output = join(dirname(fileURLToPath(import.meta.url)), 'live-fixtures');

const escapePdf = (text) => text.replace(/[\\()]/g, '\\$&');

function pdf({ pages, fields = [] }) {
  const objects = [];
  const add = (body) => objects.push(body) && objects.length;
  const catalog = add('');
  const pageTree = add('');
  const font = add('<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>');
  const pageIds = pages.map((lines) => {
    const stream = ['BT', '/F1 12 Tf', '14 TL', '72 740 Td', ...lines.map((line) => `(${escapePdf(line)}) '`), 'ET'].join('\n');
    const content = add(`<< /Length ${stream.length} >>\nstream\n${stream}\nendstream`);
    return { content, id: add('') };
  });
  const widgets = fields.map((name, index) => {
    const top = 600 - index * 40;
    return add(
      `<< /Type /Annot /Subtype /Widget /FT /Tx /T (${escapePdf(name)}) /V () /DA (/Helv 12 Tf 0 g) /F 4 /Rect [72 ${top - 20} 300 ${top}] /Border [0 0 1] /P ${pageIds[0].id} 0 R >>`,
    );
  });
  pageIds.forEach(({ content, id }, index) => {
    const annots = index === 0 && widgets.length > 0 ? ` /Annots [${widgets.map((w) => `${w} 0 R`).join(' ')}]` : '';
    objects[id - 1] = `<< /Type /Page /Parent ${pageTree} 0 R /MediaBox [0 0 612 792] /Contents ${content} 0 R /Resources << /Font << /F1 ${font} 0 R /Helv ${font} 0 R >> >>${annots} >>`;
  });
  objects[pageTree - 1] = `<< /Type /Pages /Kids [${pageIds.map(({ id }) => `${id} 0 R`).join(' ')}] /Count ${pages.length} >>`;
  const acroForm =
    widgets.length > 0
      ? ` /AcroForm << /Fields [${widgets.map((w) => `${w} 0 R`).join(' ')}] /DA (/Helv 12 Tf 0 g) /DR << /Font << /Helv ${font} 0 R >> >> >>`
      : '';
  objects[catalog - 1] = `<< /Type /Catalog /Pages ${pageTree} 0 R${acroForm} >>`;
  let body = '%PDF-1.4\n';
  const offsets = objects.map((object, index) => {
    const offset = Buffer.byteLength(body, 'latin1');
    body += `${index + 1} 0 obj\n${object}\nendobj\n`;
    return offset;
  });
  const xref = Buffer.byteLength(body, 'latin1');
  body += `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`;
  body += offsets.map((offset) => `${String(offset).padStart(10, '0')} 00000 n \n`).join('');
  body += `trailer\n<< /Size ${objects.length + 1} /Root ${catalog} 0 R >>\nstartxref\n${xref}\n%%EOF\n`;
  return Buffer.from(body, 'latin1');
}

const crcTable = Array.from({ length: 256 }, (_, n) => {
  let c = n;
  for (let k = 0; k < 8; k += 1) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
  return c >>> 0;
});
const crc32 = (buffer) => {
  let crc = 0xffffffff;
  for (const byte of buffer) crc = crcTable[(crc ^ byte) & 0xff] ^ (crc >>> 8);
  return (crc ^ 0xffffffff) >>> 0;
};

function zip(entries) {
  const locals = [];
  const centrals = [];
  let offset = 0;
  for (const [name, text] of Object.entries(entries)) {
    const nameBuffer = Buffer.from(name);
    const data = Buffer.from(text);
    const crc = crc32(data);
    const local = Buffer.alloc(30);
    local.writeUInt32LE(0x04034b50, 0);
    local.writeUInt16LE(20, 4);
    local.writeUInt32LE(crc, 14);
    local.writeUInt32LE(data.length, 18);
    local.writeUInt32LE(data.length, 22);
    local.writeUInt16LE(nameBuffer.length, 26);
    const central = Buffer.alloc(46);
    central.writeUInt32LE(0x02014b50, 0);
    central.writeUInt16LE(20, 4);
    central.writeUInt16LE(20, 6);
    central.writeUInt32LE(crc, 16);
    central.writeUInt32LE(data.length, 20);
    central.writeUInt32LE(data.length, 24);
    central.writeUInt16LE(nameBuffer.length, 28);
    central.writeUInt32LE(offset, 42);
    locals.push(local, nameBuffer, data);
    centrals.push(central, nameBuffer);
    offset += local.length + nameBuffer.length + data.length;
  }
  const centralBuffer = Buffer.concat(centrals);
  const end = Buffer.alloc(22);
  end.writeUInt32LE(0x06054b50, 0);
  end.writeUInt16LE(Object.keys(entries).length, 8);
  end.writeUInt16LE(Object.keys(entries).length, 10);
  end.writeUInt32LE(centralBuffer.length, 12);
  end.writeUInt32LE(offset, 16);
  return Buffer.concat([...locals, centralBuffer, end]);
}

const xmlHeader = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n';
const escapeXml = (text) => text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

function docx(paragraphs) {
  const body = paragraphs.map((text) => `<w:p><w:r><w:t>${escapeXml(text)}</w:t></w:r></w:p>`).join('');
  return zip({
    '[Content_Types].xml': `${xmlHeader}<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>`,
    '_rels/.rels': `${xmlHeader}<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>`,
    'word/document.xml': `${xmlHeader}<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>${body}</w:body></w:document>`,
  });
}

const columnName = (index) => String.fromCharCode(65 + index);

function xlsx(rows) {
  const sheetRows = rows
    .map(
      (row, r) =>
        `<row r="${r + 1}">${row
          .map((value, c) => {
            const ref = `${columnName(c)}${r + 1}`;
            return typeof value === 'number'
              ? `<c r="${ref}"><v>${value}</v></c>`
              : `<c r="${ref}" t="inlineStr"><is><t>${escapeXml(value)}</t></is></c>`;
          })
          .join('')}</row>`,
    )
    .join('');
  return zip({
    '[Content_Types].xml': `${xmlHeader}<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>`,
    '_rels/.rels': `${xmlHeader}<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>`,
    'xl/workbook.xml': `${xmlHeader}<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Sheet1" sheetId="1" r:id="rId1"/></sheets></workbook>`,
    'xl/_rels/workbook.xml.rels': `${xmlHeader}<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>`,
    'xl/worksheets/sheet1.xml': `${xmlHeader}<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>${sheetRows}</sheetData></worksheet>`,
  });
}

const invoiceLines = (number, vendor, issued, items) => [
  `Invoice ${number}`,
  vendor,
  `Issue date: ${issued}`,
  'Bill to: Sample Customer Ltd',
  '',
  ...items.map(([description, quantity, price]) => `${description}   ${quantity} x ${price.toFixed(2)}`),
  '',
  `Total due: ${items.reduce((sum, [, quantity, price]) => sum + quantity * price, 0).toFixed(2)} EUR`,
  'Payment terms: 30 days',
];

const invoice = (number) =>
  pdf({
    pages: [
      invoiceLines(`INV-${number}`, 'Sample Vendor Co.', '2024-03-15', [
        ['Consulting hours', 10, 95],
        ['Software licence', 1, 450],
        ['Travel expenses', 1, 120.5],
      ]),
    ],
  });

const longDocument = (title, pageCount) =>
  pdf({
    pages: Array.from({ length: pageCount }, (_, index) => [
      `${title}`,
      `Section ${index + 1}`,
      '',
      `This page describes section ${index + 1} of the sample document.`,
      'It holds neutral placeholder text so that page ranges can be tested.',
      `Reference: SAMPLE-${String(index + 1).padStart(3, '0')}`,
    ]),
  });

const textPage = (lines) => pdf({ pages: [lines] });

const files = {
  'invoice.pdf': invoice('1001'),
  'invoice1.pdf': invoice('1002'),
  'invoice2.pdf': invoice('1003'),
  'invoice3.pdf': invoice('1004'),
  'new-invoice.pdf': invoice('1005'),
  'sample-invoice.pdf': invoice('1006'),
  'sample_invoice.pdf': invoice('1007'),
  'your-document.pdf': invoice('1008'),
  'scan.pdf': invoice('1009'),
  'handwritten.pdf': invoice('1010'),
  'invoice_template.pdf': textPage(['Invoice template', 'Invoice number: ________', 'Vendor: ________', 'Total: ________']),
  'large-invoice.pdf': longDocument('Sample invoice, long form', 12),
  'large-report.pdf': longDocument('Sample annual report', 12),
  'report.pdf': longDocument('Sample quarterly report', 12),
  'document.pdf': longDocument('Sample document', 12),
  'safety-data-sheet.pdf': longDocument('Sample safety data sheet', 12),
  'contract.pdf': textPage([
    'Services Agreement',
    'Between Sample Vendor Co. (the Supplier) and Sample Customer Ltd (the Customer).',
    '1. Term. This agreement runs for twelve months from the effective date.',
    '2. Fees. The Customer pays the Supplier 5,000.00 EUR per month.',
    '3. Termination. Either party may terminate with 30 days written notice.',
    '4. Governing law. This agreement is governed by the laws of the Netherlands.',
  ]),
  'slip-2025.pdf': textPage(['Pay slip 2025', 'Employee: Sample Employee', 'Gross: 3,000.00 EUR', 'Net: 2,250.00 EUR']),
  'slip-2026-draft.pdf': textPage(['Pay slip 2026 (draft)', 'Employee: Sample Employee', 'Gross: 3,100.00 EUR', 'Net: 2,330.00 EUR']),
  'registration.pdf': textPage(['Vehicle registration', 'Plate: SAMPLE-01', 'Owner: Sample Customer Ltd', 'Registered: 2023-06-01', 'Valid until: 2028-06-01']),
  'warranty-list.pdf': longDocument('Sample warranty list', 12),
  'fillable-form.pdf': pdf({ pages: [['Registration form', 'Name:', 'Email:']], fields: ['name', 'email'] }),
  'contract.docx': docx([
    'Services Agreement',
    'Between Sample Vendor Co. and Sample Customer Ltd.',
    'The Customer pays the Supplier 5,000.00 EUR per month.',
    'Either party may terminate with 30 days written notice.',
  ]),
  'submission.xlsx': xlsx([
    ['Vendor', 'Month', 'Amount'],
    ['Sample Vendor Co.', 'January', 1200],
    ['Sample Vendor Co.', 'February', 1350],
    ['Other Vendor Ltd', 'January', 800],
  ]),
  'template.xlsx': xlsx([
    ['Vendor', 'Invoice number', 'Total'],
    ['', '', ''],
  ]),
  'vendors-2024.csv': 'vendor_id,vendor_name,country\nV001,Sample Vendor Co.,NL\nV002,Other Vendor Ltd,DE\nV003,Third Supplier BV,BE\n',
  'vendor-catalog.csv': 'sku,description,unit_price\nA-100,Widget,9.50\nA-200,Gadget,14.25\nA-300,Gizmo,3.10\n',
  'invoice-schema.json': `${JSON.stringify(
    {
      type: 'object',
      properties: {
        invoiceNumber: { type: 'string', description: 'Invoice number' },
        vendor: { type: 'string', description: 'Name of the vendor' },
        total: { type: 'number', description: 'Total amount due' },
      },
      required: ['invoiceNumber', 'vendor', 'total'],
    },
    null,
    2,
  )}\n`,
  'producer-bundle.zip': zip({ 'README.txt': 'Sample producer bundle used only to hash a file.\n' }),
  'soccer.jpg': Buffer.from(
    '/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAAMCAgMCAgMDAwMEAwMEBQgFBQQEBQoHBwYIDAoMDAsKCwsNDhIQDQ4RDgsLEBYQERMUFRUVDA8XGBYUGBIUFRT/wAALCAABAAEBAREA/8QAFAABAAAAAAAAAAAAAAAAAAAACP/EABQQAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEAAD8AVN//2Q==',
    'base64',
  ),
};

mkdirSync(output, { recursive: true });
for (const [name, content] of Object.entries(files)) writeFileSync(join(output, name), content);
console.log(`Wrote ${Object.keys(files).length} fixtures to ${output}`);
