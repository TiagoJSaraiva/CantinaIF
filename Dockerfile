# syntax=docker/dockerfile:1

# ---------- estagio 1: build (JDK 21 + Maven Wrapper) ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY db db
COPY src src
# os testes rodam no CI (./mvnw verify); aqui so empacotamos
RUN ./mvnw -B -q -DskipTests package && cp target/*.war /tmp/app.war

# ---------- estagio 2: runtime (Tomcat 10.1 + JRE 21) ----------
FROM tomcat:10.1-jre21-temurin

# Memoria contida: limita o heap a uma fracao da RAM do contêiner.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Djava.security.egd=file:/dev/urandom" \
    PORT=8080

# Aplicacao na raiz (ROOT.war => contexto "/"); porta vinda de ${http.port}, definida a partir de PORT.
# RemoteIpValve: so confia em X-Forwarded-* quando CANTINA_BEHIND_PROXY=true (veja docker-entrypoint.sh).
RUN rm -rf /usr/local/tomcat/webapps/* \
 && sed -i 's/port="8080"/port="${http.port:-8080}"/' /usr/local/tomcat/conf/server.xml \
 && sed -i 's#</Host>#<Valve className="org.apache.catalina.valves.RemoteIpValve" remoteIpHeader="X-Forwarded-For" protocolHeader="X-Forwarded-Proto" internalProxies="${cantina.trusted.proxies:-a^}" /></Host>#' /usr/local/tomcat/conf/server.xml \
 && groupadd --system app \
 && useradd --system --gid app --no-create-home --home-dir /usr/local/tomcat app

COPY --from=build /tmp/app.war /usr/local/tomcat/webapps/ROOT.war
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh \
 && chown -R app:app /usr/local/tomcat

# Nao roda como root
USER app
EXPOSE 8080

# Sem CANTINA_DB_URL a aplicacao usa H2 em memoria com dados de demonstracao (veja .env.example).
ENTRYPOINT ["docker-entrypoint.sh"]
