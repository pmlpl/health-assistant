// healthassistantfrontend/vite.config.js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vite.dev/config/
export default defineConfig({
    plugins: [vue()],
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
                // 重写路径，保持/api 前缀
                rewrite: (path) => path,
                // CORS 配置
                configure: (proxy, _options) => {
                    proxy.on('error', (err, _req, _res) => {
                        console.log('代理错误:', err);
                    });
                    proxy.on('proxyReq', (proxyReq, req, _res) => {
                        console.log('发送请求到:', req.method, req.url);
                    });
                    proxy.on('proxyRes', (proxyRes, req, _res) => {
                        console.log('收到响应:', req.method, req.url, proxyRes.statusCode);
                    });
                }
            }
        }
    },
    build: {
        // 生产环境构建配置
        outDir: 'dist',
        assetsDir: 'assets',
        sourcemap: false,
        // 使用默认压缩方式
        minify: false
    }
})
