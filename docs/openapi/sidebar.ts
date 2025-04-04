import type { SidebarsConfig } from "@docusaurus/plugin-content-docs";

const sidebar: SidebarsConfig = {
  apisidebar: [
    {
      type: "doc",
      id: "openapi/docudevs-api",
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
      label: "document",
      items: [
        {
          type: "doc",
          id: "openapi/process-document",
          label: "processDocument",
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
          id: "openapi/upload-template",
          label: "uploadTemplate",
          className: "api-method post",
        },
      ],
    },
    {
      type: "category",
      label: "job",
      items: [
        {
          type: "doc",
          id: "openapi/result",
          label: "result",
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
  ],
};

export default sidebar.apisidebar;
