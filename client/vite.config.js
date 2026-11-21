import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import { readFileSync } from 'node:fs';
import https from 'node:https';
import { fileURLToPath } from 'node:url';

export default defineConfig(({ mode, command }) => {
    const root = fileURLToPath(new URL('.', import.meta.url));
    const env = { ...loadEnv(mode, root, ''), ...process.env };
    function proxy(target, certificate, prefix) {
        if (!target.startsWith('https://')) throw new Error('API должен работать по HTTPS');
        return {
            target,
            changeOrigin: true,
            secure: true,
            agent: new https.Agent({ ca: readFileSync(certificate) }),
            rewrite: (path) => path.replace(prefix, ''),
            configure(server) {
                server.on('error', (_error, _request, response) => {
                    if (
                        response &&
                        typeof response.writeHead === 'function' &&
                        !response.headersSent
                    ) {
                        response.writeHead(503, {
                            'Content-Type': 'application/xml; charset=UTF-8',
                        });
                        response.end(
                            '<error><message>Не удалось подключиться к сервису. Проверьте, запущен ли сервер и верен ли сертификат.</message></error>',
                        );
                    }
                });
            },
        };
    }

    const proxies =
        command === 'serve'
            ? {
                  '/api/org': proxy(
                      env.ORG_API_URL || 'https://localhost:18081',
                      env.ORG_CA_CERT ||
                          '/Applications/Payara6/glassfish/domains/organizations-lab/config/organizations-cert.pem',
                      '/api/org',
                  ),
                  '/api/people': proxy(
                      env.PEOPLE_API_URL || 'https://localhost:18443/orgmanager',
                      env.PEOPLE_CA_CERT ||
                          '/Applications/WildFly35/standalone/configuration/people-cert.pem',
                      '/api/people',
                  ),
              }
            : {};
    return {
        root,
        plugins: [react()],
        server: { port: 5173, strictPort: true, proxy: proxies },
        preview: { port: 5173, strictPort: true, proxy: proxies },
    };
});
