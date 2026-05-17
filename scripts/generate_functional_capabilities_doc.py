"""Generate Word document with functional capabilities and role access for cms-risk-service."""

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.shared import Cm, Pt
from docx.oxml.ns import qn
from docx.oxml import OxmlElement


def set_cell_shading(cell, fill_hex: str) -> None:
    shading = OxmlElement("w:shd")
    shading.set(qn("w:fill"), fill_hex)
    cell._tc.get_or_add_tcPr().append(shading)


def add_table_row(table, cells_data, header: bool = False) -> None:
    row = table.add_row()
    for idx, text in enumerate(cells_data):
        cell = row.cells[idx]
        cell.text = text
        for paragraph in cell.paragraphs:
            for run in paragraph.runs:
                run.font.size = Pt(10)
                run.font.name = "Calibri"
            if header:
                for run in paragraph.runs:
                    run.bold = True
        if header:
            set_cell_shading(cell, "D9E2F3")


def main() -> None:
    doc = Document()

    section = doc.sections[0]
    section.top_margin = Cm(2)
    section.bottom_margin = Cm(2)
    section.left_margin = Cm(2.5)
    section.right_margin = Cm(2)

    title = doc.add_heading("Перечень функциональных возможностей", level=0)
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER

    subtitle = doc.add_paragraph("Сервис: cms-risk-service (TrustFlow CMS)")
    subtitle.alignment = WD_ALIGN_PARAGRAPH.CENTER
    subtitle.runs[0].font.size = Pt(12)

    doc.add_paragraph(
        "Документ описывает функции, реализованные в микросервисе управления правилами "
        "и рисками, и роли пользователей, которым они доступны. Авторизация REST API "
        "выполняется по JWT; детальная проверка прав — через сервис compliance-auth-service."
    )

    doc.add_heading("Роли пользователей системы", level=1)
    roles_table = doc.add_table(rows=1, cols=3)
    roles_table.style = "Table Grid"
    hdr = roles_table.rows[0].cells
    hdr[0].text = "Код роли"
    hdr[1].text = "Наименование"
    hdr[2].text = "Примечание"
    for cell in hdr:
        set_cell_shading(cell, "D9E2F3")

    role_rows = [
        ("EXECUTIVE", "Руководитель (исполнительный)", "При регистрации получает все права доступа, включая MANAGE_RULES_AND_RISKS"),
        ("SUPERVISOR", "Руководитель подразделения", "Права назначаются администратором вручную"),
        ("MANAGER", "Менеджер", "Права назначаются администратором вручную"),
        ("DEFAULT", "Пользователь по умолчанию", "Права назначаются администратором вручную"),
    ]
    for row_data in role_rows:
        add_table_row(roles_table, row_data)

    doc.add_paragraph(
        "Уровни доступа к API: (1) любой аутентифицированный пользователь с валидным JWT "
        "и контекстом компании (заголовок CompanyId или claim companyId); "
        "(2) пользователь с правом MANAGE_RULES_AND_RISKS в auth-сервисе; "
        "(3) внутренние интеграции с localhost без роли пользователя."
    )

    doc.add_heading("Функциональные возможности", level=1)

    cap_table = doc.add_table(rows=1, cols=4)
    cap_table.style = "Table Grid"
    h = cap_table.rows[0].cells
    h[0].text = "№"
    h[1].text = "Функциональная возможность"
    h[2].text = "Описание / API"
    h[3].text = "Роли и условия доступа"
    for cell in h:
        set_cell_shading(cell, "D9E2F3")

    capabilities = [
        (
            "1",
            "Просмотр списка правил риска",
            "GET /api/rules — список правил компании с категориями и объектами риска",
            "EXECUTIVE, SUPERVISOR, MANAGER, DEFAULT — все аутентифицированные пользователи компании",
        ),
        (
            "2",
            "Просмотр деталей правила",
            "GET /api/rules/{id}, GET /api/rules/short/{id}",
            "Все аутентифицированные пользователи компании",
        ),
        (
            "3",
            "Создание правила риска",
            "POST /api/rules",
            "Пользователи с правом MANAGE_RULES_AND_RISKS (по умолчанию — EXECUTIVE; для остальных ролей — при назначении администратором)",
        ),
        (
            "4",
            "Изменение правила риска",
            "PUT /api/rules/{id}",
            "MANAGE_RULES_AND_RISKS (EXECUTIVE или роли с назначенным правом)",
        ),
        (
            "5",
            "Изменение привязки объекта риска к правилу",
            "PUT /api/rules/{id}/risk-object",
            "MANAGE_RULES_AND_RISKS",
        ),
        (
            "6",
            "Просмотр истории изменений правил",
            "GET /api/rules/change-history, GET /api/rules/change-history/{id}",
            "Все аутентифицированные пользователи компании",
        ),
        (
            "7",
            "Просмотр списка категорий рисков",
            "GET /api/risk-categories",
            "Все аутентифицированные пользователи компании",
        ),
        (
            "8",
            "Создание категории риска",
            "POST /api/risk-categories",
            "MANAGE_RULES_AND_RISKS",
        ),
        (
            "9",
            "Изменение категории риска",
            "PUT /api/risk-categories/{id}",
            "MANAGE_RULES_AND_RISKS",
        ),
        (
            "10",
            "Удаление категории риска",
            "DELETE /api/risk-categories/{id}",
            "MANAGE_RULES_AND_RISKS",
        ),
        (
            "11",
            "Просмотр статистики обработки",
            "GET /api/rules/processing/statistic, GET /api/risks/processing/statistic — очередь outbox и число результатов верификации",
            "Все аутентифицированные пользователи компании",
        ),
        (
            "12",
            "Просмотр результатов верификации",
            "GET /api/verification-results, GET /api/verification-results/{id}",
            "Все аутентифицированные пользователи компании (данные в рамках companyId из JWT)",
        ),
        (
            "13",
            "Создание результата верификации",
            "POST /api/verification-results",
            "Все аутентифицированные пользователи компании",
        ),
        (
            "14",
            "Изменение результата верификации",
            "PUT /api/verification-results/{id}",
            "Все аутентифицированные пользователи компании",
        ),
        (
            "15",
            "Удаление результата верификации",
            "DELETE /api/verification-results/{id}",
            "Все аутентифицированные пользователи компании",
        ),
        (
            "16",
            "Внутренний просмотр правила (интеграция)",
            "GET /api/internal/rules/{id} — только с localhost",
            "Системная интеграция (без роли пользователя)",
        ),
        (
            "17",
            "Внутренний просмотр категорий рисков (интеграция)",
            "GET /api/internal/risk-categories — только с localhost, заголовок CompanyId",
            "Системная интеграция (без роли пользователя)",
        ),
        (
            "18",
            "Приём событий риска из Kafka",
            "Consumer топика risk-topic → сохранение в outboox_monitoring",
            "Системный процесс сервиса (не пользователь)",
        ),
        (
            "19",
            "Асинхронная обработка мониторинга и оценка правил",
            "Планировщик: Monitoring take → RuleEvaluationEngine → сохранение / публикация инцидента",
            "Системный процесс сервиса (не пользователь)",
        ),
        (
            "20",
            "Публикация инцидентов в Kafka",
            "Топик incident_topic при срабатывании правил (found=true)",
            "Системный процесс сервиса (не пользователь)",
        ),
        (
            "21",
            "Очистка истории изменений правил",
            "Планировщик RuleHistoryCleanupScheduler по расписанию",
            "Системный процесс сервиса (не пользователь)",
        ),
        (
            "22",
            "Документация API (Swagger)",
            "GET /swagger-ui.html, /v3/api-docs",
            "Без аутентификации (только для среды разработки/документирования)",
        ),
    ]

    for row in capabilities:
        add_table_row(cap_table, row)

    doc.add_heading("Сводная матрица доступа по ролям", level=1)

    matrix = doc.add_table(rows=1, cols=6)
    matrix.style = "Table Grid"
    mh = matrix.rows[0].cells
    mh[0].text = "Группа функций"
    mh[1].text = "EXECUTIVE"
    mh[2].text = "SUPERVISOR"
    mh[3].text = "MANAGER"
    mh[4].text = "DEFAULT"
    mh[5].text = "Условие"
    for cell in mh:
        set_cell_shading(cell, "D9E2F3")

    matrix_rows = [
        (
            "Чтение правил, истории, категорий, статистики, верификации",
            "Да",
            "Да*",
            "Да*",
            "Да*",
            "* при наличии JWT и контекста компании",
        ),
        (
            "Создание/изменение/удаление правил и категорий",
            "Да",
            "По назначению",
            "По назначению",
            "По назначению",
            "Требуется право MANAGE_RULES_AND_RISKS в auth",
        ),
        (
            "CRUD результатов верификации",
            "Да",
            "Да*",
            "Да*",
            "Да*",
            "* любой аутентифицированный пользователь компании",
        ),
        (
            "Фоновая обработка Kafka и планировщики",
            "—",
            "—",
            "—",
            "—",
            "Выполняется сервисом автоматически",
        ),
    ]
    for row in matrix_rows:
        add_table_row(matrix, row)

    doc.add_heading("Права доступа (permissions) и предоставляемая функциональность", level=1)
    doc.add_paragraph(
        "Права хранятся в compliance-auth-service (таблица permissions) и назначаются "
        "индивидуально каждому пользователю. Роль EXECUTIVE при регистрации получает полный "
        "набор прав; для MANAGER, SUPERVISOR и DEFAULT набор изначально пустой и "
        "настраивается пользователем с правом EDIT_USERS. Ниже — что даёт каждое право "
        "в экосистеме TrustFlow CMS и как оно связано с cms-risk-service."
    )

    perm_table = doc.add_table(rows=1, cols=4)
    perm_table.style = "Table Grid"
    ph = perm_table.rows[0].cells
    ph[0].text = "Код права"
    ph[1].text = "Предоставляемая функциональность"
    ph[2].text = "Где применяется"
    ph[3].text = "Связь с cms-risk-service"
    for cell in ph:
        set_cell_shading(cell, "D9E2F3")

    permission_rows = [
        (
            "VIEW_ALL_PAGES",
            "Доступ ко всем разделам админ-панели (обход поштучных VIEW_*_PAGE при проверке навигации в UI).",
            "Веб-интерфейс (фронтенд / auth)",
            "Не проверяется API сервиса. Косвенно открывает раздел правил, если нет отдельного VIEW_RULES_AND_RISKS_PAGE.",
        ),
        (
            "VIEW_DASHBOARD_PAGE",
            "Просмотр главной страницы (дашборда) с агрегированной информацией.",
            "Веб-интерфейс",
            "Не проверяется cms-risk-service.",
        ),
        (
            "VIEW_USERS_PAGE",
            "Просмотр списка пользователей компании.",
            "compliance-auth-service: GET /api/users (обязательная проверка права).",
            "Не проверяется cms-risk-service.",
        ),
        (
            "VIEW_RISK_OBJECTS_PAGE",
            "Просмотр раздела «Объекты риска»: каталог моделей и экземпляров объектов риска.",
            "Веб-интерфейс; маршруты /api/risk-objects через API Gateway → cms-monitoring-service.",
            "Не проверяется cms-risk-service. Сервис только читает объекты риска по внутреннему API "
            "при обогащении списка правил (GET /api/internal/risk-objects/{id}).",
        ),
        (
            "VIEW_INTEGRATIONS_PAGE",
            "Просмотр раздела «Интеграции» (подключённые системы, настройки обмена).",
            "Веб-интерфейс; сервисы интеграций",
            "Не проверяется cms-risk-service.",
        ),
        (
            "VIEW_RULES_AND_RISKS_PAGE",
            "Просмотр раздела «Правила и риски» в админ-панели: список правил, детали, "
            "история изменений, категории, статистика обработки, результаты верификации (режим чтения в UI).",
            "Веб-интерфейс (контроль видимости меню и страниц)",
            "Не проверяется REST API cms-risk-service. Для чтения данных API достаточно JWT; "
            "отсутствие права ограничивает только доступ через UI.",
        ),
        (
            "VIEW_SETTINGS_PAGE",
            "Просмотр раздела «Настройки» компании и системных параметров.",
            "Веб-интерфейс",
            "Не проверяется cms-risk-service.",
        ),
        (
            "VIEW_PROFILE_PAGE",
            "Просмотр и редактирование собственного профиля пользователя.",
            "Веб-интерфейс",
            "Не проверяется cms-risk-service.",
        ),
        (
            "EDIT_USERS",
            "Изменение статуса пользователей компании; просмотр и изменение набора permissions "
            "других пользователей (PUT /api/users/{id}/access).",
            "compliance-auth-service",
            "Не проверяется cms-risk-service. Требуется администратору для выдачи MANAGE_RULES_AND_RISKS "
            "пользователям с ролями MANAGER / SUPERVISOR / DEFAULT.",
        ),
        (
            "MANAGE_RISK_OBJECTS",
            "Создание, изменение и удаление объектов риска и связанных сущностей в модуле мониторинга.",
            "cms-monitoring-service (через API Gateway), веб-интерфейс",
            "Не проверяется cms-risk-service. Связан с правилами косвенно: правило может ссылаться "
            "на riskObjectId; изменение привязки в правиле требует MANAGE_RULES_AND_RISKS.",
        ),
        (
            "MANAGE_INTEGRATIONS",
            "Настройка и изменение интеграций (источники данных, параметры подключения).",
            "Сервисы интеграций, веб-интерфейс",
            "Не проверяется cms-risk-service. События из интеграций попадают в обработку через Kafka "
            "независимо от прав пользователя.",
        ),
        (
            "MANAGE_RULES_AND_RISKS",
            "Полное управление правилами риска и категориями рисков: создание и изменение правил, "
            "привязка объекта риска к правилу, CRUD категорий. В UI — кнопки «Создать», «Сохранить», "
            "«Удалить» в разделе правил и категорий.",
            "cms-risk-service (серверная проверка через auth); веб-интерфейс",
            "Обязательная проверка для операций записи (см. таблицу ниже). "
            "Запрос: GET /api/users/{id}/permissions/check?permission=MANAGE_RULES_AND_RISKS.",
        ),
    ]
    for row in permission_rows:
        add_table_row(perm_table, row)

    doc.add_heading(
        "Соответствие прав и операций cms-risk-service (REST API)", level=2
    )
    doc.add_paragraph(
        "Проверка права MANAGE_RULES_AND_RISKS выполняется в прикладном слое перед изменением данных. "
        "Операции чтения и CRUD результатов верификации дополнительных прав не требуют."
    )

    map_table = doc.add_table(rows=1, cols=3)
    map_table.style = "Table Grid"
    mh2 = map_table.rows[0].cells
    mh2[0].text = "Право"
    mh2[1].text = "Операции API cms-risk-service"
    mh2[2].text = "Поведение без права"
    for cell in mh2:
        set_cell_shading(cell, "E2EFDA")

    permission_api_map = [
        (
            "— (только JWT)",
            "GET /api/rules, GET /api/rules/{id}, GET /api/rules/short/{id}, "
            "GET /api/rules/change-history, GET /api/rules/change-history/{id}, "
            "GET /api/risk-categories, GET /api/rules/processing/statistic, "
            "GET /api/risks/processing/statistic, GET/POST/PUT/DELETE /api/verification-results",
            "Доступны любому аутентифицированному пользователю с валидным JWT и companyId.",
        ),
        (
            "VIEW_RULES_AND_RISKS_PAGE",
            "Те же операции чтения, что и выше (через UI)",
            "Раздел «Правила и риски» скрыт в админ-панели; прямой вызов API по-прежнему возможен при наличии JWT.",
        ),
        (
            "MANAGE_RULES_AND_RISKS",
            "POST /api/rules; PUT /api/rules/{id}; PUT /api/rules/{id}/risk-object; "
            "POST /api/risk-categories; PUT /api/risk-categories/{id}; DELETE /api/risk-categories/{id}",
            "HTTP 403 Forbidden с сообщением «User has no permission: MANAGE_RULES_AND_RISKS».",
        ),
        (
            "EDIT_USERS",
            "—",
            "Не используется API cms-risk-service. Нужен для назначения MANAGE_RULES_AND_RISKS другим пользователям.",
        ),
        (
            "MANAGE_RISK_OBJECTS",
            "—",
            "Не используется API cms-risk-service. Управление объектами риска — в cms-monitoring-service.",
        ),
        (
            "Прочие VIEW_* / MANAGE_INTEGRATIONS",
            "—",
            "Не влияют на API cms-risk-service.",
        ),
    ]
    for row in permission_api_map:
        add_table_row(map_table, row)

    doc.add_paragraph(
        "Вход в админ-панель (POST /auth/admin/login в compliance-auth-service) разрешён пользователям, "
        "у которых в permissions назначен хотя бы один элемент. Наличие VIEW_RULES_AND_RISKS_PAGE "
        "или MANAGE_RULES_AND_RISKS не заменяет JWT при вызове API cms-risk-service — токен обязателен "
        "для всех публичных эндпоинтов, кроме Swagger и внутренних localhost-маршрутов."
    )

    doc.add_paragraph()
    footer = doc.add_paragraph(
        "Источник: исходный код cms-risk-service и compliance-auth-service. "
        "Дата формирования: 17.05.2026."
    )
    footer.runs[0].font.size = Pt(9)
    footer.runs[0].italic = True

    output_path = (
        r"C:\trustflow-backend\cms-risk-service\docs"
        r"\Перечень_функциональных_возможностей_cms-risk-service.docx"
    )
    import os

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    doc.save(output_path)
    print(output_path)


if __name__ == "__main__":
    main()
