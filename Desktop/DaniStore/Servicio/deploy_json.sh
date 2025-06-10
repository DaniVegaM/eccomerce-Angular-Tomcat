#!/bin/zsh
# Script para compilar, limpiar, empaquetar y desplegar el WAR SOLO con servicio_json

# Variables (ajusta si tu Tomcat está en otra ruta)
TOMCAT_HOME=../apache-tomcat-8.5.99
WAR_NAME=Servicio.war

# 1. Limpiar clases viejas
rm -rf WEB-INF/classes/servicio_url
rm -f WEB-INF/classes/servicio_json/*

# 2. Compilar servicio_json
javac -cp WEB-INF/lib/javax.ws.rs-api-2.0.1.jar:WEB-INF/lib/gson-2.3.1.jar:. servicio_json/*.java

# 3. Copiar .class
cp servicio_json/*.class WEB-INF/classes/servicio_json/

# 4. Empaquetar WAR
jar cvf $WAR_NAME WEB-INF META-INF

# 5. Eliminar WAR y carpeta expandida en Tomcat
rm -f $TOMCAT_HOME/webapps/$WAR_NAME
rm -rf $TOMCAT_HOME/webapps/Servicio

# 6. Copiar nuevo WAR
cp $WAR_NAME $TOMCAT_HOME/webapps/

# 7. Reiniciar Tomcat
$TOMCAT_HOME/bin/shutdown.sh
sleep 3
$TOMCAT_HOME/bin/startup.sh

echo "\nDespliegue terminado. Verifica los logs de Tomcat si hay algún problema."
