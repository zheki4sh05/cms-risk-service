// ============================================
// ID: R-01
// Название: Превышение лимита суммы договора
// Категория: Договорной риск
// ============================================

// 1. Получаем данные из параметров
def contractAmount = params.price      // сумма договора
def limit = 10000              // лимит (по умолчанию 10000 BYN)

// 2. Выполняем проверку
def isExceeded = contractAmount > limit

// 3. Формируем результат
if (isExceeded) {
    def excess = contractAmount - limit
    def severity = "MEDIUM"

    // Если превышение больше чем в 2 раза — повышаем критичность
    if (contractAmount > limit * 2) {
        severity = "HIGH"
    }

    return [
            result : "success",
            found  : true,
            details: [
                    result: "Превышен"
            ]
    ]
}

// 4. Если проверка не пройдена
return [
        result : "success",
        found  : false,
        details: [
                result: "Все гуд",
        ]
]