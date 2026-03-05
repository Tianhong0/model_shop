import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

const sassWarnFilter = {
  warn(message, options) {
    const text = String(message || '')
    const stack = String(options?.stack || '')
    const isImportDeprecation = text.includes('Sass @import rules are deprecated')
    const fromUview = stack.includes('node_modules/uview-plus') || stack.includes('node_modules\\uview-plus')

    if (isImportDeprecation && fromUview) {
      return
    }

    console.warn(message)
  }
}

export default defineConfig({
  plugins: [
    uni()
  ],
  css: {
    preprocessorOptions: {
      scss: {
        quietDeps: true,
        silenceDeprecations: ['legacy-js-api', 'import'],
        logger: sassWarnFilter
      }
    }
  },
  server: {
    port: 5174,
    host: true
  }
})
