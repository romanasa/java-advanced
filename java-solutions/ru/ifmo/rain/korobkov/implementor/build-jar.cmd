mkdir _build

cd ../../../../../

javac -p ../../java-advanced-2020/artifacts/info.kgeorgiy.java.advanced.implementor.jar;^
../../java-advanced-2020/artifacts/info.kgeorgiy.java.advanced.base.jar;^
../../java-advanced-2020/lib/junit-4.11.jar^
			-d ru/ifmo/rain/korobkov/implementor/_build/ ru/ifmo/rain/korobkov/implementor/*.java module-info.java
			
cd ru/ifmo/rain/korobkov/implementor
							  
echo Manifest-Version: 1.0 > _build/MANIFEST.TXT
echo Class-Path: ../../../../../../../java-advanced-2020/artifacts/info.kgeorgiy.java.advanced.implementor.jar >> _build/MANIFEST.TXT 

cd _build
jar cfem ../_implementor.jar ru.ifmo.rain.korobkov.implementor.Implementor MANIFEST.TXT *.class ru/ifmo/rain/korobkov/implementor/*.class 
cd ..
RD /S /Q _build


