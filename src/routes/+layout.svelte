<script lang="ts">
	import { page } from '$app/stores';
</script>

<header id="navigation" class="p-navigation is-dark">
	<div class="p-navigation__row">
		<div class="p-navigation__banner">
			<div class="p-navigation__tagged-logo">
				<a class="p-navigation__link" href="/">
					<span class="p-navigation__logo-title">DocuDevs</span>
				</a>
			</div>
			<a href="#navigation" class="p-navigation__toggle--open" title="menu">Menu</a>
			<a href="#navigation-closed" class="p-navigation__toggle--close" title="close menu"
			>Close menu</a
			>
		</div>
		<nav class="p-navigation__nav" aria-label="Navigation">
			<ul class="p-navigation__items">

					<li
						class="p-navigation__item"
						class:is-selected={$page.url.pathname === '/'}
						aria-current={$page.url.pathname === '/' ? 'page' : null}
					>
						<a class="p-navigation__link" href='/'>Home</a>
					</li>
				<li
					class="p-navigation__item"
					class:is-selected={$page.url.pathname === '/documentation'}
					aria-current={$page.url.pathname === '/documentation' ? 'page' : null}
				>
					<a class="p-navigation__link" href='/documentation'>Documentation</a>
				</li>

				<li class="p-navigation__item">
					<a
						class="p-navigation__link"
						target="_blank"
						href="https://docudevs.ai"
					>Docudevs.ai</a
					>
				</li>
			</ul>
		</nav>
	</div>
</header>

<main class="p-strip is-shallow">
	<div class="row">
		<slot />
	</div>
</main>
