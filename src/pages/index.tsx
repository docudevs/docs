import Link from '@docusaurus/Link';
import Heading from '@theme/Heading';
import Layout from '@theme/Layout';
import clsx from 'clsx';

import styles from './index.module.css';

const coreFeatures = [
    {
        title: 'AI Analysis',
        description: 'Use conversational prompts to summarize, classify, or reason across entire documents.',
        to: '/docs/core/ai-analysis',
    },
    {
        title: 'Schema Generation',
        description: 'Let DocuDevs derive JSON schemas from sample documents before you wire them into SDKs.',
        to: '/docs/core/schema-generation',
    },
    {
        title: 'Map-Reduce Extraction',
        description: 'Chunk very long documents, keep headers in sync, and aggregate clean row sets.',
        to: '/docs/core/map-reduce-extraction',
    },
    {
        title: 'Batch Processing',
        description: 'Submit tens or thousands of files at once and monitor progress per fragment.',
        to: '/docs/core/batch-processing',
    },
    {
        title: 'Knowledge Search',
        description: 'Attach knowledge-base search tools so extraction prompts can cite trusted facts.',
        to: '/docs/core/knowledge-search',
    },
];

export default function Home() {
    return (
        <Layout description="Guides and API references for DocuDevs.ai">
            <header className={clsx('hero hero--primary', styles.heroBanner)}>
                <div className="container">
                    <Heading as="h1">DocuDevs Documentation</Heading>
                    <p className={styles.tagline}>
                        Everything you need to automate document processing, from basic uploads to knowledge-aware enrichment.
                    </p>
                    <div className={styles.buttons}>
                        <Link className="button button--secondary button--lg" to="/docs/intro">
                            Start with the Quick Start
                        </Link>
                        <Link className="button button--outline button--lg margin-left--md" to="/docs/openapi/reference/DocuDevsApi">
                            Explore the API
                        </Link>
                    </div>
                </div>
            </header>
            <main>
                <section className={styles.featuresSection}>
                    <div className="container">
                        <Heading as="h2">Core Features</Heading>
                        <div className={styles.featuresGrid}>
                            {coreFeatures.map((feature) => (
                                <Link key={feature.title} className={styles.featureCard} to={feature.to}>
                                    <h3 className={styles.featureTitle}>{feature.title}</h3>
                                    <p className={styles.featureDescription}>{feature.description}</p>
                                    <span className={styles.featureLink}>Read the guide →</span>
                                </Link>
                            ))}
                        </div>
                    </div>
                </section>
            </main>
        </Layout>
    );
}
