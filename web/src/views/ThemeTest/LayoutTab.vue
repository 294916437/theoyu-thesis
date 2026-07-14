<template>
	<div>
		<!-- 文字排版 -->
		<v-card class="mb-6">
			<v-card-title>文字排版</v-card-title>
			<v-card-text>
				<h1 class="text-h1 mb-2">Heading 1</h1>
				<h2 class="text-h2 mb-2">Heading 2</h2>
				<h3 class="text-h3 mb-2">Heading 3</h3>
				<h4 class="text-h4 mb-2">Heading 4</h4>
				<h5 class="text-h5 mb-2">Heading 5</h5>
				<h6 class="text-h6 mb-4">Heading 6</h6>

				<p class="text-body-1 mb-2">Body 1 - 这是正文文字样式</p>
				<p class="text-body-2 mb-2">Body 2 - 这是较小的正文文字</p>
				<p class="text-caption mb-2">Caption - 这是说明文字</p>
				<p class="text-overline">OVERLINE - 这是上标文字</p>
			</v-card-text>
		</v-card>

		<!-- 网格系统 -->
		<v-card class="mb-6">
			<v-card-title>网格系统</v-card-title>
			<v-card-text>
				<v-row>
					<v-col v-for="n in 12" :key="n" cols="1">
						<v-sheet color="primary" class="pa-2 text-center" rounded>
							<span class="text-caption">{{ n }}</span>
						</v-sheet>
					</v-col>
				</v-row>

				<v-row class="mt-4">
					<v-col cols="12" sm="6" md="4">
						<v-sheet color="secondary" class="pa-4" rounded>
							<div class="text-center">12 / 6 / 4</div>
						</v-sheet>
					</v-col>
					<v-col cols="12" sm="6" md="4">
						<v-sheet color="success" class="pa-4" rounded>
							<div class="text-center">12 / 6 / 4</div>
						</v-sheet>
					</v-col>
					<v-col cols="12" sm="12" md="4">
						<v-sheet color="warning" class="pa-4" rounded>
							<div class="text-center">12 / 12 / 4</div>
						</v-sheet>
					</v-col>
				</v-row>
			</v-card-text>
		</v-card>

		<!-- 间距系统 -->
		<v-card class="mb-6">
			<v-card-title>间距系统</v-card-title>
			<v-card-text>
				<div class="mb-4">
					<h4 class="mb-3">Padding</h4>
					<v-row>
						<v-col v-for="n in [0, 1, 2, 4, 6, 8]" :key="n" cols="6" sm="4" md="2">
							<v-sheet color="grey-lighten-3" :class="`pa-${n}`" rounded>
								<v-sheet color="primary" class="text-center pa-2" rounded> pa-{{ n }} </v-sheet>
							</v-sheet>
						</v-col>
					</v-row>
				</div>

				<div>
					<h4 class="mb-3">Margin</h4>
					<v-sheet color="grey-lighten-3" class="pa-4" rounded>
						<v-sheet color="primary" class="ma-2 pa-2" rounded>ma-2</v-sheet>
						<v-sheet color="secondary" class="ma-4 pa-2" rounded>ma-4</v-sheet>
						<v-sheet color="success" class="ma-6 pa-2" rounded>ma-6</v-sheet>
					</v-sheet>
				</div>
			</v-card-text>
		</v-card>

		<!-- 圆角 -->
		<v-card class="mb-6">
			<v-card-title>圆角</v-card-title>
			<v-card-text>
				<v-row>
					<v-col v-for="radius in roundedOptions" :key="radius.value" cols="6" sm="4" md="3">
						<v-sheet color="primary" :rounded="radius.value" class="pa-4 text-center">
							{{ radius.label }}
						</v-sheet>
					</v-col>
				</v-row>
			</v-card-text>
		</v-card>

		<!-- 阴影 -->
		<v-card>
			<v-card-title>阴影</v-card-title>
			<v-card-text>
				<v-row>
					<v-col v-for="n in [0, 1, 2, 4, 8, 12, 16, 24]" :key="n" cols="6" sm="4" md="3">
						<v-sheet color="surface" :elevation="n" class="pa-4 text-center" rounded>
							elevation-{{ n }}
						</v-sheet>
					</v-col>
				</v-row>
			</v-card-text>
		</v-card>
	</div>
</template>

<script setup>
const roundedOptions = [
	{ label: '0', value: '0' },
	{ label: 'sm', value: 'sm' },
	{ label: 'default', value: true },
	{ label: 'lg', value: 'lg' },
	{ label: 'xl', value: 'xl' },
	{ label: 'pill', value: 'pill' },
	{ label: 'circle', value: 'circle' },
]
</script>
