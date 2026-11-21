# Клиент

React и Vite. Node и npm нужны только на Mac для разработки и сборки.

Локальная разработка:

```bash
npm ci
npm run dev
```

Открыть `http://localhost:5173`. Vite проксирует запросы к двум HTTPS API.
Адреса и сертификаты прокси задаются в `vite.config.js` или через
`ORG_API_URL`, `PEOPLE_API_URL`, `ORG_CA_CERT`, `PEOPLE_CA_CERT`.

Сборка статического WAR для Helios, из папки `client` на Mac:

```bash
VITE_ORG_API_URL='' VITE_PEOPLE_API_URL='https://localhost:38443/orgmanager' npm run build -- --base=/client/
mkdir -p target
jar --create --file target/client.war -C dist .
```

`target/client.war` развёртывается в Payara с контекстом `/client`.
Node на Helios не требуется. При открытии через SSH-туннель адрес клиента:
`https://localhost:22081/client/`. Браузер должен доверять сертификатам
Payara и WildFly. В сервисе людей разрешён CORS для этого адреса;
другой origin задаётся параметром JVM `-Dclient.origin=...`.

В `src`: `api` — запросы и преобразование XML; `organizations` — список и формы
организаций, включая объединение; `people` — список и формы сотрудников.
`App.jsx` содержит вкладки, состояние загрузки и сообщения.
