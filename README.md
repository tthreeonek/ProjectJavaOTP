# OTP Verification Service

Сервис генерации и проверки одноразовых кодов (OTP) для подтверждения операций.  
Разработан в учебных целях, но решает реальную задачу заказчика Promo IT.

## Требования
- Java 17+
- Maven 3.8+
- PostgreSQL 17 (созданная база `otpdb`)
- Для тестирования SMS – эмулятор [SMPPSim](https://github.com/aristotelis-metsinis/smsc-smpp-simulator) (порт по умолчанию 2775)
- Для отправки в Telegram – бот, созданный через [@BotFather](https://t.me/BotFather), и ваш chat_id
- При использовании VPN для доступа к Telegram API не забудьте запускать приложение с флагом системного прокси (см. раздел «Запуск»)

## Настройка

1. **База данных**  
   Установите PostgreSQL 17, создайте базу `otpdb`.  
   В файле `src/main/resources/db.properties` укажите свои параметры подключения:
   ```properties
   db.url=jdbc:postgresql://localhost:5432/otpdb
   db.username=postgres
   db.password=ваш_пароль
   db.pool.size=5

2. **Остальные конфигурации**
   Файлы `jwt.properties`, `email.properties`, `sms.properties`, `telegram.properties` уже содержат шаблоны. Заполните их актуальными данными.  
   *Для email:* потребуется реальный SMTP‑сервер (Gmail, Yandex и т.п.) с включённой поддержкой «ненадёжных приложений» или паролем приложения.  
   *Для Telegram:* укажите токен бота и ваш chat_id (узнать его можно, отправив боту любое сообщение и открыв `https://api.telegram.org/bot<ТОКЕН>/getUpdates`).

3. **Эмулятор SMS**
   Скачайте SMPPSim, распакуйте и запустите `startsmppsim.bat`. Убедитесь, что в `sms.properties` хост и порт совпадают (`localhost:2775`).

4. **Telegram-бот**
   В приложении Telegram начните диалог с ботом (нажмите «Start») – иначе сообщения от бота доставляться не будут.

## Сборка и запуск
**Сборка:** ``mvn clean package``

После успешной сборки в папке target появится исполняемый JAR со всеми зависимостями.``

**Запуск (обычный режим):**
``java -jar target/otp-service-1.0-SNAPSHOT.jar``

По умолчанию сервер слушает порт 8081 (можно изменить в Main.java).

**Запуск с VPN (для работы Telegram, если прямой доступ заблокирован):**

``java -Djava.net.useSystemProxies=true -jar target/otp-service-1.0-SNAPSHOT.jar``

Этот режим заставляет Java использовать системные настройки прокси (ваш VPN).

**Либо запуск через Maven без сборки JAR:**

``mvn exec:java -Dexec.mainClass="com.promoit.otp.Main"``

API
Сервер принимает JSON, аутентификация – через заголовок Authorization: Bearer <токен>.
Далее приведены примеры с использованием curl (для Windows командной строки). Все запросы логируются (консоль + файл logs/otp-service.log).

**1. Регистрация пользователя:**

``curl -X POST http://localhost:8081/api/register ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"admin\",\"role\":\"ADMIN\"}"``

Если администратор уже существует, вернётся ошибка.

**2. Логин (получение JWT):**

``curl -X POST http://localhost:8081/api/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"admin\"}"``

Из ответа скопируйте значение "token". Токен действителен 1 час.

**3. Генерация OTP и отправка по выбранным каналам**

Замените ВАШ_ТОКЕН и укажите нужные каналы (sms, email, telegram, file) и operationId.
Для email/sms укажите destination (номер телефона или email), для telegram – оставьте пустым (chat_id уже задан в настройках):

``curl -X POST http://localhost:8081/api/user/generate-otp ^
  -H "Authorization: Bearer ВАШ_ТОКЕН" ^
  -H "Content-Type: application/json" ^
  -d "{\"operationId\":\"op123\",\"channels\":[\"telegram\"],\"destination\":\"\"}"``

После выполнения в выбранный канал должен прийти код.

**4. Проверка (валидация) кода:**

``curl -X POST http://localhost:8081/api/user/validate-otp ^
  -H "Authorization: Bearer ВАШ_ТОКЕН" ^
  -H "Content-Type: application/json" ^
  -d "{\"operationId\":\"op123\",\"code\":\"123456\"}"``

Ответ: {"valid": true} или {"valid": false}.

**5. Административные методы (только роль ADMIN)**

Изменить параметры OTP (длина кода, время жизни в секундах):

``curl -X PUT http://localhost:8081/api/admin/config ^
  -H "Authorization: Bearer ВАШ_ТОКЕН" ^
  -H "Content-Type: application/json" ^
  -d "{\"codeLength\":8,\"ttlSeconds\":600}"``

**Список всех пользователей (кроме админов):**

``curl -X GET http://localhost:8081/api/admin/users ^
  -H "Authorization: Bearer ВАШ_ТОКЕН"``

**Удалить пользователя с ID = 2:**

``curl -X DELETE http://localhost:8081/api/admin/users/2 ^
  -H "Authorization: Bearer ВАШ_ТОКЕН"``

**Логирование**

Все запросы к API записываются в консоль и в файл logs/otp-service.log.
Формат лога: → МЕТОД /путь, ← МЕТОД /путь → СТАТУС (время ms).
При ошибках в лог попадают детали исключений.

**Примечания:**

1. При первом запуске автоматически создаётся администратор admin / admin, если его ещё нет.

2. OTP‑коды, срок которых истёк, помечаются как EXPIRED фоновым планировщиком (раз в минуту).

3. Телеграм-рассылка требует VPN, если доступ к api.telegram.org ограничен в вашей сети. Запускайте сервер с флагом -Djava.net.useSystemProxies=true.

4. Проект реализован на встроенном HTTP‑сервере com.sun.net.httpserver, без Spring.