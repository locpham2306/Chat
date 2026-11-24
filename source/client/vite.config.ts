import babel from '@rolldown/plugin-babel'
import tailwindcss from '@tailwindcss/vite'
import react, { reactCompilerPreset } from '@vitejs/plugin-react'
import path from 'path'
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  const apiTarget = env.VITE_API_URL || 'http://localhost:8086'
  return {
    plugins: [tailwindcss(), react(), babel({ presets: [reactCompilerPreset()] })],
    server: {
      port: 3001,
      host: '0.0.0.0',
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true
        },
        '/ws': {
          target: apiTarget,
          changeOrigin: true,
          ws: true
        }
      }
    },
    preview: { port: 3001 },
    css: { devSourcemap: true },
    resolve: {
      alias: {
        '~': path.resolve(import.meta.dirname, './src')
      }
    }
  }
})
