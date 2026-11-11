# syntax=docker/dockerfile:1

# ---------- estagio 1: build (JDK 21 + Maven Wrapper) ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw
COPY db db
COPY src src
# os testes rodam no CI (./mvnw verify); aqui so empacotamos
# (sem dependency:go-offline: uma dependencia transitiva do JSTL referencia um POM inexistente e quebraria o resolve)
RUN ./mvnw -B -q -DskipTests package && cp target/*.war /tmp/app.war

# ---------- estagio 2: runtime (Tomcat 10.1 + JRE 21) ----------
FROM tomcat:10.1-jre21-temurin

# Memoria contida; se faltar memoria o processo sai (-XX:+ExitOnOutOfMemoryError) e a plataforma reinicia o
# servico (em modo demonstracao a base volta ao seed).
ENV JAVA_OPTS="-XX:MaxRAMPercentage=60 -XX:MaxMetaspaceSize=192m -XX:+ExitOnOutOfMemoryError -XX:+UseSerialGC -Djava.awt.headless=true -Djava.security.egd=file:/dev/urandom" \
    PORT=8080

# Aplicacao na raiz (ROOT.war => contexto "/"). O docker-entrypoint.sh ajusta o conf/server.xml
# (porta, ErrorReportValve e, atras de proxy, RemoteIpValve) a cada inicio, de forma idempotente.
RUN rm -rf /usr/local/tomcat/webapps/* \
 && groupadd --system app \
 && useradd --system --gid app --no-create-home --home-dir /usr/local/tomcat app

COPY --from=build /tmp/app.war /usr/local/tomcat/webapps/ROOT.war
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
# conf/, logs/, temp/ e work/ precisam ser gravaveis pelo usuario "app" (o entrypoint edita conf/server.xml)
RUN chmod +x /usr/local/bin/docker-entrypoint.sh \
 && chown -R app:app /usr/local/tomcat

# Nao roda como root
USER app
EXPOSE 8080

# Sem CANTINA_DB_URL a aplicacao usa H2 em memoria com dados de demonstracao (veja .env.example).
ENTRYPOINT ["docker-entrypoint.sh"]
