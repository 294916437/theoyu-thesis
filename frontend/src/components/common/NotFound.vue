<template>
	<v-container class="not-found-page">
		<v-row align="center" justify="center" style="min-height: 80vh">
			<v-col cols="12" md="6" class="text-center">
				<v-icon size="200" color="grey-lighten-2" class="mb-8"> mdi-cloud-question </v-icon>

				<h1 class="text-h1 font-weight-bold mb-4 gradient-text">404</h1>

				<h2 class="text-h4 mb-4">页面未找到</h2>

				<p class="text-body-1 text-grey mb-8">抱歉,您访问的页面不存在或已被移除</p>

				<v-btn color="primary" size="large" prepend-icon="mdi-home" @click="goHome"> 返回首页 </v-btn>
			</v-col>
		</v-row>
	</v-container>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
	router.push('/')
}
</script>

<style scoped>
.not-found-page {
	min-height: 100vh;
	display: flex;
	align-items: center;
	justify-content: center;
}

.gradient-text {
	background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	-webkit-background-clip: text;
	-webkit-text-fill-color: transparent;
	background-clip: text;
}
</style>
