// healthassistantfrontend/vite.config.js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { resolve } from 'path'

export default defineConfig({
    plugins: [
        vue(),
        AutoImport({
            resolvers: [ElementPlusResolver()],
        }),
        Components({
            resolvers: [ElementPlusResolver()],
        }),
    ],
    resolve: {
        alias: {
            '@': resolve(__dirname, './src')
        }
    },
    server: {
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                secure: false,
                rewrite: (path) => path,
                // SSE 长连接：禁用代理超时并避免缓冲，否则 AI 流式会长时间无输出
                timeout: 0,
                proxyTimeout: 0,
                configure: (proxy) => {
                    proxy.on('proxyReq', (proxyReq, req) => {
                        if (req.headers.accept?.includes('text/event-stream')) {
                            proxyReq.setHeader('Connection', 'keep-alive');
                            proxyReq.setHeader('Cache-Control', 'no-cache');
                        }
                    });
                    proxy.on('proxyRes', (proxyRes, req, res) => {
                        if (req.headers.accept?.includes('text/event-stream')) {
                            res.setHeader('Cache-Control', 'no-cache');
                            res.setHeader('Connection', 'keep-alive');
                            proxyRes.headers['x-accel-buffering'] = 'no';
                        }
                    });
                },
            },
            // 食谱配图保存在后端 uploads，开发时需代理否则图片 404
            '/uploads': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                secure: false,
            },
        }
    },
    build: {
        outDir: 'dist',
        assetsDir: 'assets',
        sourcemap: false,
        minify: true,
    }
})
