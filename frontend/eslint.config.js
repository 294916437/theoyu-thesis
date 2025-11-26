import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import globals from 'globals'
import vueParser from 'vue-eslint-parser'
import prettierConfig from 'eslint-config-prettier'

export default [
	{
		ignores: [
			'**/dist/**',
			'**/node_modules/**',
			'**/.vscode/**',
			'**/.idea/**',
			'**/coverage/**',
			'**/*.min.js',
			'pnpm-lock.yaml',
		],
	},

	// JavaScript 推荐配置
	js.configs.recommended,

	// Vue 3 推荐配置
	...pluginVue.configs['flat/recommended'],

	// 全局配置
	{
		languageOptions: {
			ecmaVersion: 'latest',
			sourceType: 'module',
			globals: {
				...globals.browser,
				...globals.node,
				...globals.es2020,
			},
			parser: vueParser,
			parserOptions: {
				ecmaVersion: 'latest',
				sourceType: 'module',
				ecmaFeatures: {
					jsx: true,
				},
			},
		},

		rules: {
			// JavaScript 规则
			'prefer-const': [
				'error',
				{
					destructuring: 'any',
					ignoreReadBeforeAssign: false,
				},
			],
			'no-unused-vars': [
				'warn',
				{
					argsIgnorePattern: '^_',
					varsIgnorePattern: '^_',
				},
			],
			'no-var': 'error',
			'vue/custom-event-name-casing': [
				'error',
				'kebab-case',
				{
					ignores: [],
				},
			],

			// Vue 规则
			'vue/multi-word-component-names': 'off',
			'vue/no-v-html': 'warn',
			'vue/require-default-prop': 'off',
			'vue/no-unused-vars': 'error',
			'vue/component-name-in-template-casing': ['error', 'PascalCase'],
			'vue/html-self-closing': [
				'error',
				{
					html: {
						void: 'always',
						normal: 'always',
						component: 'always',
					},
				},
			],
			'vue/max-attributes-per-line': [
				'warn',
				{
					singleline: 3,
					multiline: 1,
				},
			],
			'vue/singleline-html-element-content-newline': 'off',
			'vue/multiline-html-element-content-newline': 'off',
			// ========== 命名规范 ==========
			'vue/component-definition-name-casing': ['error', 'PascalCase'],
			'vue/prop-name-casing': ['error', 'camelCase'],
		},
	},

	// Prettier 配置（必须放在最后）
	prettierConfig,
]
