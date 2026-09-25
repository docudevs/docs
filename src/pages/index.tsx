import Link from '@docusaurus/Link';
import Layout from '@theme/Layout';
import Head from '@docusaurus/Head';
import styles from './index.module.css';

const sections = [
	{
		id: 'processing', title: 'Document processing', description: 'Turn documents into text and structured data.',
		links: [
			['Structured data extraction', '/docs/basics/SimpleDocuments'],
			['OCR and text recognition', '/docs/basics/PlainOCR'],
			['Generate an extraction schema', '/docs/core/schema-generation'],
			['Process documents in batches', '/docs/core/batch-processing'],
		]
	},
	{
		id: 'knowledge', title: 'Knowledge and AI', description: 'Search, analyze, and ask questions about documents.',
		links: [
			['Knowledge search', '/docs/core/knowledge-search'],
			['Agent chat', '/docs/core/agent-chat'],
			['AI document analysis', '/docs/core/ai-analysis'],
			['Source locations and evidence', '/docs/core/source-locations'],
		]
	},
	{
		id: 'workflows', title: 'Workflows and operations', description: 'Manage longer-running work and inspect results.',
		links: [
			['Job management', '/docs/advanced/job-management'],
			['Extraction pipelines', '/docs/reference/pipeline-mode'],
			['Map-reduce extraction', '/docs/core/map-reduce-extraction'],
			['Tracing and debugging', '/docs/advanced/tracing'],
		]
	},
	{
		id: 'reference', title: 'Reference', description: 'Look up methods, commands, and platform behavior.',
		links: [
			['API reference', '/docs/openapi/docudevs-api'],
			['Python SDK methods', '/docs/reference/sdk-methods'],
			['CLI reference', '/docs/reference/cli-reference'],
			['Limits and quotas', '/docs/reference/limits-quotas'],
		]
	},
];

export default function Home() {
	return (
		<Layout>
			<Head>
				<title>Documentation | DocuDevs</title>
				<meta name="description" content="Find DocuDevs tutorials, document processing guides, and API, Python SDK, and CLI references." />
			</Head>
			<main className={styles.home}>
				<aside className={styles.sidebar}>
					<nav aria-label="Documentation home">
						<p className={styles.navLabel}>Documentation</p>
						<a href="#overview">Overview</a>
						<a href="#get-started">Get started</a>
						<p className={styles.navLabel}>Explore the guides</p>
						{sections.map(section => <a key={section.id} href={`#${section.id}`}>{section.title}</a>)}
						<p className={styles.navLabel}>Resources</p>
						<Link to="/docs/reference/error-codes">Error codes</Link>
						<Link to="/docs/reference/sdk-changelog">SDK changelog</Link>
					</nav>
				</aside>
				<div className={styles.content}>
					<header id="overview" className={styles.intro}>
						<p className={styles.eyebrow}>Guides / Tutorials / Reference</p>
						<h1>DocuDevs documentation</h1>
						<p>Learn how to extract structured data, run OCR, and build document workflows with DocuDevs. Start with a tutorial or jump straight to the reference you need.</p>
					</header>

					<section id="get-started" aria-labelledby="start-title" className={styles.startSection}>
						<h2 id="start-title">Get started</h2>
						<div className={styles.startGrid}>
							<Link className={styles.featured} to="/docs/getting-started/quick-start">
								<span className={styles.badge}>5-minute tutorial</span>
								<h3>Process your first document</h3>
								<p>Set up the SDK, upload a file, and retrieve your first structured result.</p>
								<span className={styles.action}>Follow the quick start <span aria-hidden="true">→</span></span>
							</Link>
							<div className={styles.shortcuts}>
								<Link to="/docs/basics/install"><strong>Installation and API keys <span aria-hidden="true">↗</span></strong><span>Prepare your environment and authentication.</span></Link>
								<Link to="/docs/openapi/docudevs-api"><strong>API reference <span aria-hidden="true">↗</span></strong><span>Endpoints, request parameters, and responses.</span></Link>
								<Link to="/docs/reference/sdk-methods"><strong>Python SDK reference <span aria-hidden="true">↗</span></strong><span>Find methods and working code examples.</span></Link>
							</div>
						</div>
					</section>

					<section aria-labelledby="browse-title" className={styles.browse}>
						<div className={styles.sectionHeading}><h2 id="browse-title">Browse the documentation</h2><span>Find a guide for your task</span></div>
						<div className={styles.guideGrid}>
							{sections.map(section => (
								<section id={section.id} key={section.id} className={styles.guide} aria-labelledby={`${section.id}-title`}>
									<h3 id={`${section.id}-title`}>{section.title}</h3>
									<p>{section.description}</p>
									<ul>{section.links.map(([label, to]) => <li key={to}><Link to={to}>{label}<span aria-hidden="true">→</span></Link></li>)}</ul>
								</section>
							))}
						</div>
					</section>
					<section className={styles.help} aria-labelledby="help-title">
						<div><h2 id="help-title">Troubleshooting an integration?</h2><p>Look up an error, inspect a job, or check what changed.</p></div>
						<div className={styles.helpLinks}><Link to="/docs/reference/error-codes">Error codes →</Link><Link to="/docs/advanced/tracing">Tracing →</Link><Link to="/docs/reference/sdk-changelog">SDK changelog →</Link></div>
					</section>
				</div>
			</main>
		</Layout>
	);
}
