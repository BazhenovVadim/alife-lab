# Искусственная жизнь

## Запуск оконного приложения

JavaFX — это отдельный набор библиотек, который не входит в JDK начиная с Java 11.
Поэтому приложение **нельзя** запускать как обычный Spring Boot JAR командой
`java -jar target/alife-lab-1.0.0.jar`: в таком запуске JVM не получает JavaFX
`module-path` и нативные библиотеки для операционной системы.

Для запуска GUI используйте Maven-цель, добавленную в проект:

```bash
mvn javafx:run
```

В IntelliJ IDEA откройте окно **Maven** → **Plugins** → **javafx** →
`javafx:run`, либо создайте Maven Run Configuration с командой `javafx:run`.
Запускать нужно `com.vadim.alife.AlifeApplication`, а не
`com.vadim.alife.ui.EcosystemFxApplication` напрямую.

## Консольный режим

Чтобы вместо окна запустить консольную симуляцию, включите свойство:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--simulation.console=true"
```

Карта печатается через интервал `simulation.print-every-n-steps` (по умолчанию
100 шагов). Обозначения: `Р` — растение, `Т` — травоядное, `Х` — хищник.
