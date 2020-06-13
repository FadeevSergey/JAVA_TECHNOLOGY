module=info/kgeorgiy/java/advanced/implementor
cur=ru/ifmo/rain/fadeev/implementor
javac -d "_build" -cp "../../../../../../../java-advanced-2020/artifacts/*" JarImplementor.java Implementor.java
cd "_build"

echo "Manifest-Version: 1.0" > MANIFEST.MF
echo "\n" > MANIFEST.MF
echo "Main-Class: ru.ifmo.rain.fadeev.implementor.JarImplementor" > MANIFEST.MF

jar xf ../../../../../../../../java-advanced-2020/artifacts/info.kgeorgiy.java.advanced.implementor.jar ${module}/Impler.class ${module}/JarImpler.class ${module}/ImplerException.class
jar cfm ../_implementor.jar MANIFEST.MF ${cur}/*.class ${module}/*.class
cd ../
rm -r "_build"
