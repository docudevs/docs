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
          id: "openapi/resolve-llm-provider",
          label: "resolveLlmProvider",
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
      label: "knowledge-bases",
      items: [
        {
          type: "doc",
          id: "openapi/list-knowledge-bases",
          label: "listKnowledgeBases",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/promote-knowledge-base",
          label: "promoteKnowledgeBase",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/get-knowledge-base",
          label: "getKnowledgeBase",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/demote-knowledge-base",
          label: "demoteKnowledgeBase",
          className: "api-method delete",
        },
      ],
    },
    {
      type: "category",
      label: "LLM Providers",
      items: [
        {
          type: "doc",
          id: "openapi/list-llm-keys",
          label: "listLlmKeys",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/update-llm-key",
          label: "updateLlmKey",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "openapi/list-llm-providers",
          label: "listLlmProviders",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/create-llm-provider",
          label: "createLlmProvider",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/get-llm-provider",
          label: "getLlmProvider",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/delete-llm-provider",
          label: "deleteLlmProvider",
          className: "api-method delete",
        },
        {
          type: "doc",
          id: "openapi/update-llm-provider",
          label: "updateLlmProvider",
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
          id: "openapi/list-ocr-keys",
          label: "listOcrKeys",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/update-ocr-key",
          label: "updateOcrKey",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "openapi/list-ocr-providers",
          label: "listOcrProviders",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/create-ocr-provider",
          label: "createOcrProvider",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "openapi/get-ocr-provider",
          label: "getOcrProvider",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "openapi/delete-ocr-provider",
          label: "deleteOcrProvider",
          className: "api-method delete",
        },
        {
          type: "doc",
          id: "openapi/update-ocr-provider",
          label: "updateOcrProvider",
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
          id: "openapi/resolve-ocr-provider",
          label: "resolveOcrProvider",
          className: "api-method get",
        },
      ],
    },
  ],
};

export default sidebar.apisidebar;
