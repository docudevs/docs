import React from 'react';
import Link from '@docusaurus/Link';
import clsx from 'clsx';

export default function MDXA(props) {
  return <Link {...props} className={clsx(props.className)} />;
}
