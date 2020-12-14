### Процесс сборки и запуска программы

######Предполагается что в системе установлены следующие компоненты:
**jdk 11**
**maven**
**git**
**jarwrapper** 

1. Перейдите в домашний каталог
*cd~

1. Скопируйте проект из git репозитория
*git clone https://github.com/olegermolaev84/archiver.git

1. Перейдите в каталог с проектом.
*cd archiver

1. Соберите проект
*mvn package

1. Переместите jar файл в каталог /usr/bin
*sudo mv target/archiver* /usr/bin/archiver

1. Сделайте jar файл исполняемым
*chmod u+x /usr/bin/archiver
