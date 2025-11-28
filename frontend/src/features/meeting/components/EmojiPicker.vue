<template>
	<v-card max-width="320" max-height="400" @click.stop>
		<v-tabs v-model="activeCategory" density="compact">
			<v-tab v-for="category in categories" :key="category.name" :value="category.name">
				{{ category.icon }}
			</v-tab>
		</v-tabs>

		<v-card-text class="emoji-grid">
			<div v-for="emoji in filteredEmojis" :key="emoji" class="emoji-item" @click="selectEmoji(emoji)">
				{{ emoji }}
			</div>
		</v-card-text>
	</v-card>
</template>

<script>
const categories = [
	{ name: 'smileys', icon: '😊' },
	{ name: 'gestures', icon: '👋' },
	{ name: 'objects', icon: '🎉' },
]

// Object.freeze 明确告诉 Vue 不需要对这些数据做响应式监听
const emojis = Object.freeze({
	smileys: [
		'😀',
		'😃',
		'😄',
		'😁',
		'😅',
		'😂',
		'🤣',
		'😊',
		'😇',
		'🙂',
		'🙃',
		'😉',
		'😌',
		'😍',
		'🥰',
		'😘',
		'😗',
		'😙',
		'😚',
		'😋',
		'😛',
		'😝',
		'😜',
		'🤪',
		'🤨',
		'🧐',
		'🤓',
		'😎',
	],
	gestures: [
		'👋',
		'🤚',
		'🖐',
		'✋',
		'🖖',
		'👌',
		'🤌',
		'🤏',
		'✌️',
		'🤞',
		'🤟',
		'🤘',
		'🤙',
		'👈',
		'👉',
		'👆',
		'🖕',
		'👇',
		'☝️',
		'👍',
		'👎',
		'✊',
		'👊',
		'🤛',
		'🤜',
		'👏',
		'🙌',
	],
	objects: [
		'🎉',
		'🎊',
		'🎈',
		'🎁',
		'🏆',
		'🥇',
		'🥈',
		'🥉',
		'⭐',
		'🌟',
		'💫',
		'✨',
		'🔥',
		'💥',
		'💢',
		'💯',
		'💪',
		'🦾',
		'🦿',
		'🦵',
		'🦶',
		'👂',
		'🦻',
		'👃',
		'🧠',
		'🦷',
		'🦴',
	],
})
</script>

<script setup>
import { ref, computed } from 'vue'

const emit = defineEmits(['select'])

const activeCategory = ref('smileys')

const filteredEmojis = computed(() => {
	return emojis[activeCategory.value] || []
})

const selectEmoji = emoji => {
	emit('select', emoji)
}
</script>

<style scoped>
.emoji-grid {
	display: grid;
	grid-template-columns: repeat(8, 1fr);
	gap: 4px;
	max-height: 300px;
	overflow-y: auto;
	padding: 8px;
}

.emoji-item {
	font-size: 24px;
	cursor: pointer;
	text-align: center;
	padding: 4px;
	border-radius: 4px;
	transition: background-color 0.2s;
}

.emoji-item:hover {
	background-color: rgba(0, 0, 0, 0.05);
}
</style>
