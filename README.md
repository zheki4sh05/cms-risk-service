# cms-risk-service

## Взаимодействие с другими сервисами

Интеграции строятся на **HTTP** (Spring `RestClient`), **Kafka** и **общей БД** (PostgreSQL через JPA; отдельный сервис БД в инфраструктуре, не «REST-сервис»). Ниже — вызовы между микросервисами и открытые контракты этого сервиса.

### Исходящие HTTP-вызовы (клиент)

Все базовые URL задаются в `application.yaml` и переопределяются переменными окружения.

| Целевой сервис | Переменная / URL по умолчанию | Вызов | Назначение |
|----------------|------------------------------|--------|------------|
| **Auth** | `AUTH_SERVICE_BASE_URL` (`http://localhost:9091`) | `GET /api/users/{id}` с заголовком `Authorization: Bearer <token>` | Получение ФИО / имени пользователя для истории изменений правил (`AuthServiceUserInfoAdapter` → `UserInfoRepository`). При ошибке подставляется `"Unknown user"`. |
| **Auth** | то же | `GET /api/users/{id}/permissions/check?permission=MANAGE_RULES_AND_RISKS` с `Authorization: Bearer <token>` | Проверка права на управление правилами и рисками. Ответ считается допуском, если поле `access` равно `permit` (без учёта регистра). Используется при создании/изменении правил, категорий рисков и привязке объекта риска к правилу (`AuthServicePermissionAdapter` → `PermissionCheckPort`). |
| **Monitoring** | `MONITORING_SERVICE_BASE_URL` (`http://localhost:9093`) | `PUT /api/monitoring-results/{id}/take` с `Accept: application/json` | Планировщик `OutbooxMonitoringProcessingScheduler` по очереди забирает сущность мониторинга по `id` из поля `monitoring_entity` в записи `outboox_monitoring`. Тело ответа маппится в `MonitoringTakePayload` и передаётся в `RuleEvaluationEngine`. Запрос **без** передачи JWT пользователя (серверный вызов от имени самого `cms-risk-service`). |
| **Risk object** | `RISK_OBJECT_SERVICE_BASE_URL` (`http://localhost:9093`) | `GET /api/internal/risk-objects/{id}` с `Authorization: Bearer <token>` и заголовком `CompanyId: <uuid>` | Обогащение списка правил данными объекта риска (`RiskObjectDetailsAdapter` → `RiskObjectDetailsRepository`). При ошибке HTTP или сети соответствующий элемент в ответе API может иметь `riskObject: null`, сам запрос списка правил при этом не падает. |

Реализация клиентов: `HttpClientsConfig` (бины `RestClient`), адаптеры в пакетах `infrastructure.auth`, `infrastructure.monitoring`, `infrastructure.riskobject`.

### Входящие запросы (этот сервис как API)

- **Публичные REST API** под защитой JWT: токен проверяется локально (Spring Security OAuth2 Resource Server, публичный ключ в `app.security.jwt.public-key`). Контекст пользователя (`UserContext`) заполняется из JWT и при необходимости заголовков компании. Часть операций дополнительно обращается к **Auth** за проверкой разрешений (см. таблицу выше).
- **Внутренние эндпоинты (только с localhost)** — для интеграций на той же машине:
  - `GET /api/internal/rules/{id}` — детали правила по идентификатору (см. `InternalRuleController`).
  - `GET /api/internal/risk-categories` — список категорий рисков компании; обязателен заголовок `CompanyId` (или устаревший `companyId`) с UUID (`InternalRiskCategoryController`).

Подробные пути публичного API см. в Swagger (`/swagger-ui.html`, `/v3/api-docs`).

### Kafka и асинхронный поток

- **Вход:** consumer читает топик риск-событий (`app.kafka.topics.risk-topic` / `KAFKA_RISK_TOPIC`), сохраняет JSON в таблицу `outboox_monitoring` (`RiskTopicListener`).
- **Обработка:** планировщик забирает данные из БД, вызывает **Monitoring** (`take`), затем правила и при необходимости пишет инцидент в Kafka.
- **Выход:** producer публикует JSON в топик инцидентов (подробности ниже в разделе **Kafka: исходящие сообщения (producer)**).

## Kafka: исходящие сообщения (producer)

Сервис **публикует** сообщения только в **один** топик — топик инцидентов. Краткое описание consumer (входной топик риска) дано в разделе «Взаимодействие с другими сервисами» выше.

### Топик

| Свойство | Значение по умолчанию | Переменная окружения | Spring property |
|----------|------------------------|----------------------|-----------------|
| Имя топика | `incident_topic` | `KAFKA_INCIDENT_TOPIC` | `app.kafka.topics.incident-topic` |

Конфигурация задаётся в `application.yaml` (см. `app.kafka.topics`).

### Когда отправляется сообщение

Сообщение публикуется после обработки данных мониторинга движком правил (`RuleEvaluationEngine`), **если хотя бы для одного правила** в результате выполнения скрипта установлено `found == true` (обнаружено несоответствие / триггер правила). Если расхождений нет, событие в Kafka не отправляется — результат сохраняется только в БД как успешная верификация.

### Формат сообщения

- **Ключ сообщения (Kafka key):** не задаётся (`null`). Используется вызов `KafkaTemplate.send(topic, value)` только с телом сообщения.
- **Значение (value):** строка **JSON** в кодировке UTF-8 (`String`), сериализация через Jackson (`ObjectMapper`).

Корневой объект и вложенные элементы соответствуют структуре `CreateIncidentMessage` и `RuleResultMessage` в `IncidentEventPublisher`.

#### Поля корневого объекта (JSON)

| Поле JSON | Тип | Описание |
|-----------|-----|----------|
| `companyId` | string (UUID) | Идентификатор компании (из первого найденного правила по объекту риска). |
| `integrationId` | number | Идентификатор интеграции из входного payload мониторинга. |
| `riskObjectId` | string | Идентификатор объекта риска (как в мониторинге, не обязательно чистый UUID). |
| `documentId` | string или `null` | Идентификатор документа, извлечённый по mapping rules из данных (`to` = `id`). |
| `rules` | array | Список результатов **по всем** обработанным правилам для данного события (включая правила без срабатывания или с ошибкой скрипта). |

#### Элемент массива `rules`

| Поле JSON | Тип | Описание |
|-----------|-----|----------|
| `rulesId` | string (UUID) | Идентификатор правила. |
| `rulePriority` | string | Приоритет правила из сущности правила. |
| `responsible_user_id` | string (UUID) или `null` | Ответственный пользователь (имя поля в JSON — **snake_case**, см. `@JsonProperty`). |
| `result` | string | Итог выполнения: в частности `"success"` или `"failed"` (в т.ч. при падении скрипта). |
| `found` | boolean | Признак срабатывания правила (несоответствие найдено). |
| `details` | object | Детали от скрипта или при ошибке — объект с полями вроде `error`, `reason` и др. (произвольная структура `Map<String, Object>`). |
| `detectedAt` | string | Метка времени в формате строки `Instant.now().toString()` (ISO-8601 с часовым поясом системы JVM). |

Пример тела сообщения (значения UUID и тексты условные):

```json
{
  "companyId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "integrationId": 42,
  "riskObjectId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "documentId": "doc-12345",
  "rules": [
    {
      "rulesId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "rulePriority": "HIGH",
      "responsible_user_id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
      "result": "success",
      "found": true,
      "details": {
        "amount": 1000
      },
      "detectedAt": "2026-05-09T12:00:00.123456789Z"
    }
  ]
}
```

### См. в коде

- Публикация: `com.trustflow.cms_risk_service.infrastructure.kafka.IncidentEventPublisher`
- Имена топиков: `com.trustflow.cms_risk_service.infrastructure.kafka.KafkaTopicProperties`
- Условие отправки и наполнение `rules`: `com.trustflow.cms_risk_service.infrastructure.monitoring.RuleEvaluationEngine`
