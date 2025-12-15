import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
	history: createWebHistory(),
	routes: [
		{
			path: '/',
			name: 'Home',
			component: () => import('@/features/home/views/HomePage.vue'),
			meta: {
				title: '首页',
			},
		},
		{
			path: '/user/login',
			name: 'Login',
			component: () => import('@/features/user/views/LoginPage.vue'),
			meta: {
				title: '用户登陆页面',
			},
		},
		{
			path: '/user/profile',
			name: 'Profile',
			component: () => import('@/features/user/views/ProfilePage.vue'),
			meta: {
				title: '用户资料',
				requiresAuth: true,
			},
		},
		{
			path: '/meeting/:id',
			name: 'MeetingRoom',
			component: () => import('@/features/meeting/views/MeetingRoom.vue'),
			meta: {
				title: '会议室',
				requiresAuth: true,
			},
		},
		{
			path: '/meeting/detail/:id',
			name: 'MeetingDetail',
			component: () => import('@/features/meeting/views/MeetingDetail.vue'),
			meta: {
				title: '会议详情',
				requiresAuth: true,
			},
		},
		{
			path: '/:pathMatch(.*)*',
			name: 'NotFound',
			component: () => import('@/components/common/NotFound.vue'),
		},
		{
			path: '/chat',
			name: 'ChatCenter',
			component: () => import('@/features/chat/views/Chat.vue'),
			meta: {
				title: '私聊中心',
				requiresAuth: true,
			},
		},
		{
			path: '/theme-test',
			name: 'ThemeTest',
			component: () => import('@/views/ThemeTestPage.vue'),
			meta: {
				title: '主题测试',
			},
		},
	],

	scrollBehavior(to, from, savedPosition) {
		if (savedPosition) {
			return savedPosition
		} else {
			return { top: 0 }
		}
	},
})
router.beforeEach((to, from, next) => {
	const userStore = useUserStore()

	// 设置页面标题
	if (to.meta.title) {
		document.title = to.meta.title
	}

	// 检查是否需要认证
	if (to.meta.requiresAuth) {
		// 未登录，重定向到登录页
		if (!userStore.token) {
			next({
				name: 'Login',
				query: {
					redirect: to.fullPath,
				},
			})
			return
		}
	}

	// 已登录用户访问登录页，重定向到首页
	if (to.name === 'Login' && userStore.token) {
		next({ name: 'Home' })
		return
	}

	// 正常放行
	next()
})

export default router
