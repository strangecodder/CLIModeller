### Установка утилиты
Для установки необходимо выполнить следующую команду на linux/macos
````
curl -fsSL https://raw.githubusercontent.com/strangecodder/CLIModeller/main/install.sh | sudo bash
````
Далее монжо проверить корректность установки командой
````
cfsm-modeller --help
````
### Пример моделирования
В репозитории в папке example представлен пример конфигурации системы взаимодействующих конечных автоматов.
Для проверки достаточно скачать его и выполнить следующую команду
````
cfsm-modeller -r sensor.scxml  -l "4h" -s sensor_result.xes -p 
````
