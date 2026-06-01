import Link from '@docusaurus/Link';
import Layout from '@theme/Layout';
import clsx from 'clsx';

import styles from './index.module.css';

const quickLinks = [
	{
		number: '01',
		label: 'Quick start',
		title: 'Process the first document',
		description: 'Upload a file, run extraction, and inspect the structured JSON result.',
		to: '/docs/getting-started/quick-start'
	},
	{
		number: '02',
		label: 'SDKs',
		title: 'Use Python, Java, or cURL',
		description: 'Copy practical examples for uploads, jobs, templates, and result downloads.',
		to: '/docs/reference/sdk-methods'
	},
	{
		number: '03',
		label: 'API',
		title: 'Call every endpoint',
		description: 'Browse the generated OpenAPI reference with request and response details.',
		to: '/docs/openapi/docudevs-api'
	}
];

const docTracks = [
	{
		title: 'Document processing',
		description: 'OCR, schema generation, structured extraction, map-reduce, and batch runs.',
		to: '/docs/core/schema-generation'
	},
	{
		title: 'Knowledge and evaluation',
		description: 'Knowledge-base search, agent chat, document evidence, and quality checks.',
		to: '/docs/core/knowledge-search'
	},
	{
		title: 'Integration operations',
		description: 'Runtime setup, operational tools, job management, tracing, and troubleshooting.',
		to: '/docs/integration/use-cases'
	},
	{
		title: 'Reference material',
		description: 'SDK methods, billing tokens, limits, error codes, and changelog entries.',
		to: '/docs/reference/sdk-methods'
	}
];

const workflow = [
	{
		step: 'Define',
		copy: 'Shape prompts, schemas, calculated fields, and source-location requirements.'
	},
	{
		step: 'Run',
		copy: 'Use uploads, configurations, batches, or pipelines from the app and APIs.'
	},
	{
		step: 'Verify',
		copy: 'Read job status, traces, source anchors, and quality evaluation outputs.'
	}
];

export default function Home() {
	return (
		<Layout>
			<main className={styles.docsHome}>
				<section className={styles.heroSection}>
					<div className={styles.heroCopy}>
						<p className={styles.eyebrow}>DocuDevs Documentation</p>
						<h1 className={styles.heroTitle}>Build production document workflows faster.</h1>
						<p className={styles.heroText}>
							Practical guides for intelligent document processing: extraction, OCR, map-reduce,
							knowledge search, document evaluation, APIs, SDKs, and integration runtime operations.
						</p>
						<div className={styles.heroActions}>
							<Link
								className={clsx(styles.button, styles.primaryButton)}
								to="/docs/getting-started/quick-start"
							>
								Start quick start
							</Link>
							<Link
								className={clsx(styles.button, styles.secondaryButton)}
								to="/docs/openapi/docudevs-api"
							>
								Open API reference
							</Link>
						</div>
						<div className={styles.heroMeta} aria-label="Documentation coverage">
							<span>OCR</span>
							<span>Extraction</span>
							<span>Knowledge search</span>
							<span>SDKs</span>
						</div>
					</div>

					<div className={styles.heroVisual} aria-label="Documentation map">
						<article className={clsx(styles.posterPanel, styles.posterPink, styles.posterStart)}>
							<div className={styles.posterHeader}>
								<span>01</span>
								<span>Quick start</span>
							</div>
							<div className={styles.posterNumber}>JSON</div>
							<div className={styles.posterNote}>first file to structured output</div>
							<pre className={clsx(styles.codeBlock, styles.compactCodeBlock)}>
								<code>{`client.submit_and_process_document(
  document=document,
  schema=schema,
)`}</code>
							</pre>
						</article>

						<article className={clsx(styles.posterPanel, styles.posterCyan, styles.posterEvidence)}>
							<div className={styles.posterHeader}>
								<span>02</span>
								<span>Evidence</span>
							</div>
							<img
								className={styles.documentImage}
								src="/files/simple_documents/invoice-screenshot.png"
								alt="Example invoice used in the documentation"
							/>
							<div className={styles.sourceLine}>source locations to page boxes</div>
						</article>

						<article
							className={clsx(styles.posterPanel, styles.posterYellow, styles.posterReference)}
						>
							<div className={styles.posterHeader}>
								<span>03</span>
								<span>Reference</span>
							</div>
							<div className={styles.routeList}>
								<span>POST /document/upload-files</span>
								<span>POST /document/process/:guid</span>
								<span>GET /job/status/:guid</span>
								<span>GET /job/result/:guid</span>
							</div>
							<div className={styles.posterNote}>SDK guides and endpoint details</div>
						</article>
					</div>
				</section>

				<section className={styles.quickSection} aria-labelledby="quick-paths">
					<div className={styles.sectionHeader}>
						<p className={styles.eyebrow}>Choose a path</p>
						<h2 id="quick-paths">Start from the job you need done.</h2>
					</div>
					<div className={styles.quickGrid}>
						{quickLinks.map((link) => (
							<Link key={link.title} className={styles.quickCard} to={link.to}>
								<span className={styles.cardNumber}>{link.number}</span>
								<span className={styles.cardLabel}>{link.label}</span>
								<h3>{link.title}</h3>
								<p>{link.description}</p>
								<span className={styles.cardAction}>Read guide</span>
							</Link>
						))}
					</div>
				</section>

				<section className={styles.tracksSection} aria-labelledby="doc-tracks">
					<div className={styles.sectionHeader}>
						<p className={styles.eyebrow}>Documentation tracks</p>
						<h2 id="doc-tracks">From first upload to governed automation.</h2>
					</div>
					<div className={styles.trackGrid}>
						{docTracks.map((track) => (
							<Link key={track.title} className={styles.trackLink} to={track.to}>
								<h3>{track.title}</h3>
								<p>{track.description}</p>
							</Link>
						))}
					</div>
				</section>

				<section className={styles.workflowSection} aria-labelledby="workflow">
					<div>
						<p className={styles.eyebrow}>Operating model</p>
						<h2 id="workflow">The docs follow the real workflow.</h2>
					</div>
					<div className={styles.workflowGrid}>
						{workflow.map((item, index) => (
							<div key={item.step} className={styles.workflowItem}>
								<span>{String(index + 1).padStart(2, '0')}</span>
								<h3>{item.step}</h3>
								<p>{item.copy}</p>
							</div>
						))}
					</div>
				</section>
			</main>
		</Layout>
	);
}
