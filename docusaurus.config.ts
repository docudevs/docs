import type { Config } from '@docusaurus/types';
import { themes as prismThemes } from 'prism-react-renderer';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

const config: Config = {
  title: 'Docudevs.ai Documentation',
  tagline: 'Document automation made easy',
  favicon: 'img/favicon.ico',


  // Set the production url of your site here
  url: 'https://docs.docudevs.ai',
  // Set the /<baseUrl>/ pathname under which your site is served
  // For GitHub pages deployment, it is often '/<projectName>/'
  baseUrl: '/',

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: 'docudevs', // Usually your GitHub org/user name.
  projectName: 'docudevs docs', // Usually your repo name.

  onBrokenLinks: 'warn',
  onBrokenMarkdownLinks: 'warn',

  // Even if you don't use internationalization, you can use this field to set
  // useful metadata like html lang. For example, if your site is Chinese, you
  // may want to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  plugins: [
    [
      'docusaurus-plugin-openapi-docs',
      {
        id: 'docudevs-openapi',
        docsPluginId: 'classic',
        config: {
          docudevs: {
            specPath: 'static/files/docudevs.yaml',
            outputDir: 'docs/openapi',
            sidebarOptions: {
              groupPathsBy: 'tag',
            },
          },
        },
      },
    ]
  ],

  presets: [
    [
      'classic',
      {
        docs: {
          routeBasePath: 'docs',
          sidebarPath: './sidebars.ts',
          docItemComponent: "@theme/ApiItem",
          // Please change this to your repo.
          // Remove this to remove the "edit this page" links.
        },
        blog: false
      },
    ],
  ],

  themes: ['docusaurus-theme-openapi-docs'],

  themeConfig: {
    // OpenAPI-specific configuration
    // Replace with your project's social card
    image: 'img/docusaurus-social-card.jpg',
    colorMode: {
      defaultMode: 'light',
      disableSwitch: false,
      respectPrefersColorScheme: true,
    },
    navbar: {
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'tutorialSidebar',
          position: 'left',
          label: 'Documentation',
        },
        {
          type: 'docSidebar',
          sidebarId: 'apiSidebar',
          position: 'left',
          label: 'API',
        },
        {
          to: '/docs/core/knowledge-search',
          position: 'left',
          label: 'Knowledge Search',
        }
      ],
      style: 'dark',
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Docs',
          items: [
            {
              label: 'Tutorial',
              to: '/docs/intro',
            },
          ],
        },
        {
          title: 'More',
          items: [
            {
              label: 'GitHub',
              href: 'https://github.com/docudevs/docudevs.ai',
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} Docudevs.ai`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula
    },
  },
};

export default config;
