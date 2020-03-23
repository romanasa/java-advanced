mkdir _build
javac -p ../../../../../../../java-advanced-2020/artifacts/info.kgeorgiy.java.advanced.implementor.jar;^
../../../../../../../java-advanced-2020/artifacts/info.kgeorgiy.java.advanced.base.jar;^
../../../../../../../java-advanced-2020/lib/junit-4.11.jar^
			-d _build/ Implementor.java JarImplementor.java module-info.java
							  
echo Manifest-Version: 1.0 > _build/MANIFEST.TXT
echo Main-Class: ru.ifmo.rain.korobkov.implementor.Implementor >> _build/MANIFEST.TXT
echo Class-Path: ../../../../../../../java-advanced-2020/artifacts/info.kgeorgiy.java.advanced.implementor.jar >> _build/MANIFEST.TXT 

cd _build
jar cfm ../_implementor.jar MANIFEST.TXT *.class ru/ifmo/rain/korobkov/implementor/*.class
cd ..

