import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig(env => {
    const conf =  {
        plugins: [react()]
    }
    if (env.mode == 'dev') return {
        ...conf,
        build: {
            minify: false,
            sourcemap: 'inline'
        },
    }
    else return conf
})
