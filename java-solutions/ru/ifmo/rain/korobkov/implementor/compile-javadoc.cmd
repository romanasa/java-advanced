@echo off
mkdir _javadoc


cd ../../../../../../
Xcopy /E /I java-solutions ru.ifmo.rain.korobkov.implementor

cd ru.ifmo.rain.korobkov.implementor/ru/ifmo/rain/korobkov/implementor
javadoc -private --module-source-path ../../../../../../../java-advanced-2020/modules/;../../../../../..^
 -p ../../../../../../../java-advanced-2020/lib/junit-4.11.jar ^
 -d ../../../../../../java-solutions/ru/ifmo/rain/korobkov/implementor/_javadoc/ ^
 Implementor.java JarImplementor.java ../../../../../module-info.java
 
cd ../../../../../../

RD /S /Q ru.ifmo.rain.korobkov.implementor
cd java-solutions/ru/ifmo/rain/korobkov/implementor