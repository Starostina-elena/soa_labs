# Сервис сотрудников

Java 17, Spring MVC, WildFly 35, Hibernate 6.6, PostgreSQL. API: `../docs/swagger/people-openapi.yaml`.

Сборка: `mvn package`. Результат: `target/orgmanager.war`, контекст `/orgmanager`.
Подключение к БД: `DB_URL`, `DB_USER`, `PGPASSFILE`, как в сервисе организаций.
Таблицы создаются файлом `../db_init.sql`.

Параметры JVM для обращения к первому сервису:

```text
-Dorganizations.url=https://localhost:18081
-Dorganizations.caCert=/path/to/organizations-cert.pem
```

Объединение: проверить организации, создать новую, перенести сотрудников, удалить исходные.
Перенос связей выполняется одной транзакцией. При ошибке часть шагов может быть уже выполнена.
HTTPS и сертификат настраиваются в WildFly; HTTP-listener должен быть отключён.
На Helios все новые слушающие порты должны быть от 16384.

Пакеты: `config` — настройка Spring; `api` — REST, проверка запросов и ошибки;
`model` — структуры данных и сущности; `service` — операции с сотрудниками и объединение;
`repository` — операции Hibernate; `client` — HTTPS-запросы к сервису организаций.

`config/HibernateConfig.java` задаёт подключение и режим `validate`: Hibernate проверяет
таблицы, но не создаёт и не меняет их. Пароль читает PostgreSQL-драйвер из `.pgpass`.

База данных — `studs`, схема таблиц — `s414257` (выбрана через `currentSchema` в JDBC URL).
