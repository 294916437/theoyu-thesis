import { createRouter, createWebHistory } from 'vue-router'

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
	// TODO: 添加路由守卫
})

export default router
