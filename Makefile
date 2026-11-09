.PHONY: build test run clean

# Сборка jar-файла без запуска тестов
build:
	mvn -q package -DskipTests

# Запуск всех тестов
test:
	mvn test

# Запуск программы: make run ARGS="--package Newtonsoft.Json --package-version 13.0.3"
run:
	./run.sh $(ARGS)

# Удаление артефактов сборки
clean:
	mvn -q clean
