import type { SidebarsConfig } from "@docusaurus/plugin-content-docs";

const sidebar: SidebarsConfig = {
  apisidebar: [
    {
      type: "doc",
      id: "openapi/docudevs-api",
    },
    {
      type: "category",
      label: "cases",
      items: [
        {
          type: "doc",
          id: "openapi/list-cases",
          label: "listCases",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/create-case",
          label: "createCase",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/get-case",
          label: "getCase",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/update-case",
          label: "updateCase",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "openapi/delete-case",
          label: "deleteCase",
          className: "api-method delete",
        },
        {
          type: "doc",
          id: "openapi/list-case-documents",
          label: "listCaseDocuments",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/upload-case-document",
          label: "uploadCaseDocument",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/get-case-document",
          label: "getCaseDocument",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/delete-case-document",
          label: "deleteCaseDocument",
          className: "api-method delete",
        },
        {
          type: "doc",
          id: "openapi/upload-case-document-legacy",
          label: "uploadCaseDocumentLegacy",
          className: "menu__list-item--deprecated api-method post",
        },
      ],
    },
    {
      type: "category",
      label: "configuration",
      items: [
        {
          type: "doc",
          id: "openapi/list-configurations",
          label: "listConfigurations",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/get-configuration",
          label: "getConfiguration",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/save-configuration",
          label: "saveConfiguration",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/delete-configuration",
          label: "deleteConfiguration",
          className: "api-method delete",
        },
      ],
    },
    {
      type: "category",
      label: "batch",
      items: [
        {
          type: "doc",
          id: "openapi/create-batch",
          label: "createBatch",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/process-batch",
          label: "processBatch",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/schedule-batch",
          label: "scheduleBatch",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/upload-batch-document",
          label: "uploadBatchDocument",
          className: "api-method post",
        },
      ],
    },
    {
      type: "category",
      label: "document",
      items: [
        {
          type: "doc",
          id: "openapi/generate-schema",
          label: "generateSchema",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/ocr-document-sync",
          label: "ocrDocumentSync",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/ocr-document",
          label: "ocrDocument",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/process-document",
          label: "processDocument",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/process-document-with-configuration",
          label: "processDocumentWithConfiguration",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/upload-document",
          label: "uploadDocument",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/upload-files",
          label: "uploadFiles",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/upload-files-sync",
          label: "uploadFilesSync",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/upload-template",
          label: "uploadTemplate",
          className: "api-method post",
        },
      ],
    },
    {
      type: "category",
      label: "Internal LLM",
      items: [
        {
          type: "doc",
          id: "openapi/resolve",
          label: "resolve",
          className: "api-method get",
        },
      ],
    },
    {
      type: "category",
      label: "job",
      items: [
        {
          type: "doc",
          id: "openapi/get-dependencies",
          label: "getDependencies",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/list-jobs",
          label: "listJobs",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/result",
          label: "result",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/result-csv",
          label: "resultCsv",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/result-excel",
          label: "resultExcel",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/result-json",
          label: "resultJson",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/status",
          label: "status",
          className: "api-method get",
        },
      ],
    },
    {
      type: "category",
      label: "LLM Providers",
      items: [
        {
          type: "doc",
          id: "openapi/list-keys",
          label: "listKeys",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/update-key",
          label: "updateKey",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "openapi/list-providers",
          label: "listProviders",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/create",
          label: "create",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/get",
          label: "get",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/delete",
          label: "delete",
          className: "api-method delete",
        },
        {
          type: "doc",
          id: "openapi/update",
          label: "update",
          className: "api-method patch",
        },
      ],
    },
    {
      type: "category",
      label: "Azure OCR Providers",
      items: [
        {
          type: "doc",
          id: "openapi/list-keys-1",
          label: "listKeys_1",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/update-key-1",
          label: "updateKey_1",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "openapi/list-providers-1",
          label: "listProviders_1",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/create-1",
          label: "create_1",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/get-1",
          label: "get_1",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/delete-1",
          label: "delete_1",
          className: "api-method delete",
        },
        {
          type: "doc",
          id: "openapi/update-1",
          label: "update_1",
          className: "api-method patch",
        },
      ],
    },
    {
      type: "category",
      label: "operations",
      items: [
        {
          type: "doc",
          id: "openapi/submit-operation",
          label: "submitOperation",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/get-operation-status",
          label: "getOperationStatus",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/get-operation-result",
          label: "getOperationResult",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/create-generative-task",
          label: "createGenerativeTask",
          className: "api-method post",
        },
      ],
    },
    {
      type: "category",
      label: "settings",
      items: [
        {
          type: "doc",
          id: "openapi/settings",
          label: "settings",
          className: "api-method get",
        },
      ],
    },
    {
      type: "category",
      label: "template",
      items: [
        {
          type: "doc",
          id: "openapi/fill",
          label: "fill",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/list-templates",
          label: "listTemplates",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/metadata",
          label: "metadata",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/delete-template",
          label: "deleteTemplate",
          className: "api-method delete",
        },
      ],
    },
    {
      type: "category",
      label: "UNTAGGED",
      items: [
        {
          type: "doc",
          id: "openapi/resolve-1",
          label: "resolve_1",
          className: "api-method get",
        },
      ],
    },
  ],
};

export default sidebar.apisidebar;
