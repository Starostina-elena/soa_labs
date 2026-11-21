# Сервис организаций

Java 17, JAX-RS, Payara 6, Hibernate 6.6, PostgreSQL. API: `../docs/swagger/organizations-openapi.yaml`.

Сборка: `mvn package`. Результат: `target/organizations.war`.

Перед запуском сервера задать переменные окружения:

```bash
export DB_URL='jdbc:postgresql://pg:5432/studs?currentSchema=s414257'
export DB_USER=s414257
export PGPASSFILE="$HOME/.pgpass"
```

Таблицы создаются файлом `../db_init.sql`. Пароль JDBC-драйвер читает из `~/.pgpass` (или файла `PGPASSFILE`). В исходники он не записывается.
HTTPS и сертификат настраиваются в Payara; HTTP-listener должен быть отключён.
На Helios все новые слушающие порты должны быть от 16384.

Пакеты: `config` — запуск JAX-RS; `api` — REST, проверка запросов и ошибки;
`model` — структуры данных и сущности; `repository` — операции Hibernate и поиск через Criteria API.

`config/HibernateConfig.java` задаёт подключение и режим `validate`: Hibernate проверяет
таблицы, но не создаёт и не меняет их. Пароль читает PostgreSQL-драйвер из `.pgpass`.

База данных — `studs`, схема таблиц — `s414257` (выбрана через `currentSchema` в JDBC URL).
