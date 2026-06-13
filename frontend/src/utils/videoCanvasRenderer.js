const VERTEX_SHADER_SOURCE = `#version 300 es
in vec2 a_position;
in vec2 a_texCoord;
out vec2 v_texCoord;

void main() {
	gl_Position = vec4(a_position, 0.0, 1.0);
	v_texCoord = a_texCoord;
}`

const FRAGMENT_SHADER_SOURCE = `#version 300 es
precision highp float;

in vec2 v_texCoord;
out vec4 outColor;
uniform sampler2D u_video;

void main() {
	outColor = texture(u_video, v_texCoord);
}`

export function createHiddenVideo(muted) {
	const video = document.createElement('video')
	video.id = 'effect-source-hidden'
	video.autoplay = true
	video.playsInline = true
	video.muted = muted
	video.style.position = 'absolute'
	video.style.top = '-9999px'
	video.style.left = '-9999px'
	video.style.width = '1px'
	video.style.height = '1px'
	video.style.opacity = '0'
	video.style.pointerEvents = 'none'
	document.body.appendChild(video)
	return video
}

function compileShader(gl, type, source) {
	const shader = gl.createShader(type)
	gl.shaderSource(shader, source)
	gl.compileShader(shader)

	if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
		const message = gl.getShaderInfoLog(shader)
		gl.deleteShader(shader)
		throw new Error(message || 'Failed to compile WebGL shader')
	}

	return shader
}

function createProgram(gl) {
	const vertexShader = compileShader(gl, gl.VERTEX_SHADER, VERTEX_SHADER_SOURCE)
	const fragmentShader = compileShader(gl, gl.FRAGMENT_SHADER, FRAGMENT_SHADER_SOURCE)
	const program = gl.createProgram()

	gl.attachShader(program, vertexShader)
	gl.attachShader(program, fragmentShader)
	gl.linkProgram(program)
	gl.deleteShader(vertexShader)
	gl.deleteShader(fragmentShader)

	if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
		const message = gl.getProgramInfoLog(program)
		gl.deleteProgram(program)
		throw new Error(message || 'Failed to link WebGL program')
	}

	return program
}

function setupWebGL(canvas) {
	const gl = canvas.getContext('webgl2', {
		alpha: false,
		desynchronized: true,
		preserveDrawingBuffer: false,
	})

	if (!gl) return null

	const program = createProgram(gl)
	gl.useProgram(program)

	const vertexBuffer = gl.createBuffer()
	gl.bindBuffer(gl.ARRAY_BUFFER, vertexBuffer)
	gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 0, 1, 1, -1, 1, 1, -1, 1, 0, 0, 1, 1, 1, 0]), gl.STATIC_DRAW)

	const positionLocation = gl.getAttribLocation(program, 'a_position')
	const texCoordLocation = gl.getAttribLocation(program, 'a_texCoord')
	gl.enableVertexAttribArray(positionLocation)
	gl.vertexAttribPointer(positionLocation, 2, gl.FLOAT, false, 16, 0)
	gl.enableVertexAttribArray(texCoordLocation)
	gl.vertexAttribPointer(texCoordLocation, 2, gl.FLOAT, false, 16, 8)

	const texture = gl.createTexture()
	gl.bindTexture(gl.TEXTURE_2D, texture)
	gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR)
	gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR)
	gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE)
	gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE)
	gl.uniform1i(gl.getUniformLocation(program, 'u_video'), 0)

	return {
		gl,
		program,
		vertexBuffer,
		texture,
	}
}

function setCanvasSourceSize(canvas, video) {
	const width = video.videoWidth || 640
	const height = video.videoHeight || 360

	if (canvas.width !== width) canvas.width = width
	if (canvas.height !== height) canvas.height = height
}

export function createVideoCanvasRenderer(canvas, options = {}) {
	let { muted = true } = options
	const sourceVideo = createHiddenVideo(muted)
	let stream = null
	let rafId = null
	let disposed = false
	let webgl = null
	let fallbackCtx = null

	try {
		webgl = setupWebGL(canvas)
	} catch (error) {
		console.warn('[VideoCanvasRenderer] WebGL2 init failed, falling back to 2D canvas:', error)
		webgl = null
	}

	if (!webgl) {
		fallbackCtx = canvas.getContext('2d', { alpha: false, willReadFrequently: false })
	}

	function renderFrame() {
		if (disposed) return

		if (sourceVideo.readyState >= HTMLMediaElement.HAVE_CURRENT_DATA && sourceVideo.videoWidth > 0 && sourceVideo.videoHeight > 0) {
			setCanvasSourceSize(canvas, sourceVideo)

			if (webgl) {
				const { gl, texture } = webgl
				gl.viewport(0, 0, canvas.width, canvas.height)
				gl.bindTexture(gl.TEXTURE_2D, texture)
				gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, sourceVideo)
				gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4)
			} else if (fallbackCtx) {
				fallbackCtx.drawImage(sourceVideo, 0, 0, canvas.width, canvas.height)
			}
		}

		rafId = requestAnimationFrame(renderFrame)
	}

	function ensureLoop() {
		if (!rafId && !disposed) {
			rafId = requestAnimationFrame(renderFrame)
		}
	}

	function clearStream() {
		sourceVideo.pause()
		sourceVideo.srcObject = null
		stream = null
	}

	return {
		canvas,
		get sourceVideo() {
			return sourceVideo
		},
		setMuted(nextMuted) {
			muted = Boolean(nextMuted)
			sourceVideo.muted = muted
		},
		setStream(nextStream) {
			if (disposed || stream === nextStream) return

			if (!nextStream || !nextStream.getVideoTracks().some(track => track.readyState === 'live')) {
				clearStream()
				return
			}

			stream = nextStream
			sourceVideo.srcObject = nextStream
			sourceVideo.play().catch(error => {
				if (error.name !== 'AbortError') {
					console.warn('[VideoCanvasRenderer] Source video play failed:', error)
				}
			})
			ensureLoop()
		},
		dispose() {
			disposed = true
			if (rafId) {
				cancelAnimationFrame(rafId)
				rafId = null
			}
			clearStream()
			if (sourceVideo.parentNode) {
				sourceVideo.parentNode.removeChild(sourceVideo)
			}
			if (webgl?.gl) {
				const { gl, program, vertexBuffer, texture } = webgl
				gl.deleteTexture(texture)
				gl.deleteBuffer(vertexBuffer)
				gl.deleteProgram(program)
			}
			webgl = null
			fallbackCtx = null
		},
	}
}
